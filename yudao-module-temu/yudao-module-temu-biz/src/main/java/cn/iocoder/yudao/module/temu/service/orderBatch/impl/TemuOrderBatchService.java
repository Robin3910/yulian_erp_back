package cn.iocoder.yudao.module.temu.service.orderBatch.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.*;
import cn.iocoder.yudao.module.temu.dal.dataobject.*;
import cn.iocoder.yudao.module.temu.dal.mysql.*;
import cn.iocoder.yudao.module.temu.enums.TemuOrderBatchStatusEnum;
import cn.iocoder.yudao.module.temu.enums.TemuOrderStatusEnum;
import cn.iocoder.yudao.module.temu.service.orderBatch.ITemuOrderBatchService;
import cn.iocoder.yudao.module.temu.mq.producer.weixin.WeiXinProducer;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;
import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.temu.enums.TemuOrderBatchStatusEnum.*;

@Service
public class TemuOrderBatchService implements ITemuOrderBatchService {
	
	@Resource
	private TemuOrderBatchMapper temuOrderBatchMapper;
	@Resource
	private TemuOrderMapper temuOrderMapper;
	@Resource
	private TemuOrderBatchRelationMapper temuOrderBatchRelationMapper;
	@Resource
	private TemuShopMapper temuShopMapper;
	@Resource
	private AdminUserMapper adminUserMapper;
	@Resource
	private OrderBatchTaskMapper orderBatchTaskMapper;
	@Resource
	private WeiXinProducer weiXinProducer;
	
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
		
		// 检查订单是否存在，并保存订单信息用于后续统计
		List<TemuOrderDO> orders = new ArrayList<>();
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
			orders.add(temuOrderDO);
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
		//设置uuid年月日时分秒毫秒
		DateTime dateTime = DateTime.now();
		temuOrderBatchDO.setBatchNo(dateTime.toString("yyyyMMddHHmmssSSS") + (new Random().nextInt(10000) + 10000));
		int insert = temuOrderBatchMapper.insert(temuOrderBatchDO);
		if (insert <= 0) {
			throw exception(ORDER_BATCH_CREATE_FAIL);
		}
		temuOrderBatchRelationDOList.forEach(temuOrderBatchRelationDOItem -> {
			temuOrderBatchRelationDOItem.setBatchId(temuOrderBatchDO.getId());
		});
		Boolean result = temuOrderBatchRelationMapper.insertBatch(temuOrderBatchRelationDOList);
		
		// 发送webhook通知
		if (result) {
			// 获取所有类目
			Set<String> categories = orders.stream()
					.map(TemuOrderDO::getCategoryName)
					.collect(Collectors.toSet());
			
			// 构建通知消息
			StringBuilder message = new StringBuilder();
			message.append("批次打包完成通知\n");
			message.append("批次编号：").append(temuOrderBatchDO.getBatchNo()).append("\n");
			message.append("订单数量：").append(orders.size()).append("\n");
			message.append("类目列表：\n");
			categories.forEach(category ->
					message.append("- ").append(category).append("\n")
			);
			
			// 获取shopId为88888888的店铺webhook地址
			TemuShopDO shop = temuShopMapper.selectByShopId(88888888L);
			if (shop != null && StringUtils.isNotEmpty(shop.getWebhook())) {
				weiXinProducer.sendMessage(shop.getWebhook(), message.toString());
			}
		}
		
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
		// 获取所有分配关联用户的信息
		temuOrderBatchDOPageResult.getList().forEach(batch -> {
			MPJLambdaWrapperX<TemuOrderBatchTaskDO> objectMPJLambdaWrapperX = new MPJLambdaWrapperX<>();
			objectMPJLambdaWrapperX.selectAll(TemuOrderBatchTaskDO.class)
					.selectAs(AdminUserDO::getId, TemuOrderBatchTaskUserInfoDO::getUserId)
					.selectAs(AdminUserDO::getNickname, TemuOrderBatchTaskUserInfoDO::getNickName)
					.leftJoin(AdminUserDO.class, AdminUserDO::getId, TemuOrderBatchTaskDO::getUserId)
					.eq(TemuOrderBatchTaskDO::getBatchOrderId, batch.getId());
			List<TemuOrderBatchTaskUserInfoDO> adminUserDOS = orderBatchTaskMapper.selectJoinList(TemuOrderBatchTaskUserInfoDO.class, objectMPJLambdaWrapperX);
			batch.setUserList(adminUserDOS);
		});
		// 获取所有订单的shopId
		List<Long> shopIds = temuOrderBatchDOPageResult.getList().stream()
				.flatMap(batch -> batch.getOrderList().stream())
				.map(TemuOrderDetailDO::getShopId)
				.distinct()
				.collect(Collectors.toList());
		
