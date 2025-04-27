package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchPageVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.*;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper
public interface TemuOrderBatchMapper extends BaseMapperX<TemuOrderBatchDO> {
	default PageResult<TemuOrderBatchDetailDO> selectPage(TemuOrderBatchPageVO temuOrderBatchPageVO) {
		// 第一步：查询批次信息
		MPJLambdaWrapperX<TemuOrderBatchDO> batchWrapper = new MPJLambdaWrapperX<>();
		batchWrapper.selectAll(TemuOrderBatchDO.class)
				.eqIfExists(TemuOrderBatchDO::getStatus, temuOrderBatchPageVO.getStatus())
				.like(StringUtils.isNotEmpty(temuOrderBatchPageVO.getBatchNo()), TemuOrderBatchDO::getBatchNo,
						temuOrderBatchPageVO.getBatchNo());

		// 时间范围查询
		if (temuOrderBatchPageVO.getCreateTime() != null && temuOrderBatchPageVO.getCreateTime().length == 2) {
			batchWrapper.between(TemuOrderBatchDO::getCreateTime, temuOrderBatchPageVO.getCreateTime()[0],
					temuOrderBatchPageVO.getCreateTime()[1]);
		}

		// 按批次分组
		batchWrapper.groupBy(TemuOrderBatchDO::getId);
		
		// 按照创建时间倒序排列
		batchWrapper.orderByDesc(TemuOrderBatchDO::getCreateTime);
		
		// 执行批次分页查询
		PageResult<TemuOrderBatchDO> batchPageResult = selectPage(temuOrderBatchPageVO, batchWrapper);
		
		// 如果没有批次数据，直接返回空结果
		if (batchPageResult.getList().isEmpty()) {
			return new PageResult<>(new ArrayList<>(), batchPageResult.getTotal(), 
					temuOrderBatchPageVO.getPageNo(), temuOrderBatchPageVO.getPageSize());
		}
		
		// 获取批次ID列表
		List<Long> batchIds = batchPageResult.getList().stream()
				.map(TemuOrderBatchDO::getId)
				.collect(Collectors.toList());
		
		// 第二步：查询批次关联的订单信息
		MPJLambdaWrapperX<TemuOrderBatchDO> orderWrapper = new MPJLambdaWrapperX<>();
		orderWrapper.selectAll(TemuOrderBatchDO.class)
				.leftJoin(TemuOrderBatchRelationDO.class, TemuOrderBatchRelationDO::getBatchId, TemuOrderBatchDO::getId)
				.leftJoin(TemuOrderDO.class, TemuOrderDO::getId, TemuOrderBatchRelationDO::getOrderId)
				.selectCollection(TemuOrderDO.class, TemuOrderBatchDetailDO::getOrderList)
				.in(TemuOrderBatchDO::getId, batchIds)
				.like(StringUtils.isNotEmpty(temuOrderBatchPageVO.getCustomSku()), TemuOrderDO::getCustomSku,
						temuOrderBatchPageVO.getCustomSku())
				.orderByDesc(TemuOrderDO::getCreateTime);
		
		// 查询批次详情（包含订单列表）
		List<TemuOrderBatchDetailDO> batchDetailList = selectJoinList(TemuOrderBatchDetailDO.class, orderWrapper);
		
		// 根据批次ID对结果进行分组
		Map<Long, List<TemuOrderBatchDetailDO>> batchDetailMap = batchDetailList.stream()
				.collect(Collectors.groupingBy(TemuOrderBatchDetailDO::getId));
		
		// 按原批次顺序组装最终结果
		List<TemuOrderBatchDetailDO> resultList = new ArrayList<>();
		for (TemuOrderBatchDO batch : batchPageResult.getList()) {
			List<TemuOrderBatchDetailDO> details = batchDetailMap.get(batch.getId());
			if (details != null && !details.isEmpty()) {
				// 合并同一批次下的所有订单
				TemuOrderBatchDetailDO mergedDetail = details.get(0);
				if (details.size() > 1) {
					List<TemuOrderDetailDO> allOrders = details.stream()
							.filter(d -> d.getOrderList() != null)
							.flatMap(d -> d.getOrderList().stream())
							.collect(Collectors.toList());
					mergedDetail.setOrderList(allOrders);
				}
				resultList.add(mergedDetail);
			}
		}
		
		// 返回最终分页结果
		return new PageResult<>(resultList, batchPageResult.getTotal(), 
				temuOrderBatchPageVO.getPageNo(), temuOrderBatchPageVO.getPageSize());
	}
}
