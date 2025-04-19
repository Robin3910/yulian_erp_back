package cn.iocoder.yudao.module.temu.service.orderBatch.impl;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchCreateVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchPageVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchUpdateFileVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.*;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderBatchMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderBatchRelationMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.enums.TemuOrderStatusEnum;
import cn.iocoder.yudao.module.temu.service.orderBatch.ITemuOrderBatchService;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.*;

@Service
public class TemuOrderBatchService implements ITemuOrderBatchService {
	
	@Resource
	private TemuOrderBatchMapper temuOrderBatchMapper;
	@Resource
	private TemuOrderMapper temuOrderMapper;
	@Resource
	private TemuOrderBatchRelationMapper temuOrderBatchRelationMapper;
	
	/*
	 * 创建批次订单
	 * @param temuOrderBatchCreateVO 批次订单创建VO
	 * @return 批次订单关系记录数量
	 */
	@Override
	@Transactional
	public Integer createBatch(TemuOrderBatchCreateVO temuOrderBatchCreateVO) {
		if (temuOrderBatchCreateVO == null || temuOrderBatchCreateVO.getOrderIds().isEmpty()) {
			throw exception(ORDER_NOT_EXISTS);
		}
		//数据去重
		List<Long> orderIds = temuOrderBatchCreateVO.getOrderIds().stream().distinct().collect(Collectors.toList());
		
		List<TemuOrderBatchRelationDO> temuOrderBatchRelationDOList = CollectionUtils.convertList(orderIds, orderId -> {
			TemuOrderBatchRelationDO temuOrderBatchRelationDO = new TemuOrderBatchRelationDO();
			temuOrderBatchRelationDO.setOrderId(orderId);
			return temuOrderBatchRelationDO;
		});
		// 检查订单是否存在
		temuOrderBatchRelationDOList.forEach(temuOrderBatchRelationDOItem -> {
			TemuOrderDO temuOrderDO = temuOrderMapper.selectById(temuOrderBatchRelationDOItem.getOrderId());
			// 检查订单是否存在 
			if (temuOrderDO == null) {
				throw exception(ORDER_NOT_EXISTS);
			}
			//检查订单是否已经被下单
			if (temuOrderDO.getOrderStatus() != TemuOrderStatusEnum.ORDERED) {
				throw exception(ORDER_STATUS_ERROR);
			}
			//检查订单是否已经被批次化
			// 检查订单状态是否存在历史批次中
			Long count = temuOrderBatchRelationMapper.selectCount("order_id", temuOrderBatchRelationDOItem.getOrderId());
			if (count > 0) {
				throw exception(ORDER_BATCH_EXISTS);
			}
		});
		//批量更新订单
		QueryWrapper<TemuOrderDO> temuOrderDOQueryWrapper = new QueryWrapper<>();
		temuOrderDOQueryWrapper.in("id", orderIds);
		TemuOrderDO temuOrderDO = new TemuOrderDO();
		//更新订单状态 已送产待生产
		temuOrderDO.setOrderStatus(TemuOrderStatusEnum.IN_PRODUCTION);
		int update = temuOrderMapper.update(temuOrderDO, temuOrderDOQueryWrapper);
		if (update <= 0) {
			throw exception(ORDER_BATCH_CREATE_FAIL);
		}
		//插入批次
		//创建批次订单记录
		TemuOrderBatchDO temuOrderBatchDO = new TemuOrderBatchDO();
		//设置uuid
		temuOrderBatchDO.setBatchNo(IdUtil.randomUUID());
		int insert = temuOrderBatchMapper.insert(temuOrderBatchDO);
		if (insert <= 0) {
			throw exception(ORDER_BATCH_CREATE_FAIL);
		}
		temuOrderBatchRelationDOList.forEach(temuOrderBatchRelationDOItem -> {
			temuOrderBatchRelationDOItem.setBatchId(temuOrderBatchDO.getId());
		});
		Boolean result = temuOrderBatchRelationMapper.insertBatch(temuOrderBatchRelationDOList);
		return result ? temuOrderBatchRelationDOList.size() : null;
	}
	
	/*
	 * 分页查询批次订单
	 * @param temuOrderBatchPageVO 批次订单分页查询VO
	 * @return 批次订单分页查询结果
	 */
	@Override
	public PageResult<TemuOrderBatchDetailDO> list(TemuOrderBatchPageVO temuOrderBatchPageVO) {
		PageResult<TemuOrderBatchDetailDO> temuOrderBatchDOPageResult = temuOrderBatchMapper.selectPage(temuOrderBatchPageVO);
		temuOrderBatchDOPageResult.getList().forEach(temuOrderBatchDO -> {
			//查询关联的订单详情
			List<TemuOrderDetailDO> orderList = temuOrderBatchDO.getOrderList();
			//查询所有关联的批次订单
			List<TemuOrderDetailDO> temuOrderBatchRelationDOList = temuOrderMapper.selectListByOrderIds(
					orderList.stream().map(TemuOrderDetailDO::getId)
							.collect(Collectors.toList())
			);
			temuOrderBatchDO.setOrderList(temuOrderBatchRelationDOList);
		});
		
		return temuOrderBatchDOPageResult;
	}
	
	/**
	 * 更新批次文件信息
	 * 该方法通过接收一个 TemuOrderBatchUpdateFileVO 对象作为参数，将其转换为 TemuOrderBatchDO 对象，并调用 mapper 层的更新方法
	 * 主要目的是为了更新数据库中批次文件的相关信息
	 * @param temuOrderBatchUpdateFileVO 包含要更新的批次文件信息的视图对象
	 * @return 返回更新操作影响的行数
	 */
	@Override
	public int updateBatchFile(TemuOrderBatchUpdateFileVO temuOrderBatchUpdateFileVO) {
	    return temuOrderBatchMapper.updateById(BeanUtils.toBean(temuOrderBatchUpdateFileVO, TemuOrderBatchDO.class));
	}
	
}