		// 批量查询店铺信息
		if (!shopIds.isEmpty()) {
			Map<Long, TemuShopDO> shopMap = temuShopMapper.selectList(new QueryWrapper<TemuShopDO>()
							.in("shop_id", shopIds))
					.stream()
					.collect(Collectors.toMap(TemuShopDO::getShopId, shop -> shop));
			
			// 为每个订单设置shopName
			temuOrderBatchDOPageResult.getList().forEach(batch -> {
				if (batch.getOrderList() != null) {
					batch.getOrderList().forEach(order -> {
						TemuShopDO shop = shopMap.get(order.getShopId());
						if (shop != null) {
							order.setShopName(shop.getShopName());
						}
					});
				}
			});
		}
		
		return temuOrderBatchDOPageResult;
	}
	
	@Override
	public PageResult<TemuOrderBatchUserDetailDO> taskList(TemuOrderBatchPageVO temuOrderBatchPageVO) {
		PageResult<TemuOrderBatchUserDetailDO> temuOrderBatchDOPageResult = selectUserPage(temuOrderBatchPageVO);
		// 获取所有分配关联用户的信息
		temuOrderBatchDOPageResult.getList().forEach(batch -> {
			MPJLambdaWrapperX<TemuOrderBatchTaskDO> objectMPJLambdaWrapperX = new MPJLambdaWrapperX<>();
			objectMPJLambdaWrapperX.selectAll(TemuOrderBatchTaskDO.class)
					.selectAs(AdminUserDO::getId, TemuOrderBatchTaskUserInfoDO::getUserId)
					.selectAs(AdminUserDO::getNickname, TemuOrderBatchTaskUserInfoDO::getNickName)
					.leftJoin(AdminUserDO.class, AdminUserDO::getId, TemuOrderBatchTaskDO::getUserId)
					.eq(TemuOrderBatchTaskDO::getBatchOrderId, batch.getId());
			List<TemuOrderBatchTaskUserInfoDO> adminUserDOS = orderBatchTaskMapper.selectJoinList(TemuOrderBatchTaskUserInfoDO.class, objectMPJLambdaWrapperX);
			batch.setUserList(adminUserDOS);
		});
		// 获取所有订单的shopId
		List<Long> shopIds = temuOrderBatchDOPageResult.getList().stream()
				.flatMap(batch -> batch.getOrderList().stream())
				.map(TemuOrderDetailDO::getShopId)
				.distinct()
				.collect(Collectors.toList());
		
		// 批量查询店铺信息
		if (!shopIds.isEmpty()) {
			Map<Long, TemuShopDO> shopMap = temuShopMapper.selectList(new QueryWrapper<TemuShopDO>()
							.in("shop_id", shopIds))
					.stream()
					.collect(Collectors.toMap(TemuShopDO::getShopId, shop -> shop));
			
			// 为每个订单设置shopName
			temuOrderBatchDOPageResult.getList().forEach(batch -> {
				if (batch.getOrderList() != null) {
					batch.getOrderList().forEach(order -> {
						TemuShopDO shop = shopMap.get(order.getShopId());
						if (shop != null) {
							order.setShopName(shop.getShopName());
						}
					});
				}
			});
		}
		
		return temuOrderBatchDOPageResult;
	}
	
	/**
	 * 更新批次文件信息
	 * 该方法通过接收一个 TemuOrderBatchUpdateFileVO 对象作为参数，将其转换为 TemuOrderBatchDO 对象，并调用 mapper 层的更新方法
	 * 主要目的是为了更新数据库中批次文件的相关信息
	 *
	 * @param temuOrderBatchUpdateFileVO 包含要更新的批次文件信息的视图对象
	 * @return 返回更新操作影响的行数
	 */
	@Override
	public int updateBatchFile(TemuOrderBatchUpdateFileVO temuOrderBatchUpdateFileVO) {
		return temuOrderBatchMapper.updateById(BeanUtils.toBean(temuOrderBatchUpdateFileVO, TemuOrderBatchDO.class));
	}
	
	@Override
	@Transactional
	public int updateBatchFileByTask(TemuOrderBatchUpdateFileByTaskVO temuOrderBatchUpdateFileVO) {
		//检查批次任务状态
		TemuOrderBatchTaskDO temuOrderBatchTaskDO = orderBatchTaskMapper.selectById(temuOrderBatchUpdateFileVO.getTaskId());
		if (temuOrderBatchTaskDO == null) {
			throw exception(ORDER_BATCH_TASK_NOT_EXISTS);
		}
		//检查当前任务是否由处理人处理
		if (!Objects.equals(temuOrderBatchTaskDO.getUserId(), SecurityFrameworkUtils.getLoginUserId())) {
			throw exception(ORDER_BATCH_TASK_NOT_OWNER);
		}
		//待处理状态下完成任务 ，并且修改下个关联任务的状态
		if (temuOrderBatchTaskDO.getStatus() == TemuOrderBatchStatusEnum.TASK_STATUS_WAIT) {
			temuOrderBatchTaskDO.setStatus(TemuOrderBatchStatusEnum.TASK_STATUS_COMPLETE);
			orderBatchTaskMapper.updateById(temuOrderBatchTaskDO);
			if (temuOrderBatchTaskDO.getNextTaskType() != null) {
				//	查找类型和相同批次id任务
				TemuOrderBatchTaskDO nextBatchTaskDO = orderBatchTaskMapper.selectOne(new QueryWrapper<TemuOrderBatchTaskDO>()
						.eq("batch_order_id", temuOrderBatchTaskDO.getBatchOrderId())
						.eq("type", temuOrderBatchTaskDO.getNextTaskType())
						.eq("status", TemuOrderBatchStatusEnum.TASK_STATUS_NOT_HANDLED)
				);
				if (nextBatchTaskDO != null) {
					nextBatchTaskDO.setStatus(TemuOrderBatchStatusEnum.TASK_STATUS_WAIT);
					orderBatchTaskMapper.updateById(nextBatchTaskDO);
				}
			}
		}
		return temuOrderBatchMapper.updateById(BeanUtils.toBean(temuOrderBatchUpdateFileVO, TemuOrderBatchDO.class));
	}
	
	@Override
	@Transactional
	public int updateStatus(TemuOrderBatchUpdateStatusVO temuOrderBatchUpdateStatusVO) {
		//检查批次订单是否存在
		TemuOrderBatchDO temuOrderBatchDO = temuOrderBatchMapper.selectById(temuOrderBatchUpdateStatusVO.getId());
		if (temuOrderBatchDO == null) {
			throw exception(ORDER_BATCH_NOT_EXISTS);
		}
		if (temuOrderBatchDO.getStatus() != TemuOrderBatchStatusEnum.IN_PRODUCTION) {
			throw exception(ORDER_BATCH_STATUS_ERROR);
		}
		List<TemuOrderBatchRelationDO> temuOrderBatchRelationDOList = temuOrderBatchRelationMapper.selectByMap(MapUtil.<String, Object>builder()
				.put("batch_id", temuOrderBatchUpdateStatusVO.getId())
				.build()
		);
		if (temuOrderBatchRelationDOList == null || temuOrderBatchRelationDOList.isEmpty()) {
			throw exception(ORDER_BATCH_NOT_EXISTS);
		}
		//获取所有的订单id
		List<Long> orderIds = temuOrderBatchRelationDOList.stream().map(TemuOrderBatchRelationDO::getOrderId).collect(Collectors.toList());
		//批量更新订单状态
		List<TemuOrderDO> temuOrderDOList = orderIds.stream().unordered().map(orderId -> {
			TemuOrderDO temuOrderDO = new TemuOrderDO();
			temuOrderDO.setId(orderId);
			temuOrderDO.setOrderStatus(TemuOrderStatusEnum.SHIPPED);
			return temuOrderDO;
		}).collect(Collectors.toList());
		temuOrderMapper.updateBatch(temuOrderDOList);
		//设置批次订单状态
		temuOrderBatchDO.setStatus(TemuOrderBatchStatusEnum.PRODUCTION_COMPLETE);
		return temuOrderBatchMapper.updateById(temuOrderBatchDO);
	}
	
	@Override
	@Transactional
	public int updateStatusByTask(TemuOrderBatchUpdateStatusByTaskVO temuOrderBatchUpdateStatusVO) {
		//检查批次任务状态
		TemuOrderBatchTaskDO temuOrderBatchTaskDO = orderBatchTaskMapper.selectById(temuOrderBatchUpdateStatusVO.getTaskId());
		if (temuOrderBatchTaskDO == null) {
			throw exception(ORDER_BATCH_TASK_NOT_EXISTS);
		}
		//检查当前任务是否由处理人处理
		if (!Objects.equals(temuOrderBatchTaskDO.getUserId(), SecurityFrameworkUtils.getLoginUserId())) {
			throw exception(ORDER_BATCH_TASK_NOT_OWNER);
		}
		if (temuOrderBatchTaskDO.getStatus() == TemuOrderBatchStatusEnum.TASK_STATUS_NOT_HANDLED) {
			throw exception(ORDER_STATUS_ERROR);
		}
		// 检查任务是否已经完成
		if (temuOrderBatchTaskDO.getStatus() == TemuOrderBatchStatusEnum.TASK_STATUS_COMPLETE) {
			throw exception(ORDER_BATCH_TASK_COMPLETE);
		}
		//待处理状态下完成任务 ，并且修改下个关联任务的状态
		if (temuOrderBatchTaskDO.getStatus() == TemuOrderBatchStatusEnum.TASK_STATUS_WAIT) {
			temuOrderBatchTaskDO.setStatus(TemuOrderBatchStatusEnum.TASK_STATUS_COMPLETE);
			orderBatchTaskMapper.updateById(temuOrderBatchTaskDO);
		}
		//检查批次订单是否存在
		TemuOrderBatchDO temuOrderBatchDO = temuOrderBatchMapper.selectById(temuOrderBatchUpdateStatusVO.getId());
		if (temuOrderBatchDO == null) {
			throw exception(ORDER_BATCH_NOT_EXISTS);
		}
		if (temuOrderBatchDO.getStatus() != TemuOrderBatchStatusEnum.IN_PRODUCTION) {
			throw exception(ORDER_BATCH_STATUS_ERROR);
		}
		List<TemuOrderBatchRelationDO> temuOrderBatchRelationDOList = temuOrderBatchRelationMapper.selectByMap(MapUtil.<String, Object>builder()
				.put("batch_id", temuOrderBatchUpdateStatusVO.getId())
				.build()
		);
		if (temuOrderBatchRelationDOList == null || temuOrderBatchRelationDOList.isEmpty()) {
			throw exception(ORDER_BATCH_NOT_EXISTS);
		}
		
		//获取所有的订单id
		List<Long> orderIds = temuOrderBatchRelationDOList.stream().map(TemuOrderBatchRelationDO::getOrderId).collect(Collectors.toList());
		//批量更新订单状态
		List<TemuOrderDO> temuOrderDOList = orderIds.stream().unordered().map(orderId -> {
			TemuOrderDO temuOrderDO = new TemuOrderDO();
			temuOrderDO.setId(orderId);
			temuOrderDO.setOrderStatus(TemuOrderStatusEnum.SHIPPED);
			return temuOrderDO;
		}).collect(Collectors.toList());
		temuOrderMapper.updateBatch(temuOrderDOList);
		//设置批次订单状态
		temuOrderBatchDO.setStatus(TemuOrderBatchStatusEnum.PRODUCTION_COMPLETE);
		return temuOrderBatchMapper.updateById(temuOrderBatchDO);
	}
	
	@Override
	public Boolean saveOrderRemark(TemuOrderBatchSaveOrderRemarkReqVO requestVO) {
		//检查批次订单是否存在
		TemuOrderBatchDO temuOrderBatchDO = temuOrderBatchMapper.selectById(requestVO.getOrderId());
		if (temuOrderBatchDO == null) {
			throw exception(ORDER_BATCH_NOT_EXISTS);
		}
		temuOrderBatchDO.setRemark(requestVO.getRemark());
		
		return temuOrderBatchMapper.updateById(temuOrderBatchDO) > 0;
	}
	
	@Override
	@Transactional
	public Boolean dispatchTask(TemuOrderBatchDispatchTaskVO requestVO) {
		//检查批次订单是否存在
		if (requestVO.getOrderIds() == null || requestVO.getOrderIds().length == 0) {
			throw exception(ORDER_BATCH_NOT_EXISTS);
		}
		//检查用户是否存在
		AdminUserDO artStaffUserDO = adminUserMapper.selectById(requestVO.getArtStaffUserId());
		if (artStaffUserDO == null) {
			throw exception(USER_NOT_EXISTS);
		}
		AdminUserDO productionStaffUserDO = adminUserMapper.selectById(requestVO.getProductionStaffUserId());
		if (productionStaffUserDO == null) {
			throw exception(USER_NOT_EXISTS);
		}
		List<TemuOrderBatchTaskDO> temuOrderBatchTaskDOList = new ArrayList<>();
		List<TemuOrderBatchDO> temuOrderBatchDOList = new ArrayList<>();
		//检查批次订单状态是否待生产 未分配
		for (Long orderId : requestVO.getOrderIds()) {
			TemuOrderBatchDO temuOrderBatchDO = temuOrderBatchMapper.selectById(orderId);
			if (temuOrderBatchDO == null) {
				throw exception(ORDER_BATCH_NOT_EXISTS);
			}
			if (temuOrderBatchDO.getStatus() != TemuOrderBatchStatusEnum.IN_PRODUCTION) {
				throw exception(ORDER_BATCH_STATUS_ERROR);
			}
			if (temuOrderBatchDO.getIsDispatchTask() != null && temuOrderBatchDO.getIsDispatchTask() == DISPATCH_TASK) {
				throw exception(ORDER_BATCH_STATUS_ERROR);
			}
			temuOrderBatchDO.setIsDispatchTask(DISPATCH_TASK);
			temuOrderBatchDOList.add(temuOrderBatchDO);
			//作图员任务分配
			temuOrderBatchTaskDOList.add(TemuOrderBatchTaskDO.builder()
					.batchOrderId(orderId)
					.userId(requestVO.getArtStaffUserId())
					.type(TASK_TYPE_ART)
					.status(TASK_STATUS_WAIT)
					.nextTaskType(TASK_TYPE_PRODUCTION)
					.build());
			//生产员任务分配
			temuOrderBatchTaskDOList.add(TemuOrderBatchTaskDO.builder()
					.batchOrderId(orderId)
					.userId(requestVO.getProductionStaffUserId())
					.type(TASK_TYPE_PRODUCTION)
					.status(TASK_STATUS_NOT_HANDLED)
					.build());
		}
		//处理批量插入
		temuOrderBatchMapper.updateBatch(temuOrderBatchDOList);
		orderBatchTaskMapper.insertBatch(temuOrderBatchTaskDOList);
		return true;
	}
	
	private PageResult<TemuOrderBatchUserDetailDO> selectUserPage(TemuOrderBatchPageVO temuOrderBatchPageVO) {
		// 第一步：查询批次信息
		MPJLambdaWrapperX<TemuOrderBatchTaskDO> batchWrapper = new MPJLambdaWrapperX<>();
		batchWrapper.selectAll(TemuOrderBatchDO.class)
				.leftJoin(TemuOrderBatchDO.class, TemuOrderBatchDO::getId, TemuOrderBatchTaskDO::getBatchOrderId)
				.selectAs(TemuOrderBatchTaskDO::getId, "task_id")
				.selectAs(TemuOrderBatchTaskDO::getType, "task_type")
				.selectAs(TemuOrderBatchTaskDO::getStatus, "task_status")
				.selectAs(TemuOrderBatchTaskDO::getUserId, "task_user_id")
				.eqIfExists(TemuOrderBatchTaskDO::getStatus, temuOrderBatchPageVO.getTaskStatus())
				.eqIfExists(TemuOrderBatchTaskDO::getUserId, temuOrderBatchPageVO.getUserId())
				.eqIfExists(TemuOrderBatchDO::getStatus, temuOrderBatchPageVO.getStatus())
				.eqIfExists(TemuOrderBatchDO::getIsDispatchTask, temuOrderBatchPageVO.getIsDispatchTask())
				.like(StringUtils.isNotEmpty(temuOrderBatchPageVO.getBatchNo()), TemuOrderBatchDO::getBatchNo,
						temuOrderBatchPageVO.getBatchNo());
		
		// 时间范围查询
		if (temuOrderBatchPageVO.getCreateTime() != null && temuOrderBatchPageVO.getCreateTime().length == 2) {
			batchWrapper.between(TemuOrderBatchDO::getCreateTime, temuOrderBatchPageVO.getCreateTime()[0],
					temuOrderBatchPageVO.getCreateTime()[1]);
		}
		
		
		// 按照创建时间倒序排列
		batchWrapper.orderByDesc(TemuOrderBatchDO::getCreateTime);
		
		// 执行批次分页查询
		PageResult<TemuOrderBatchUserDetailDO> batchPageResult = orderBatchTaskMapper.selectJoinPage(temuOrderBatchPageVO, TemuOrderBatchUserDetailDO.class, batchWrapper);
		
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
		List<TemuOrderBatchUserDetailDO> batchDetailList = temuOrderBatchMapper.selectJoinList(TemuOrderBatchUserDetailDO.class, orderWrapper);
		
		// 根据批次ID对结果进行分组
		Map<Long, List<TemuOrderBatchUserDetailDO>> batchDetailMap = batchDetailList.stream()
				.collect(Collectors.groupingBy(TemuOrderBatchUserDetailDO::getId));
		
		// 按原批次顺序组装最终结果
		List<TemuOrderBatchUserDetailDO> resultList = new ArrayList<>();
		for (TemuOrderBatchUserDetailDO batch : batchPageResult.getList()) {
			List<TemuOrderBatchUserDetailDO> details = batchDetailMap.get(batch.getId());
			if (details != null && !details.isEmpty()) {
				batch.setOrderList(details.get(0).getOrderList());
				//// 合并同一批次下的所有订单
				//TemuOrderBatchUserDetailDO mergedDetail = details.get(0);
				//if (details.size() > 1) {
				//	List<TemuOrderDetailDO> allOrders = details.stream()
				//			.filter(d -> d.getOrderList() != null)
				//			.flatMap(d -> d.getOrderList().stream())
				//			.collect(Collectors.toList());
				//	mergedDetail.setOrderList(allOrders);
				//}
				resultList.add(batch);
			}
		}
		
		// 返回最终分页结果
		return new PageResult<>(resultList, batchPageResult.getTotal(),
				temuOrderBatchPageVO.getPageNo(), temuOrderBatchPageVO.getPageSize());
	}
	
}
