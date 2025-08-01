package cn.iocoder.yudao.module.temu.service.order.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.*;
import cn.iocoder.yudao.module.temu.dal.dataobject.*;
import cn.iocoder.yudao.module.temu.dal.mysql.*;
import cn.iocoder.yudao.module.temu.mq.producer.weixin.WeiXinProducer;
import cn.iocoder.yudao.module.temu.service.order.ITemuOrderShippingService;
import cn.iocoder.yudao.module.temu.service.deliveryOrder.TemuDeliveryOrderConvertService;
import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import cn.iocoder.yudao.module.temu.dal.event.TrackingNumberValidationEvent;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.util.Objects;

import cn.iocoder.yudao.module.temu.dal.dataobject.TemuWorkerTaskDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuWorkerTaskMapper;

/**
 * Temu订单物流 Service 实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TemuOrderShippingService implements ITemuOrderShippingService {
	
	private final TemuDeliveryOrderConvertService temuDeliveryOrderConvertService;
	private final TemuOrderShippingMapper shippingInfoMapper;
	private final TemuOrderMapper orderMapper;
	private final TemuShopMapper shopMapper;
	private final TemuProductCategoryMapper categoryMapper;
	private final TemuUserShopMapper userShopMapper;
	private final ApplicationEventPublisher eventPublisher;

	@Resource
	private TemuOrderBatchCategoryMapper temuOrderBatchCategoryMapper;
	
	@Resource
	private TemuShopMapper temuShopMapper;

	@Resource
	private WeiXinProducer weiXinProducer;

	@Resource
	private TemuOrderBatchRelationMapper temuOrderBatchRelationMapper;

	@Resource
	private TemuOrderBatchMapper temuOrderBatchMapper;

	@Resource
	private AdminUserMapper adminUserMapper;
	
	@Resource
	private TemuWorkerTaskMapper temuWorkerTaskMapper;

	@Resource
	private ConfigApi configApi;
	
	// 分页查询用户店铺待发货列表
	@Override
	public PageResult<TemuOrderShippingRespVO> getOrderShippingPageByUser(TemuOrderShippingPageReqVO pageVO,
	                                                                      Long userId) {
		// 1. 查询用户绑定的店铺ID集合
		List<TemuUserShopDO> userShops = userShopMapper.selectList(
				new LambdaQueryWrapperX<TemuUserShopDO>()
						.eq(TemuUserShopDO::getUserId, userId));
		
		if (CollUtil.isEmpty(userShops)) {
			log.info("[getOrderShippingPageByUser] 用户未绑定任何店铺, userId: {}", userId);
			return new PageResult<>(new ArrayList<>(), 0L);
		}
		
		// 提取店铺ID集合
		Set<Long> shopIds = userShops.stream()
				.map(TemuUserShopDO::getShopId)
				.collect(Collectors.toSet());
		log.info("[getOrderShippingPageByUser] 用户绑定的店铺数量: {}, shopIds: {}", shopIds.size(), shopIds);
		
		// 2. 调用通用的分页查询方法，传入限制的店铺ID集合
		return getOrderShippingPage(pageVO, shopIds);
	}
	
	// 分页查询待发货列表
	@Override
	public PageResult<TemuOrderShippingRespVO> getOrderShippingPage(TemuOrderShippingPageReqVO pageVO) {
		return getOrderShippingPage(pageVO, null);
	}
	
	// 内部方法：统一的分页查询实现
	private PageResult<TemuOrderShippingRespVO> getOrderShippingPage(TemuOrderShippingPageReqVO pageVO,
	                                                                 Set<Long> limitShopIds) {
		long totalStartTime = System.currentTimeMillis();
		log.info("[getOrderShippingPage] ==================== 开始执行分页查询 ====================");
		log.info("[getOrderShippingPage] 查询参数: pageVO={}, 限制店铺: {}", pageVO, limitShopIds);
		
		// ==================== 步骤1：查询匹配订单 ====================
		long step1StartTime = System.currentTimeMillis();
		List<TemuOrderDO> matchedOrders = getMatchedOrders(pageVO.getOrderStatus(), pageVO.getOrderNo(), pageVO.getCustomSku(),
				    pageVO.getCategoryIds());
		log.info("[getOrderShippingPage] [步骤1] 查询匹配订单: 耗时={}ms, 匹配订单数={}",
				System.currentTimeMillis() - step1StartTime,
				matchedOrders == null ? 0 : matchedOrders.size());
		
		if (matchedOrders != null && matchedOrders.isEmpty()) {
			log.info("[getOrderShippingPage] ==================== 查询结束：未找到匹配订单 - 总耗时: {}ms ====================",
					System.currentTimeMillis() - totalStartTime);
			return new PageResult<>(new ArrayList<>(), 0L, pageVO.getPageNo(), pageVO.getPageSize());
		}
		
		// ==================== 步骤2：构建查询条件 ====================
		long step2StartTime = System.currentTimeMillis();
		LambdaQueryWrapperX<TemuOrderShippingInfoDO> queryWrapper = buildShippingQueryWrapper(pageVO, matchedOrders,
				limitShopIds);
		log.info("[getOrderShippingPage] [步骤2] 构建查询条件: 耗时={}ms", System.currentTimeMillis() - step2StartTime);
		
		// ==================== 步骤3：查询总数 ====================
		long step3StartTime = System.currentTimeMillis();
		Long total = shippingInfoMapper.selectCount(queryWrapper);
		log.info("[getOrderShippingPage] [步骤3] 查询总数: 耗时={}ms, 总数={}",
				System.currentTimeMillis() - step3StartTime, total);
		
		if (total == 0) {
			log.info("[getOrderShippingPage] ==================== 查询结束：总数为0 - 总耗时: {}ms ====================",
					System.currentTimeMillis() - totalStartTime);
			return new PageResult<>(new ArrayList<>(), 0L, pageVO.getPageNo(), pageVO.getPageSize());
		}
		
		// ==================== 步骤4：执行分页查询 ====================
		long step4StartTime = System.currentTimeMillis();
		List<TemuOrderShippingInfoDO> list = shippingInfoMapper.selectList(queryWrapper
				.last("LIMIT " + (pageVO.getPageNo() - 1) * pageVO.getPageSize() + "," + pageVO.getPageSize()));
		log.info("[getOrderShippingPage] [步骤4] 分页查询: 耗时={}ms, 结果数量={}",
				System.currentTimeMillis() - step4StartTime, list.size());
		
		// ==================== 步骤5：查询关联数据 ====================
		long step5StartTime = System.currentTimeMillis();
		
		// 5.1 获取所有物流单号
		Set<String> trackingNumbers = list.stream()
				.map(TemuOrderShippingInfoDO::getTrackingNumber)
				.collect(Collectors.toSet());
		log.info("[getOrderShippingPage] [步骤5.1] 获取物流单号: 数量={}", trackingNumbers.size());
		
		// 5.2 查询这些物流单号关联的所有记录
		long step5_2StartTime = System.currentTimeMillis();
		List<TemuOrderShippingInfoDO> allRelatedShippings;

		// 优化：当使用orderStatus JOIN查询时，需要查询所有相关的物流记录
		if (pageVO.getOrderStatus() != null && !StringUtils.hasText(pageVO.getOrderNo()) &&
				!StringUtils.hasText(pageVO.getCustomSku()) && CollectionUtils.isEmpty(pageVO.getCategoryIds())) {
			// 使用JOIN查询获取所有相关的物流记录
			allRelatedShippings = shippingInfoMapper.selectList(
					new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
							.in(TemuOrderShippingInfoDO::getTrackingNumber, trackingNumbers)
							.inSql(TemuOrderShippingInfoDO::getOrderNo,
									"SELECT order_no FROM temu_order WHERE order_status = " + pageVO.getOrderStatus()));
		} else if (StringUtils.hasText(pageVO.getOrderNo()) || pageVO.getOrderStatus() != null) {
			allRelatedShippings = list;
		} else if (matchedOrders == null && !CollectionUtils.isEmpty(pageVO.getCategoryIds())) {
			allRelatedShippings = shippingInfoMapper.selectList(
					new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
							.in(TemuOrderShippingInfoDO::getTrackingNumber, trackingNumbers));
		} else {
			allRelatedShippings = shippingInfoMapper.selectList(
					new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
							.in(TemuOrderShippingInfoDO::getTrackingNumber, trackingNumbers));
		}
		log.info("[getOrderShippingPage] [步骤5.2] 查询关联物流记录: 耗时={}ms, 记录数量={}",
				System.currentTimeMillis() - step5_2StartTime, allRelatedShippings.size());
		
		// 5.3 获取所有相关的订单号和店铺ID
		Set<String> allOrderNos = allRelatedShippings.stream()
				.map(TemuOrderShippingInfoDO::getOrderNo)
				.collect(Collectors.toSet());
		Set<Long> shopIds = allRelatedShippings.stream()
				.map(TemuOrderShippingInfoDO::getShopId)
				.collect(Collectors.toSet());
		log.info("[getOrderShippingPage] [步骤5.3] 获取关联ID: 订单号数量={}, 店铺数量={}",
				allOrderNos.size(), shopIds.size());
		
		// 5.4 批量查询订单信息
		long orderQueryStartTime = System.currentTimeMillis();
		List<TemuOrderDO> orders = orderMapper.selectList(
				new LambdaQueryWrapperX<TemuOrderDO>()
						.select(TemuOrderDO::getId, TemuOrderDO::getOrderNo, TemuOrderDO::getProductTitle,
								TemuOrderDO::getOrderStatus, TemuOrderDO::getSku, TemuOrderDO::getSkc,
								TemuOrderDO::getSalePrice, TemuOrderDO::getCustomSku, TemuOrderDO::getQuantity,
								TemuOrderDO::getProductProperties, TemuOrderDO::getShopId,
								TemuOrderDO::getCustomImageUrls,
								TemuOrderDO::getCustomTextList, TemuOrderDO::getProductImgUrl,
								TemuOrderDO::getCategoryId,
								TemuOrderDO::getEffectiveImgUrl, TemuOrderDO::getComplianceUrl,
								TemuOrderDO::getOriginalQuantity,
								TemuOrderDO::getComplianceImageUrl, TemuOrderDO::getComplianceGoodsMergedUrl,
								TemuOrderDO::getIsCompleteProducerTask,TemuOrderDO::getSortingSequence,
								// 新增：查bookingTime
								TemuOrderDO::getBookingTime,
								// 新增：查isFoundAll
								TemuOrderDO::getIsFoundAll,
								TemuOrderDO::getSenderId)
						.in(TemuOrderDO::getOrderNo, allOrderNos));
		Map<String, List<TemuOrderDO>> orderMap = orders.stream()
				.collect(Collectors.groupingBy(TemuOrderDO::getOrderNo));
		log.info("[getOrderShippingPage] [步骤5.4] 批量查询订单: 耗时={}ms, 订单数量={}, 查询字段数={}",
				System.currentTimeMillis() - orderQueryStartTime, orders.size(), 16);
		
		// 5.5 批量查询店铺信息
		long shopQueryStartTime = System.currentTimeMillis();
		List<TemuShopDO> shops = shopMapper.selectList(
				new LambdaQueryWrapperX<TemuShopDO>()
						.in(TemuShopDO::getShopId, shopIds));
		Map<Long, TemuShopDO> shopMap = shops.stream()
				.collect(Collectors.toMap(TemuShopDO::getShopId, Function.identity(), (v1, v2) -> v1));
		log.info("[getOrderShippingPage] [步骤5.5] 批量查询店铺: 耗时={}ms, 店铺数量={}",
				System.currentTimeMillis() - shopQueryStartTime, shops.size());
		
		// 5.6 查询分类信息
		long categoryQueryStartTime = System.currentTimeMillis();
		Map<String, TemuProductCategoryDO> categoryMap = getCategoryMap(orders);
		log.info("[getOrderShippingPage] [步骤5.6] 批量查询分类: 耗时={}ms, 分类数量={}",
				System.currentTimeMillis() - categoryQueryStartTime, categoryMap.size());
		
		log.info("[getOrderShippingPage] [步骤5] 查询关联数据完成: 总耗时={}ms",
				System.currentTimeMillis() - step5StartTime);
		
		// ==================== 步骤6：组装返回结果 ====================
		long step6StartTime = System.currentTimeMillis();
		// 6. 组装返回结果时，设置 batchNo 到 TemuOrderListRespVO
		String isBatchNoEnabled = configApi.getConfigValueByKey("is_BatchNo_Enabled");
		boolean batchNoEnabled = false;
		if (StringUtils.hasText(isBatchNoEnabled)) {
			try {
				batchNoEnabled = Boolean.parseBoolean(isBatchNoEnabled);
			} catch (Exception e) {}
		}
		Map<Long, String> orderIdToBatchNo = new HashMap<>();
		Map<String, Long> customSkuToOrderId = new HashMap<>();
		if (batchNoEnabled) {
			// 1. 收集所有订单ID，若id为null则用customSku查id
			List<Long> allOrderIds = new ArrayList<>();
			for (TemuOrderDO order : orders) {
				if (order.getId() != null) {
					allOrderIds.add(order.getId());
				} else if (order.getCustomSku() != null) {
					List<TemuOrderDO> bySku = orderMapper.selectByCustomSku(order.getCustomSku());
					if (bySku != null && !bySku.isEmpty()) {
						Long foundId = bySku.get(0).getId();
						allOrderIds.add(foundId);
						customSkuToOrderId.put(order.getCustomSku(), foundId);
					}
				}
			}
			if (!allOrderIds.isEmpty()) {
				List<TemuOrderBatchRelationDO> relations = temuOrderBatchRelationMapper.selectByOrderIds(allOrderIds);
				Map<Long, Long> orderIdToBatchId = relations.stream().collect(Collectors.toMap(TemuOrderBatchRelationDO::getOrderId, TemuOrderBatchRelationDO::getBatchId));
				List<Long> batchIds = relations.stream().map(TemuOrderBatchRelationDO::getBatchId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
				if (!batchIds.isEmpty()) {
					List<TemuOrderBatchDO> batchList = temuOrderBatchMapper.selectBatchByIds(batchIds);
					Map<Long, String> batchIdToBatchNo = batchList.stream().collect(Collectors.toMap(TemuOrderBatchDO::getId, TemuOrderBatchDO::getBatchNo));
					for (Map.Entry<Long, Long> entry : orderIdToBatchId.entrySet()) {
						Long orderId = entry.getKey();
						Long batchId = entry.getValue();
						String batchNo = batchIdToBatchNo.get(batchId);
						if (batchNo != null) {
							orderIdToBatchNo.put(orderId, batchNo);
						}
					}
				}
			}
		}
		List<TemuOrderShippingRespVO> voList = buildOrderShippingRespList(allRelatedShippings, orderMap, shopMap,
				categoryMap, orderIdToBatchNo);

		// 新增：批量查询shippedOperatorId对应的昵称
		Set<Long> operatorIds = voList.stream()
			.map(TemuOrderShippingRespVO::getShippedOperatorId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		Map<Long, String> operatorNicknameMap = new HashMap<>();
		if (!operatorIds.isEmpty()) {
			List<AdminUserDO> userList = adminUserMapper.selectBatchIds(operatorIds);
			operatorNicknameMap = userList.stream().collect(Collectors.toMap(
				AdminUserDO::getId,
				AdminUserDO::getNickname
			));
		}
		for (TemuOrderShippingRespVO vo : voList) {
			if (vo.getShippedOperatorId() != null) {
				vo.setShippedOperatorNickname(operatorNicknameMap.get(vo.getShippedOperatorId()));
			}
		}

		log.info("[getOrderShippingPage] [步骤6] 组装结果: 耗时={}ms, 结果数量={}",
				System.currentTimeMillis() - step6StartTime, voList.size());
		
		// ==================== 查询完成，输出统计信息 ====================
		log.info("[getOrderShippingPage] ==================== 查询完成 ====================");
		log.info("[getOrderShippingPage] 统计信息: 总耗时={}ms, 总记录数={}, 当前页记录数={}, 页码={}, 每页大小={}",
				System.currentTimeMillis() - totalStartTime, total, voList.size(),
				pageVO.getPageNo(), pageVO.getPageSize());
		
		return new PageResult<>(voList, total, pageVO.getPageNo(), pageVO.getPageSize());
	}
	
	/**
	 * 生成记录的唯一匹配键（物流单号+shopId+orderNo）
	 */
	private String generateMatchKey(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO vo) {
		return vo.getTrackingNumber() + "_" + vo.getShopId() + "_" + vo.getOrderNo();
	}
	
	/**
	 * 批量更新订单状态，同时更新对应的物流订单发货状态
	 *
	 * @param orderIds       订单ID列表
	 * @param orderStatus    订单状态
	 * @param trackingNumber 物流单号
	 * @return 是否更新成功
	 */
	@Transactional(rollbackFor = Exception.class)
	public Boolean batchUpdateOrderStatus(List<Long> orderIds, Integer orderStatus, String trackingNumber) {
		if (CollUtil.isEmpty(orderIds)) {
			return false;
		}
		
		log.info("[batchUpdateOrderStatus] 开始执行批量更新, orderIds={}, orderStatus={}, trackingNumber={}",
				orderIds, orderStatus, trackingNumber);
		
		// 1. 更新订单状态
		List<TemuOrderDO> updateList = orderIds.stream().map(orderId -> {
			TemuOrderDO order = new TemuOrderDO();
			order.setId(orderId);
			order.setOrderStatus(orderStatus);
			return order;
		}).collect(Collectors.toList());
		
		// 2. 执行订单状态更新
		boolean orderUpdateCount = orderMapper.updateBatch(updateList);
		log.info("[batchUpdateOrderStatus] 订单状态更新完成, 更新数量: {}", orderUpdateCount);
		
		// 3. 如果提供了物流单号，更新物流订单发货状态
		if (StringUtils.hasText(trackingNumber)) {
			// 查询订单编号
			List<TemuOrderDO> orders = orderMapper.selectBatchIds(orderIds);
			log.info("[batchUpdateOrderStatus] 查询到订单数量: {}", orders.size());
			
			if (!orders.isEmpty()) {
				// 收集订单编号
				Set<String> orderNos = orders.stream()
						.map(TemuOrderDO::getOrderNo)
						.collect(Collectors.toSet());
				log.info("[batchUpdateOrderStatus] 待更新的订单编号: {}", orderNos);
				
				// 获取当前操作人ID
				Long operatorId = null;
				try {
					operatorId = SecurityFrameworkUtils.getLoginUserId();
					log.info("[batchUpdateOrderStatus] 获取到操作人ID: {}", operatorId);
				} catch (Exception e) {
					log.warn("[batchUpdateOrderStatus] 获取操作人ID失败: {}", e.getMessage());
				}
				
				// 更新物流订单发货状态
				TemuOrderShippingInfoDO updateShipping = new TemuOrderShippingInfoDO();
				updateShipping.setShippingStatus(1); // 设置为已发货
				updateShipping.setUpdateTime(LocalDateTime.now());
				
				// 如果获取到操作人ID，则设置shippedOperatorId字段
				if (operatorId != null) {
					updateShipping.setShippedOperatorId(operatorId);
					log.info("[batchUpdateOrderStatus] 设置shippedOperatorId: {}", operatorId);
				}
				
				// 先查询符合条件的记录
				List<TemuOrderShippingInfoDO> matchingRecords = shippingInfoMapper.selectList(
						new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
								.eq(TemuOrderShippingInfoDO::getTrackingNumber, trackingNumber)
								.in(TemuOrderShippingInfoDO::getOrderNo, orderNos));
				
				log.info("[batchUpdateOrderStatus] 找到匹配的物流记录数量: {}", matchingRecords.size());
				
				if (!matchingRecords.isEmpty()) {
					// 执行更新
					int shippingUpdateCount = shippingInfoMapper.update(updateShipping,
							new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
									.eq(TemuOrderShippingInfoDO::getTrackingNumber, trackingNumber)
									.in(TemuOrderShippingInfoDO::getOrderNo, orderNos));
					
					log.info("[batchUpdateOrderStatus] 物流订单状态更新完成, 更新数量: {}, 操作人ID: {}", 
							shippingUpdateCount, operatorId);
					
					// 新增：插入工作人员任务记录（发货）
					for (TemuOrderDO order : orders) {
						TemuWorkerTaskDO workerTask = new TemuWorkerTaskDO();
						workerTask.setWorkerId(operatorId);
						// 查询操作人昵称
						String workerName = null;
						if (operatorId != null) {
							AdminUserDO user = adminUserMapper.selectById(operatorId);
							if (user != null) {
								workerName = user.getNickname();
							}
						}
						workerTask.setWorkerName(workerName);
						workerTask.setTaskType((byte)3); // 3:发货
						workerTask.setTaskStatus((byte)1); // 1:已完成
						workerTask.setOrderId(order.getId());
						workerTask.setOrderNo(order.getOrderNo());
						workerTask.setCustomSku(order.getCustomSku());
						// 统计当前用户已处理过的不同 custom_sku 数量
						int skuQuantity = temuWorkerTaskMapper.selectDistinctCustomSkuCountByWorkerId(operatorId);
						boolean alreadyProcessed = temuWorkerTaskMapper.existsByWorkerIdAndCustomSku(operatorId, order.getCustomSku());
						if (!alreadyProcessed) {
							skuQuantity += 1;
						}
						workerTask.setSkuQuantity(skuQuantity);
						workerTask.setTaskCompleteTime(LocalDateTime.now());
						workerTask.setShopId(order.getShopId());
						temuWorkerTaskMapper.insert(workerTask);
					}
				} else {
					log.warn("[batchUpdateOrderStatus] 未找到匹配的物流记录, trackingNumber={}, orderNos={}",
							trackingNumber, orderNos);
				}
			}
		}
		
		return true;
	}
	
	/**
	 * 根据订单状态和订单号查询匹配的订单
	 *
	 * @param orderStatus 订单状态
	 * @param orderNo     订单号
	 * @return 匹配的订单列表
	 */
	private List<TemuOrderDO> getMatchedOrders(Integer orderStatus, String orderNo, String customSku,List<String> categoryIds) {
		// 优化：当有类目ID条件时，不在这里查询，而是在物流表中直接JOIN查询
		boolean hasOtherConditions = orderStatus != null || StringUtils.hasText(orderNo) || StringUtils.hasText(customSku);
		boolean hasCategoryCondition = !CollectionUtils.isEmpty(categoryIds);

		// 如果只有类目条件，返回null，避免查询大量订单数据
		if (!hasOtherConditions && hasCategoryCondition) {
			log.info("[getMatchedOrders] 只有类目条件，跳过订单查询，将在物流表中直接JOIN查询");
			return null;
		}

		// 如果没有任何条件，返回null
		if (!hasOtherConditions && !hasCategoryCondition) {
			return null;
		}

		// 优化：当有orderStatus条件时，只查询订单号，避免查询大量订单数据
		if (orderStatus != null && !StringUtils.hasText(orderNo) && !StringUtils.hasText(customSku) && CollectionUtils.isEmpty(categoryIds)) {
			log.info("[getMatchedOrders] 只有orderStatus条件，使用优化查询策略");
			return getMatchedOrdersByStatusOnly(orderStatus);
		}

		LambdaQueryWrapperX<TemuOrderDO> orderWrapper = new LambdaQueryWrapperX<>();
		orderWrapper.select(TemuOrderDO::getId, TemuOrderDO::getOrderNo, TemuOrderDO::getProductTitle,
				TemuOrderDO::getOrderStatus, TemuOrderDO::getSku, TemuOrderDO::getSkc,
				TemuOrderDO::getSalePrice, TemuOrderDO::getCustomSku, TemuOrderDO::getQuantity,
				TemuOrderDO::getProductProperties, TemuOrderDO::getShopId, TemuOrderDO::getCustomImageUrls,
				TemuOrderDO::getCustomTextList, TemuOrderDO::getProductImgUrl, TemuOrderDO::getCategoryId,
				TemuOrderDO::getEffectiveImgUrl);
		
		// 添加条件查询
		if (orderStatus != null) {
			orderWrapper.eq(TemuOrderDO::getOrderStatus, orderStatus);
		}
		if (StringUtils.hasText(orderNo)) {
			orderWrapper.eq(TemuOrderDO::getOrderNo, orderNo);
		}
		 if (StringUtils.hasText(customSku)) {
		 	orderWrapper.like(TemuOrderDO::getCustomSku, customSku);
		 }
//		if (customSkuList != null && !customSkuList.isEmpty()) {
//        	orderWrapper.in(TemuOrderDO::getCustomSku, customSkuList);
//		} else if (StringUtils.hasText(customSku)) {
//			orderWrapper.like(TemuOrderDO::getCustomSku, customSku);
//		}
		if (!CollectionUtils.isEmpty(categoryIds)) {
			orderWrapper.in(TemuOrderDO::getCategoryId, categoryIds);
		}
		
		return orderMapper.selectList(orderWrapper);
	}

	/**
	 * 优化方法：当只有orderStatus条件时，只查询订单号，避免查询大量订单数据
	 */
	private List<TemuOrderDO> getMatchedOrdersByStatusOnly(Integer orderStatus) {
		// 只查询订单号，不查询其他字段
		LambdaQueryWrapperX<TemuOrderDO> orderWrapper = new LambdaQueryWrapperX<>();
		orderWrapper.select(TemuOrderDO::getOrderNo)  // 只查询订单号
				.eq(TemuOrderDO::getOrderStatus, orderStatus);

		List<TemuOrderDO> orders = orderMapper.selectList(orderWrapper);

		// 创建完整的订单对象，但只设置订单号
		return orders.stream().map(order -> {
			TemuOrderDO fullOrder = new TemuOrderDO();
			fullOrder.setOrderNo(order.getOrderNo());
			return fullOrder;
		}).collect(Collectors.toList());
	}

	/**
	 * 构建物流信息查询条件
	 */
	private LambdaQueryWrapperX<TemuOrderShippingInfoDO> buildShippingQueryWrapper(TemuOrderShippingPageReqVO pageVO,
	                                                                               List<TemuOrderDO> matchedOrders,
	                                                                               Set<Long> limitShopIds) {
		long startTime = System.currentTimeMillis();
		LambdaQueryWrapperX<TemuOrderShippingInfoDO> queryWrapper = new LambdaQueryWrapperX<>();
		
		// 打印完整的请求参数
		log.info("[buildShippingQueryWrapper] 开始处理查询, 请求参数: pageVO={}", pageVO);

		// 优化：当有orderStatus条件且没有其他复杂条件时，使用JOIN查询
		if (pageVO.getOrderStatus() != null && !StringUtils.hasText(pageVO.getOrderNo()) &&
				!StringUtils.hasText(pageVO.getCustomSku()) && CollectionUtils.isEmpty(pageVO.getCategoryIds())) {
			log.info("[buildShippingQueryWrapper] 使用orderStatus JOIN查询优化");
			String joinQuery = buildOrderStatusJoinQuery(pageVO, limitShopIds);
			queryWrapper.inSql(TemuOrderShippingInfoDO::getId, joinQuery);
			queryWrapper.orderByDesc(TemuOrderShippingInfoDO::getCreateTime);
			return queryWrapper;
		}

		// 构建基础查询条件
		StringBuilder subQuery = new StringBuilder("SELECT MAX(id) as id FROM temu_order_shipping_info WHERE 1=1");
		
		// 处理店铺ID条件
		if (pageVO.getShopId() != null) {
			subQuery.append(" AND shop_id = ").append(pageVO.getShopId());
		} else if (limitShopIds != null && !limitShopIds.isEmpty()) {
			subQuery.append(" AND shop_id IN (")
					.append(String.join(",", limitShopIds.stream().map(String::valueOf).collect(Collectors.toList())))
					.append(")");
		}
		
		// 处理 senderId 条件
		if (pageVO.getSenderId() != null) {
			subQuery.append(" AND order_no IN (SELECT order_no FROM temu_order WHERE sender_id = ")
					.append(pageVO.getSenderId()).append(")");
		}

		// 处理物流单号
		if (StringUtils.hasText(pageVO.getTrackingNumber())) {
			subQuery.append(" AND tracking_number LIKE '%").append(pageVO.getTrackingNumber()).append("%'");
		}
		
		// 处理是否加急条件
		if (pageVO.getIsUrgent() != null) {
			subQuery.append(" AND is_urgent = ").append(pageVO.getIsUrgent() ? "1" : "0");
		}
		
		// 处理物流单序号条件
		if (pageVO.getDailySequence() != null) {
			subQuery.append(" AND daily_sequence = ").append(pageVO.getDailySequence());
		}
		
		// 新增：处理发货状态条件
		if (pageVO.getShippingStatus() != null) {
			subQuery.append(" AND shipping_status = ").append(pageVO.getShippingStatus());
		}
		
		// 处理创建时间条件
		LocalDateTime beginTime = null;
		LocalDateTime endTime = null;
		if (pageVO.getCreateTime() != null && pageVO.getCreateTime().length == 2) {
			// 打印原始时间参数
			log.info("[buildShippingQueryWrapper] 原始时间参数 - createTime[0]: {}, createTime[1]: {}, 类型: {}",
					pageVO.getCreateTime()[0],
					pageVO.getCreateTime()[1],
					pageVO.getCreateTime()[0].getClass().getName());
			
			try {
				// 转换为数据库时区
				beginTime = pageVO.getCreateTime()[0].atStartOfDay();
				endTime = pageVO.getCreateTime()[1].atTime(23, 59, 59);
				
				log.info("[buildShippingQueryWrapper] 处理创建时间条件 - 开始时间: {}, 结束时间: {}", beginTime, endTime);
				
				// 直接使用字符串格式化，确保时间格式正确
				String startTimeStr = pageVO.getCreateTime()[0].toString() + " 00:00:00";
				String endTimeStr = pageVO.getCreateTime()[1].toString() + " 23:59:59";
				
				// 在子查询中使用格式化后的时间字符串
				subQuery.append(" AND create_time >= '").append(startTimeStr).append("'")
						.append(" AND create_time <= '").append(endTimeStr).append("'");
				
				// 在主查询中使用LocalDateTime对象
				queryWrapper.ge(TemuOrderShippingInfoDO::getCreateTime, beginTime)
						.le(TemuOrderShippingInfoDO::getCreateTime, endTime);
				
				// 打印最终SQL中的时间条件
				log.info("[buildShippingQueryWrapper] SQL时间条件 - startTimeStr: {}, endTimeStr: {}",
						startTimeStr, endTimeStr);
			} catch (Exception e) {
				log.error("[buildShippingQueryWrapper] 时间参数处理异常", e);
			}
		} else {
			log.info("[buildShippingQueryWrapper] 未提供时间参数或参数格式不正确: createTime={}",
					pageVO.getCreateTime() != null ? pageVO.getCreateTime().length : null);
		}
		
		// 处理订单号条件
		if (matchedOrders != null && !matchedOrders.isEmpty()) {
			Set<String> orderNos = matchedOrders.stream()
					.map(TemuOrderDO::getOrderNo)
					.collect(Collectors.toSet());
			subQuery.append(" AND order_no IN ('")
					.append(String.join("','", orderNos))
					.append("')");
		}

		// 新增：定制文字模糊查询（加开关）
		String isCustomTextQueryEnabled = configApi.getConfigValueByKey("temu.order-shipping.customText-query.enabled");
		boolean customTextQueryEnabled = false;
		if (StringUtils.hasText(isCustomTextQueryEnabled)) {
			try {
				customTextQueryEnabled = Boolean.parseBoolean(isCustomTextQueryEnabled);
			} catch (Exception e) {}
		}
		if (customTextQueryEnabled && StringUtils.hasText(pageVO.getCustomTextList())) {
			List<TemuOrderDO> textMatchedOrders = orderMapper.selectList(
				new LambdaQueryWrapperX<TemuOrderDO>()
					.select(TemuOrderDO::getOrderNo)
					.like(TemuOrderDO::getCustomTextList, pageVO.getCustomTextList())
			);
			if (!textMatchedOrders.isEmpty()) {
				Set<String> textOrderNos = textMatchedOrders.stream().map(TemuOrderDO::getOrderNo).collect(Collectors.toSet());
				subQuery.append(" AND order_no IN ('")
						.append(String.join("','", textOrderNos))
						.append("')");
			} else {
				subQuery.append(" AND 1=0");
			}
		}
		// 新增：批次编号模糊查询（加开关）
		String isBatchNoQueryEnabled = configApi.getConfigValueByKey("temu.order-shipping.batchNo-query.enabled");
		boolean batchNoQueryEnabled = false;
		if (StringUtils.hasText(isBatchNoQueryEnabled)) {
			try {
				batchNoQueryEnabled = Boolean.parseBoolean(isBatchNoQueryEnabled);
			} catch (Exception e) {}
		}
		if (batchNoQueryEnabled && StringUtils.hasText(pageVO.getBatchNo())) {
			List<TemuOrderBatchDO> batchList = temuOrderBatchMapper.selectList(
				new LambdaQueryWrapperX<TemuOrderBatchDO>()
					.like(TemuOrderBatchDO::getBatchNo, pageVO.getBatchNo())
			);
			if (!batchList.isEmpty()) {
				List<Long> batchIds = batchList.stream().map(TemuOrderBatchDO::getId).collect(Collectors.toList());
				List<TemuOrderBatchRelationDO> relations = temuOrderBatchRelationMapper.selectList(
					new LambdaQueryWrapperX<TemuOrderBatchRelationDO>()
						.in(TemuOrderBatchRelationDO::getBatchId, batchIds)
				);
				if (!relations.isEmpty()) {
					List<Long> orderIds = relations.stream().map(TemuOrderBatchRelationDO::getOrderId).collect(Collectors.toList());
					if (!orderIds.isEmpty()) {
						Set<String> batchOrderNos = orderMapper.selectBatchIds(orderIds)
							.stream().map(TemuOrderDO::getOrderNo).collect(Collectors.toSet());
						subQuery.append(" AND order_no IN ('")
								.append(String.join("','", batchOrderNos))
								.append("')");
					} else {
						subQuery.append(" AND 1=0");
					}
				} else {
					subQuery.append(" AND 1=0");
				}
			} else {
				subQuery.append(" AND 1=0");
			}
		}

		// 优化：处理类目条件 - 当只有类目条件时，使用JOIN查询
		if (matchedOrders == null && !CollectionUtils.isEmpty(pageVO.getCategoryIds())) {
			log.info("[buildShippingQueryWrapper] 使用类目JOIN查询优化");
			// 构建JOIN查询，直接在物流表中关联订单表查询类目
			String joinQuery = buildCategoryJoinQuery(pageVO, limitShopIds);
			queryWrapper.inSql(TemuOrderShippingInfoDO::getId, joinQuery);
			queryWrapper.orderByDesc(TemuOrderShippingInfoDO::getCreateTime);
			return queryWrapper;
		}

		// 新增：处理是否找齐条件
		if (pageVO.getIsFoundAll() != null) {
			subQuery.append(" AND order_no IN (SELECT order_no FROM temu_order WHERE is_found_all = ")
					.append(pageVO.getIsFoundAll()).append(")");
		}

		// 按物流单号分组，如果指定了订单号或订单状态，则同时按订单号分组
		if (StringUtils.hasText(pageVO.getOrderNo()) || pageVO.getOrderStatus() != null) {
			subQuery.append(" GROUP BY tracking_number, order_no");
		} else {
			subQuery.append(" GROUP BY tracking_number");
		}
		
		// 使用子查询
		queryWrapper.inSql(TemuOrderShippingInfoDO::getId, subQuery.toString());
		
		// 按创建时间降序排序
		queryWrapper.orderByDesc(TemuOrderShippingInfoDO::getCreateTime);
		
		String finalSql = queryWrapper.getTargetSql();
		log.info("[buildShippingQueryWrapper] 最终生成的SQL: {}", finalSql);
		
		return queryWrapper;
	}

	/**
	 * 构建orderStatus JOIN查询
	 * 直接在物流表中关联订单表进行状态查询，避免先查询大量订单
	 */
	private String buildOrderStatusJoinQuery(TemuOrderShippingPageReqVO pageVO, Set<Long> limitShopIds) {
		StringBuilder joinQuery = new StringBuilder();
		joinQuery.append("SELECT MAX(s.id) as id FROM temu_order_shipping_info s ");
		joinQuery.append("INNER JOIN temu_order o ON s.order_no = o.order_no ");
		joinQuery.append("WHERE 1=1");

		// 处理店铺ID条件
		if (pageVO.getShopId() != null) {
			joinQuery.append(" AND s.shop_id = ").append(pageVO.getShopId());
		} else if (limitShopIds != null && !limitShopIds.isEmpty()) {
			joinQuery.append(" AND s.shop_id IN (")
					.append(String.join(",", limitShopIds.stream().map(String::valueOf).collect(Collectors.toList())))
					.append(")");
		}

		// 处理物流单号
		if (StringUtils.hasText(pageVO.getTrackingNumber())) {
			joinQuery.append(" AND s.tracking_number LIKE '%").append(pageVO.getTrackingNumber()).append("%'");
		}

		// 处理是否加急条件
		if (pageVO.getIsUrgent() != null) {
			joinQuery.append(" AND s.is_urgent = ").append(pageVO.getIsUrgent() ? "1" : "0");
		}

		// 处理物流单序号条件
		if (pageVO.getDailySequence() != null) {
			joinQuery.append(" AND s.daily_sequence = ").append(pageVO.getDailySequence());
		}

		// 新增：处理发货状态条件
		if (pageVO.getShippingStatus() != null) {
			joinQuery.append(" AND s.shipping_status = ").append(pageVO.getShippingStatus());
		}

		// 处理创建时间条件
		if (pageVO.getCreateTime() != null && pageVO.getCreateTime().length == 2) {
			String startTimeStr = pageVO.getCreateTime()[0].toString() + " 00:00:00";
			String endTimeStr = pageVO.getCreateTime()[1].toString() + " 23:59:59";
			joinQuery.append(" AND s.create_time >= '").append(startTimeStr).append("'")
					.append(" AND s.create_time <= '").append(endTimeStr).append("'");
		}

		// 处理订单状态条件
		if (pageVO.getOrderStatus() != null) {
			joinQuery.append(" AND o.order_status = ").append(pageVO.getOrderStatus());
		}

		// 按物流单号分组
		joinQuery.append(" GROUP BY s.tracking_number");

		log.info("[buildOrderStatusJoinQuery] 生成的JOIN查询SQL: {}", joinQuery.toString());
		return joinQuery.toString();
	}

	/**
	 * 构建类目JOIN查询
	 * 直接在物流表中关联订单表进行类目查询，避免先查询大量订单
	 */
	private String buildCategoryJoinQuery(TemuOrderShippingPageReqVO pageVO, Set<Long> limitShopIds) {
		StringBuilder joinQuery = new StringBuilder();
		joinQuery.append("SELECT MAX(s.id) as id FROM temu_order_shipping_info s ");
		joinQuery.append("INNER JOIN temu_order o ON s.order_no = o.order_no ");
		joinQuery.append("WHERE 1=1");

		// 处理店铺ID条件
		if (pageVO.getShopId() != null) {
			joinQuery.append(" AND s.shop_id = ").append(pageVO.getShopId());
		} else if (limitShopIds != null && !limitShopIds.isEmpty()) {
			joinQuery.append(" AND s.shop_id IN (")
					.append(String.join(",", limitShopIds.stream().map(String::valueOf).collect(Collectors.toList())))
					.append(")");
		}

		// 处理物流单号
		if (StringUtils.hasText(pageVO.getTrackingNumber())) {
			joinQuery.append(" AND s.tracking_number LIKE '%").append(pageVO.getTrackingNumber()).append("%'");
		}

		// 处理是否加急条件
		if (pageVO.getIsUrgent() != null) {
			joinQuery.append(" AND s.is_urgent = ").append(pageVO.getIsUrgent() ? "1" : "0");
		}

		// 处理物流单序号条件
		if (pageVO.getDailySequence() != null) {
			joinQuery.append(" AND s.daily_sequence = ").append(pageVO.getDailySequence());
		}

		// 新增：处理发货状态条件
		if (pageVO.getShippingStatus() != null) {
			joinQuery.append(" AND s.shipping_status = ").append(pageVO.getShippingStatus());
		}

		// 处理创建时间条件
		if (pageVO.getCreateTime() != null && pageVO.getCreateTime().length == 2) {
			String startTimeStr = pageVO.getCreateTime()[0].toString() + " 00:00:00";
			String endTimeStr = pageVO.getCreateTime()[1].toString() + " 23:59:59";
			joinQuery.append(" AND s.create_time >= '").append(startTimeStr).append("'")
					.append(" AND s.create_time <= '").append(endTimeStr).append("'");
		}

		// 处理批次条件（order_no IN）
		String isBatchNoQueryEnabled = configApi.getConfigValueByKey("temu.order-shipping.batchNo-query.enabled");
		boolean batchNoQueryEnabled = false;
		if (StringUtils.hasText(isBatchNoQueryEnabled)) {
			try {
				batchNoQueryEnabled = Boolean.parseBoolean(isBatchNoQueryEnabled);
			} catch (Exception e) {}
		}
		if (batchNoQueryEnabled && StringUtils.hasText(pageVO.getBatchNo())) {
			List<TemuOrderBatchDO> batchList = temuOrderBatchMapper.selectList(
				new LambdaQueryWrapperX<TemuOrderBatchDO>()
					.like(TemuOrderBatchDO::getBatchNo, pageVO.getBatchNo())
			);
			if (!batchList.isEmpty()) {
				List<Long> batchIds = batchList.stream().map(TemuOrderBatchDO::getId).collect(Collectors.toList());
				List<TemuOrderBatchRelationDO> relations = temuOrderBatchRelationMapper.selectList(
					new LambdaQueryWrapperX<TemuOrderBatchRelationDO>()
						.in(TemuOrderBatchRelationDO::getBatchId, batchIds)
				);
				if (!relations.isEmpty()) {
					List<Long> orderIds = relations.stream().map(TemuOrderBatchRelationDO::getOrderId).collect(Collectors.toList());
					if (!orderIds.isEmpty()) {
						Set<String> batchOrderNos = orderMapper.selectBatchIds(orderIds)
							.stream().map(TemuOrderDO::getOrderNo).collect(Collectors.toSet());
						if (!batchOrderNos.isEmpty()) {
							joinQuery.append(" AND o.order_no IN ('")
								.append(String.join("','", batchOrderNos))
								.append("')");
						}
					}
				}
			}
		}

		// 处理类目条件（category_id IN）
		if (!CollectionUtils.isEmpty(pageVO.getCategoryIds())) {
			joinQuery.append(" AND o.category_id IN ('")
					.append(String.join("','", pageVO.getCategoryIds()))
					.append("')");
		}

		// 按物流单号分组
		joinQuery.append(" GROUP BY s.tracking_number");

		log.info("[buildCategoryJoinQuery] 生成的JOIN查询SQL: {}", joinQuery.toString());
		return joinQuery.toString();
	}

	/**
	 * 批量查询分类信息
	 */
	private Map<String, TemuProductCategoryDO> getCategoryMap(List<TemuOrderDO> orders) {
		Map<String, TemuProductCategoryDO> categoryMap = new HashMap<>();
		Set<String> categoryIds = orders.stream()
				.map(TemuOrderDO::getCategoryId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		
		if (!categoryIds.isEmpty()) {
			List<Long> validCategoryIds = new ArrayList<>();
			Map<Long, String> catIdMapping = new HashMap<>();
			for (String categoryId : categoryIds) {
				try {
					Long catId = Long.parseLong(categoryId);
					validCategoryIds.add(catId);
					catIdMapping.put(catId, categoryId);
				} catch (NumberFormatException e) {
					log.warn("[getCategoryMap] 转换分类ID失败: {}", categoryId);
				}
			}
			
			if (!validCategoryIds.isEmpty()) {
				List<TemuProductCategoryDO> categories = categoryMapper.selectList(
						new LambdaQueryWrapperX<TemuProductCategoryDO>()
								.in(TemuProductCategoryDO::getCategoryId, validCategoryIds));
				if (categories != null) {
					for (TemuProductCategoryDO category : categories) {
						String originalCategoryId = catIdMapping.get(category.getCategoryId());
						if (originalCategoryId != null) {
							categoryMap.put(originalCategoryId, category);
						}
					}
				}
			}
		}
		return categoryMap;
	}
	
	/**
	 * 组装订单物流返回结果
	 */
	private List<TemuOrderShippingRespVO> buildOrderShippingRespList(List<TemuOrderShippingInfoDO> shippingList,
	                                                                 Map<String, List<TemuOrderDO>> orderMap,
	                                                                 Map<Long, TemuShopDO> shopMap,
	                                                                 Map<String, TemuProductCategoryDO> categoryMap,
	                                                                 Map<Long, String> orderIdToBatchNo) {
		// 按物流单号分组
		Map<String, List<TemuOrderShippingInfoDO>> shippingGroupMap = shippingList.stream()
				.collect(Collectors.groupingBy(TemuOrderShippingInfoDO::getTrackingNumber));
		
		List<TemuOrderShippingRespVO> voList = new ArrayList<>();

		// 收集所有发货人ID
		Set<Long> senderIds = new HashSet<>();
		for (List<TemuOrderDO> orders : orderMap.values()) {
			for (TemuOrderDO order : orders) {
				if (order.getSenderId() != null) {
					senderIds.add(order.getSenderId());
				}
			}
		}

		// 批量查询发货人信息
		Map<Long, String> senderNicknameMap = new HashMap<>();
		if (!senderIds.isEmpty()) {
			List<AdminUserDO> senderList = adminUserMapper.selectBatchIds(senderIds);
			senderNicknameMap = senderList.stream()
					.collect(Collectors.toMap(
							AdminUserDO::getId,
							AdminUserDO::getNickname
					));
		}
		
		// 对分组后的Map按照每组最新创建时间降序排序
		List<Map.Entry<String, List<TemuOrderShippingInfoDO>>> sortedEntries = new ArrayList<>(
				shippingGroupMap.entrySet());
		sortedEntries.sort((e1, e2) -> {
			LocalDateTime time1 = e1.getValue().stream()
					.map(TemuOrderShippingInfoDO::getCreateTime)
					.max(LocalDateTime::compareTo)
					.orElse(LocalDateTime.MIN);
			LocalDateTime time2 = e2.getValue().stream()
					.map(TemuOrderShippingInfoDO::getCreateTime)
					.max(LocalDateTime::compareTo)
					.orElse(LocalDateTime.MIN);
			return time2.compareTo(time1);
		});
		
		for (Map.Entry<String, List<TemuOrderShippingInfoDO>> entry : sortedEntries) {
			String trackingNumber = entry.getKey();
			List<TemuOrderShippingInfoDO> groupedShippings = entry.getValue();
			
			// 使用最新的记录作为基础信息
			TemuOrderShippingInfoDO latestShipping = groupedShippings.stream()
					.max(Comparator.comparing(TemuOrderShippingInfoDO::getCreateTime))
					.orElse(groupedShippings.get(0));
			
			TemuOrderShippingRespVO vo = BeanUtils.toBean(latestShipping, TemuOrderShippingRespVO.class);
			
			// 设置店铺信息
			Long shopId = latestShipping.getShopId();
			vo.setShopId(shopId);
			TemuShopDO shop = shopMap.get(shopId);
			if (shop != null) {
				vo.setShopName(shop.getShopName());
			}
			
			// 设置发货操作人ID
			vo.setShippedOperatorId(latestShipping.getShippedOperatorId());
			
			// 按订单号分组处理订单
			Map<String, List<TemuOrderShippingInfoDO>> orderNoGroup = groupedShippings.stream()
					.collect(Collectors.groupingBy(TemuOrderShippingInfoDO::getOrderNo));
			
			List<TemuOrderNoListRespVO> orderNoList = new ArrayList<>();
			for (Map.Entry<String, List<TemuOrderShippingInfoDO>> orderEntry : orderNoGroup.entrySet()) {
				String orderNo = orderEntry.getKey();
				List<TemuOrderDO> orderList = orderMap.get(orderNo);
				
				// 如果orderList为null或为空，跳过这个订单号
				if (CollectionUtils.isEmpty(orderList)) {
					continue;
				}
				
				TemuOrderNoListRespVO orderNoVO = new TemuOrderNoListRespVO();
				orderNoVO.setOrderNo(orderNo);
				
				// 从最新的物流信息中获取图片URL
				TemuOrderShippingInfoDO latestOrderShipping = orderEntry.getValue().stream()
						.max(Comparator.comparing(TemuOrderShippingInfoDO::getCreateTime))
						.orElse(orderEntry.getValue().get(0));
				orderNoVO.setExpressImageUrl(latestOrderShipping.getExpressImageUrl());
				orderNoVO.setExpressOutsideImageUrl(latestOrderShipping.getExpressOutsideImageUrl());
				orderNoVO.setExpressSkuImageUrl(latestOrderShipping.getExpressSkuImageUrl());
				
				// 设置该订单号下的所有订单
				List<TemuOrderListRespVO> orderListVOs = new ArrayList<>();
				for (TemuOrderDO order : orderList) {
					TemuOrderListRespVO orderVO = BeanUtils.toBean(order, TemuOrderListRespVO.class);
					orderVO.setIsFoundAll(order.getIsFoundAll());
					orderVO.setSenderId(order.getSenderId());
					// 设置发货人姓名
					if (order.getSenderId() != null) {
						orderVO.setSenderName(senderNicknameMap.get(order.getSenderId()));
					}
					if (order.getCategoryId() != null) {
						TemuProductCategoryDO category = categoryMap.get(order.getCategoryId());
						if (category != null && category.getOldType() != null && shop != null
								&& shop.getOldTypeUrl() != null) {
							Object url = shop.getOldTypeUrl().get(category.getOldType());
							if (url != null) {
								orderVO.setOldTypeUrl(url.toString());
							}
						}
					}
					orderVO.setComplianceUrl(order.getComplianceUrl());
					// 新增：赋值bookingTime
					if (order.getBookingTime() != null) {
						orderVO.setBookingTime(order.getBookingTime());
					}
					// 新增：赋值batchNo
					Long oid = order.getId();
					if (oid == null && order.getCustomSku() != null) {
						List<TemuOrderDO> bySku = orderMapper.selectByCustomSku(order.getCustomSku());
						if (bySku != null && !bySku.isEmpty()) {
							Long foundId = bySku.get(0).getId();
							oid = foundId;
						}
					}
					if (orderIdToBatchNo != null && oid != null && orderIdToBatchNo.containsKey(oid)) {
						orderVO.setBatchNo(orderIdToBatchNo.get(oid));
					}
					orderListVOs.add(orderVO);
				}
				orderNoVO.setOrderList(orderListVOs);
				
				// 只有当orderListVOs不为空时才添加到orderNoList
				if (!orderListVOs.isEmpty()) {
					orderNoList.add(orderNoVO);
				}
			}
			
			// 只有当orderNoList不为空时才添加到voList
			if (!orderNoList.isEmpty()) {
				vo.setOrderNoList(orderNoList);
				voList.add(vo);
			}
		}
		return voList;
	}

	/**
	 * 获取物流单号在每日的序号映射
	 * 1. 为每个物流单号在每天分配唯一递增的每日序号
	 * 2. 序号分配规则：
	 *    - 已有物流记录：使用数据库当天已有的序号
	 *    - 新记录：在当日最大序号基础上自增后再分配
	 * @param saveRequestVOs 待保存的发货信息列表
	 * @return 双层映射结构：Map<物流单号, Map<日期, 该日序号>>
	 */
	private Map<String, Map<LocalDate, Integer>> getTrackingNumberSequences(
			List<TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO> saveRequestVOs) {
		// ============ 1. 提取关键数据 ============
		// 获取所有待处理物流单号（去重）
		Set<String> trackingNumbers = saveRequestVOs.stream()
				.map(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO::getTrackingNumber)
				.collect(Collectors.toSet());

		// 获取所有待保存物流记录关联的日期（基于发货时间）
		Set<LocalDate> saveDates = saveRequestVOs.stream()
				.map(vo -> {
					if (vo.getShippingTime() != null) {
						// 解析请求中的发货时间为日期
						return LocalDateTime.parse(vo.getShippingTime(),
								DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toLocalDate();
					}
					// 无发货时间默认为当日
					return LocalDate.now();
				})
				.collect(Collectors.toSet());

		// ============ 2. 查询数据库已有记录 ============
		// 查询条件：物流单号匹配 OR 日期匹配（两者条件满足其一）
		List<TemuOrderShippingInfoDO> existingShippings = shippingInfoMapper.selectList(
				new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
						.and(w -> w.in(TemuOrderShippingInfoDO::getTrackingNumber, trackingNumbers)
								.or()
								.apply(saveDates.stream()
										.map(date -> String.format("DATE(create_time) = '%s'", date))
										.collect(Collectors.joining(" OR ", "(", ")")
										))
						));

		// ============ 3. 构建初始映射结构 ============
		// 映射1：记录每日的最大序号（用于新序号分配）
		Map<LocalDate, AtomicInteger> dateToMaxSequence = new HashMap<>();

		// 映射2：核心返回值-记录物流单号在每个日期的序号
		Map<String, Map<LocalDate, Integer>> trackingNumberToSequence = new HashMap<>();

		// ============ 4. 处理数据库已有记录 ============
		if (!existingShippings.isEmpty()) {
			for (TemuOrderShippingInfoDO existing : existingShippings) {
				if (existing.getCreateTime() != null && existing.getDailySequence() != null) {
					LocalDate createDate = existing.getCreateTime().toLocalDate();
					// 更新该日期最大序号（比较当前记录序号与历史最大值）
					dateToMaxSequence.computeIfAbsent(createDate, k -> new AtomicInteger(0))
							.updateAndGet(current -> Math.max(current, existing.getDailySequence()));
					// 记录该物流单号在特定日期的序号（保留数据库现有值）
					trackingNumberToSequence
							.computeIfAbsent(existing.getTrackingNumber(), k -> new HashMap<>())
							.putIfAbsent(createDate, existing.getDailySequence());
				}
			}
		}

		// ============ 5. 为新的物流单号分配序号 ============
		for (TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO vo : saveRequestVOs) {
			// 确定当前记录的日期（优先使用请求中的发货日期）
			LocalDate createDate = vo.getShippingTime() != null ?
					LocalDateTime.parse(vo.getShippingTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toLocalDate() :
					LocalDate.now();
			// 获取当前物流单号的日期-序号映射
			Map<LocalDate, Integer> dateSequenceMap = trackingNumberToSequence
					.computeIfAbsent(vo.getTrackingNumber(), k -> new HashMap<>());
			// 当日首次出现该物流单号：分配新序号
			if (!dateSequenceMap.containsKey(createDate)) {
				// 获取或初始化该日期的序号计数器
				AtomicInteger maxSequence = dateToMaxSequence.computeIfAbsent(createDate, k -> new AtomicInteger(0));
				// 分配新序号（当日序号+1）
				dateSequenceMap.put(createDate, maxSequence.incrementAndGet());
			}
		}

		return trackingNumberToSequence;
	}
	
	@Override
	public TemuOrderShippingCountRespVO getUrgentOrderCount(TemuOrderShippingCountReqVO reqVO) {
		log.info("[getUrgentOrderCount] 开始查询加急未发货订单总数, 参数: {}", reqVO);
		
		// 构建查询条件：加急 + 未发货 + 时间范围
		LambdaQueryWrapperX<TemuOrderShippingInfoDO> queryWrapper = new LambdaQueryWrapperX<>();
		queryWrapper.eq(TemuOrderShippingInfoDO::getIsUrgent, true)  // 加急
				.eq(TemuOrderShippingInfoDO::getShippingStatus, 0);   // 未发货
		
		// 处理时间范围条件
		if (reqVO.getCreateTimeStart() != null && reqVO.getCreateTimeEnd() != null) {
			// 如果前端传了时间参数，使用前端的时间范围
			LocalDateTime startTime = reqVO.getCreateTimeStart().atStartOfDay();
			LocalDateTime endTime = reqVO.getCreateTimeEnd().atTime(23, 59, 59);
			queryWrapper.ge(TemuOrderShippingInfoDO::getCreateTime, startTime)
					.le(TemuOrderShippingInfoDO::getCreateTime, endTime);
			log.info("[getUrgentOrderCount] 使用前端指定的时间范围: {} 到 {}", startTime, endTime);
		} else {
			// 如果前端没有传时间参数，默认查询当天
			LocalDate today = LocalDate.now();
			LocalDateTime startTime = today.atStartOfDay();
			LocalDateTime endTime = today.atTime(23, 59, 59);
			queryWrapper.ge(TemuOrderShippingInfoDO::getCreateTime, startTime)
					.le(TemuOrderShippingInfoDO::getCreateTime, endTime);
			log.info("[getUrgentOrderCount] 使用默认当天时间范围: {} 到 {}", startTime, endTime);
		}
		
		// 查询总数
		Long total = shippingInfoMapper.selectCount(queryWrapper);
		log.info("[getUrgentOrderCount] 查询完成, 总数: {}", total);
		
		TemuOrderShippingCountRespVO respVO = new TemuOrderShippingCountRespVO();
		respVO.setTotal(total);
		return respVO;
	}

	/**
	 * 批量保存物流面单信息
	 * @param saveRequestVOs
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	public int batchSaveOrderShipping(List<TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO> saveRequestVOs) {
		if (CollectionUtils.isEmpty(saveRequestVOs)) {
			return 0;
		}
				
		// ================== 1. 修正shopId ==================
		// 检查是否启用shopId修正功能
		String isShopIdCorrectionEnabled = configApi.getConfigValueByKey("temu.order-shipping.shopId-correction.enabled");
		boolean shopIdCorrectionEnabled = false;
		if (StringUtils.hasText(isShopIdCorrectionEnabled)) {
			try {
				shopIdCorrectionEnabled = Boolean.parseBoolean(isShopIdCorrectionEnabled);
			} catch (Exception e) {
				log.warn("[batchSaveOrderShipping] 解析shopId修正开关配置失败", e);
			}
		}
		
		if (shopIdCorrectionEnabled) {
			log.info("[batchSaveOrderShipping] shopId修正功能已启用，开始修正shopId");
			// 收集所有订单号
			Set<String> orderNos = saveRequestVOs.stream()
					.map(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO::getOrderNo)
					.collect(Collectors.toSet());
			
			// 批量查询订单信息
			List<TemuOrderDO> orders = orderMapper.selectList(
					new LambdaQueryWrapperX<TemuOrderDO>()
							.select(TemuOrderDO::getOrderNo, TemuOrderDO::getShopId)
							.in(TemuOrderDO::getOrderNo, orderNos));
			
			// 构建订单号到shopId的映射
			Map<String, Long> orderNoToShopId = orders.stream()
					.collect(Collectors.toMap(TemuOrderDO::getOrderNo, TemuOrderDO::getShopId));
			
			// 更新saveRequestVOs中的shopId
			for (TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO vo : saveRequestVOs) {
				Long correctShopId = orderNoToShopId.get(vo.getOrderNo());
				if (correctShopId != null) {
					// 记录修改日志
					if (!Objects.equals(vo.getShopId(), correctShopId)) {
						log.info("[batchSaveOrderShipping] 修正shopId, orderNo={}, oldShopId={}, newShopId={}",
								vo.getOrderNo(), vo.getShopId(), correctShopId);
					}
					vo.setShopId(correctShopId);
				} else {
					log.warn("[batchSaveOrderShipping] 未找到订单对应的shopId, orderNo={}", vo.getOrderNo());
				}
			}
		} else {
			log.info("[batchSaveOrderShipping] shopId修正功能未启用，跳过修正步骤");
		}

		// 1. 参数校验（强制要求 trackingNumber 非空）
		for (TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO : saveRequestVOs) {
			if (saveRequestVO == null) {
				throw new IllegalArgumentException("待发货订单信息不能为空");
			}
			if (saveRequestVO.getOrderNo() == null) {
				throw new IllegalArgumentException("订单编号不能为空");
			}
			if (saveRequestVO.getShopId() == null) {
				throw new IllegalArgumentException("店铺ID不能为空");
			}
			if (saveRequestVO.getTrackingNumber() == null) {
				throw new IllegalArgumentException("物流单号不能为空");
			}

			// 检查是否为加急订单，如果是则发送企业微信通知
			if (Boolean.TRUE.equals(saveRequestVO.getIsUrgent())) {
				// 查询数据库中该物流单号的is_urgent状态
				Long urgentCount = shippingInfoMapper.selectCount(
						new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
								.eq(TemuOrderShippingInfoDO::getTrackingNumber, saveRequestVO.getTrackingNumber())
								.eq(TemuOrderShippingInfoDO::getIsUrgent, 1)
				);
				// 如果数据库中已经有加急记录，则不再发送告警
				if (urgentCount == 0) {
					// 获取shopId为88888888的店铺webhook地址
					TemuShopDO shop = temuShopMapper.selectByShopId(77777777L);
					if (shop != null && shop.getWebhook() != null) {
						String shopName = temuShopMapper.selectByShopId(saveRequestVO.getShopId()) != null ?
								temuShopMapper.selectByShopId(saveRequestVO.getShopId()).getShopName() : "未知店铺";

						String message = String.format("⚠️ 加急订单提醒！\n订单号：%s\n店铺：%s\n物流单号：%s\n请及时处理！",
								saveRequestVO.getOrderNo(), shopName, saveRequestVO.getTrackingNumber());

						try {
							weiXinProducer.sendMessage(shop.getWebhook(), message);
							log.info("[batchSaveOrderShipping][发送加急订单通知成功，订单号：{}]", saveRequestVO.getOrderNo());
						} catch (Exception e) {
							log.error("[batchSaveOrderShipping][发送加急订单通知失败，订单号：{}]", saveRequestVO.getOrderNo(), e);
						}
					}
				} else {
					log.info("[batchSaveOrderShipping][该物流单号已存在加急记录，不再发送加急告警，trackingNumber={}]", saveRequestVO.getTrackingNumber());
				}
			}
		}

		// ================== 2. 生成物流序号映射 ==================
		// Map<物流单号, Map<日期, 当天的序号>>
		// 用于给相同物流单号在同一天创建的记录生成自增序号
		Map<String, Map<LocalDate, Integer>> trackingNumberToSequence = getTrackingNumberSequences(saveRequestVOs);

		// 优化：在删除前提取已发货记录的shippedOperatorId，后续新数据设置
		Map<String, Long> shippedOperatorMap = new HashMap<>(); // key: orderNo + "_" + shopId, value: shippedOperatorId

		// ================== 3. 清理历史记录 ==================
		// 先查询所有匹配的历史记录，提取已发货的shippedOperatorId
		// （1） 首先查询出表中(orderNo+shopId)匹配的所有记录
		// （2） 提前保存 shippingStatus = 1 的记录的 shippedOperatorId	（已发货，发货人）
		//  (3) 最后删除所有匹配的历史记录（包括已发货状态）
		// （4） 在第4步，准备新数据，通过(orderNo+shopId)判断是否需要设置（已发货，发货人）
		//  以上步骤为确保每次同步物流面单的信息都是最新，也处理了已发货的物流（又修改了物流单号）的特殊情况
		for (TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO : saveRequestVOs) {
			List<TemuOrderShippingInfoDO> oldList = shippingInfoMapper.selectList(
					new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
							.eq(TemuOrderShippingInfoDO::getOrderNo, saveRequestVO.getOrderNo())
							.eq(TemuOrderShippingInfoDO::getShopId, saveRequestVO.getShopId())
			);
			if (oldList != null && !oldList.isEmpty()) {
				for (TemuOrderShippingInfoDO old : oldList) {
					if (old.getShippingStatus() != null && old.getShippingStatus() == 1) {
						String key = old.getOrderNo() + "_" + old.getShopId() + "_" + old.getTrackingNumber();;
						shippedOperatorMap.put(key, old.getShippedOperatorId());
					}
				}
			}
			// 删除所有匹配的历史记录（包括已发货状态）
			shippingInfoMapper.delete(
					new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
							.eq(TemuOrderShippingInfoDO::getOrderNo, saveRequestVO.getOrderNo())
							.eq(TemuOrderShippingInfoDO::getShopId, saveRequestVO.getShopId())
			);
		}

		// ================== 4. 准备新数据 ==================
		// 检查是否还有需要保存的记录
		if (!saveRequestVOs.isEmpty()) {
			// 转换VO为DO对象，sorting_sequence 先设为0
			LocalDateTime now = LocalDateTime.now();
			List<TemuOrderShippingInfoDO> toSaveList = saveRequestVOs.stream()
					.map(vo -> {
						// 创建实体对象
						TemuOrderShippingInfoDO info = new TemuOrderShippingInfoDO();
						// 设置基础字段
						info.setOrderNo(vo.getOrderNo());
						info.setTrackingNumber(vo.getTrackingNumber());
						info.setExpressImageUrl(vo.getExpressImageUrl());
						info.setExpressOutsideImageUrl(vo.getExpressOutsideImageUrl());
						info.setExpressSkuImageUrl(vo.getExpressSkuImageUrl());
						info.setShopId(vo.getShopId());
						info.setIsUrgent(vo.getIsUrgent()); // 设置是否加急

						// 判断是否需要设置为已发货
						String key = vo.getOrderNo() + "_" + vo.getShopId() + "_" + vo.getTrackingNumber();
						if (shippedOperatorMap.containsKey(key)) {
							info.setShippingStatus(1); // 已发货
							info.setShippedOperatorId(shippedOperatorMap.get(key));
						} else {
							info.setShippingStatus(0); // 新保存的记录默认为未发货状态
						}

						// 确定物流信息的创建时间： 优先使用VO中的发货时间 / 无发货时间则使用当前系统时间
						LocalDateTime createTime;
						if (vo.getShippingTime() != null) {
							createTime = LocalDateTime.parse(vo.getShippingTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
						} else {
							createTime = now;
						}
						info.setCreateTime(createTime);
						info.setUpdateTime(now);

						// ===== 设置物流单号的每日序号 =====
						// 从映射中获取该物流单号对应的日期序号表
						Map<LocalDate, Integer> dateSequenceMap = trackingNumberToSequence.get(vo.getTrackingNumber());
						if (dateSequenceMap != null) {
							// 设置当前物流单号的序号\tkey=日期，value=序号
							info.setDailySequence(dateSequenceMap.get(createTime.toLocalDate()));
						}

						return info;
					})
					.collect(Collectors.toList());

			// ================== 5. 批量保存 ==================
			try {
				int affectedRows = shippingInfoMapper.insertBatch(toSaveList);
				log.info("批量保存成功，数量：{}", affectedRows);

				// 从配置中获取是否开启物流信息校验功能
				String isValidate = configApi.getConfigValueByKey("temu.is_validate");
				log.info("是否开启物流信息校验功能: {}", isValidate);
				boolean flag = false; // 默认值
				if (StrUtil.isNotEmpty(isValidate)) {
					try {
						flag = Boolean.parseBoolean(isValidate);
					} catch (Exception e) {
						log.warn("是否开启物流信息校验功能的配置格式错误，使用默认值");
					}
				}
				if(flag) {
					// 收集所有的物流单号并按shopId分组（只收集最近5天内的记录）
					Map<String, Set<String>> shopTrackingNumbers = new HashMap<>();
					String fiveDaysAgo = LocalDateTime.now().minusDays(5)
							.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

					for (TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO vo : saveRequestVOs) {
						if (vo.getTrackingNumber() != null && StringUtils.hasText(vo.getTrackingNumber())) {
							// 如果发货时间为空或者在5天内，则添加到校验列表
							if (vo.getShippingTime() == null || vo.getShippingTime().compareTo(fiveDaysAgo) > 0) {
								String shopId = String.valueOf(vo.getShopId());
								shopTrackingNumbers.computeIfAbsent(shopId, k -> new HashSet<>())
										.add(vo.getTrackingNumber());
							}
						}
					}
					// 物流单号不为空时发布校验事件
					if (!shopTrackingNumbers.isEmpty()) {
						// 发布物流单号校验事件（核心事件驱动机制）
						// 注意：此处发布的事件是同步触发的，但实际处理可能异步执行
						eventPublisher.publishEvent(new TrackingNumberValidationEvent(shopTrackingNumbers));
						log.info("[batchSaveOrderShipping] 已发布物流单号校验事件，shopTrackingNumbers={}", shopTrackingNumbers);
					}
				}
				return affectedRows;
			} catch (Exception e) {
				log.error("批量保存失败", e);
				throw new ServiceException("批量保存失败：" + e.getMessage());
			}
		}
		return 0;
	}

	/**
	 * 批量保存物流面单信息V2
	 * @param saveRequestVOs
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	public int batchSaveOrderShippingV2(List<TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO> saveRequestVOs) {
		if (CollectionUtils.isEmpty(saveRequestVOs)) {
			return 0;
		}
				
		// ================== 1. 修正shopId ==================
		// 检查是否启用shopId修正功能
		String isShopIdCorrectionEnabled = configApi.getConfigValueByKey("temu.order-shipping.shopId-correction.enabled");
		boolean shopIdCorrectionEnabled = false;
		if (StringUtils.hasText(isShopIdCorrectionEnabled)) {
			try {
				shopIdCorrectionEnabled = Boolean.parseBoolean(isShopIdCorrectionEnabled);
			} catch (Exception e) {
				log.warn("[batchSaveOrderShipping] 解析shopId修正开关配置失败", e);
			}
		}
		
		if (shopIdCorrectionEnabled) {
			log.info("[batchSaveOrderShipping] shopId修正功能已启用，开始修正shopId");
			// 收集所有订单号
			Set<String> orderNos = saveRequestVOs.stream()
					.map(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO::getOrderNo)
					.collect(Collectors.toSet());
			
			// 批量查询订单信息
			List<TemuOrderDO> orders = orderMapper.selectList(
					new LambdaQueryWrapperX<TemuOrderDO>()
							.select(TemuOrderDO::getOrderNo, TemuOrderDO::getShopId)
							.in(TemuOrderDO::getOrderNo, orderNos));
			
			// 构建订单号到shopId的映射
			Map<String, Long> orderNoToShopId = orders.stream()
					.collect(Collectors.toMap(TemuOrderDO::getOrderNo, TemuOrderDO::getShopId));
			
			// 更新saveRequestVOs中的shopId
			for (TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO vo : saveRequestVOs) {
				Long correctShopId = orderNoToShopId.get(vo.getOrderNo());
				if (correctShopId != null) {
					// 记录修改日志
					if (!Objects.equals(vo.getShopId(), correctShopId)) {
						log.info("[batchSaveOrderShipping] 修正shopId, orderNo={}, oldShopId={}, newShopId={}",
								vo.getOrderNo(), vo.getShopId(), correctShopId);
					}
					vo.setShopId(correctShopId);
				} else {
					log.warn("[batchSaveOrderShipping] 未找到订单对应的shopId, orderNo={}", vo.getOrderNo());
				}
			}
		} else {
			log.info("[batchSaveOrderShipping] shopId修正功能未启用，跳过修正步骤");
		}

		// 1. 参数校验（强制要求 trackingNumber 非空）
		for (TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO : saveRequestVOs) {
			if (saveRequestVO == null) {
				throw new IllegalArgumentException("待发货订单信息不能为空");
			}
			if (saveRequestVO.getOrderNo() == null) {
				throw new IllegalArgumentException("订单编号不能为空");
			}
			if (saveRequestVO.getShopId() == null) {
				throw new IllegalArgumentException("店铺ID不能为空");
			}
			if (saveRequestVO.getTrackingNumber() == null) {
				throw new IllegalArgumentException("物流单号不能为空");
			}

			// 检查是否为加急订单，如果是则发送企业微信通知
			if (Boolean.TRUE.equals(saveRequestVO.getIsUrgent())) {
				// 查询数据库中该物流单号的is_urgent状态
				Long urgentCount = shippingInfoMapper.selectCount(
						new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
								.eq(TemuOrderShippingInfoDO::getTrackingNumber, saveRequestVO.getTrackingNumber())
								.eq(TemuOrderShippingInfoDO::getIsUrgent, 1)
				);
				// 如果数据库中已经有加急记录，则不再发送告警
				if (urgentCount == 0) {
					// 获取shopId为88888888的店铺webhook地址
					TemuShopDO shop = temuShopMapper.selectByShopId(77777777L);
					if (shop != null && shop.getWebhook() != null) {
						String shopName = temuShopMapper.selectByShopId(saveRequestVO.getShopId()) != null ?
								temuShopMapper.selectByShopId(saveRequestVO.getShopId()).getShopName() : "未知店铺";

						String message = String.format("⚠️ 加急订单提醒！\n订单号：%s\n店铺：%s\n物流单号：%s\n请及时处理！",
								saveRequestVO.getOrderNo(), shopName, saveRequestVO.getTrackingNumber());

						try {
							weiXinProducer.sendMessage(shop.getWebhook(), message);
							log.info("[batchSaveOrderShipping][发送加急订单通知成功，订单号：{}]", saveRequestVO.getOrderNo());
						} catch (Exception e) {
							log.error("[batchSaveOrderShipping][发送加急订单通知失败，订单号：{}]", saveRequestVO.getOrderNo(), e);
						}
					}
				} else {
					log.info("[batchSaveOrderShipping][该物流单号已存在加急记录，不再发送加急告警，trackingNumber={}]", saveRequestVO.getTrackingNumber());
				}
			}
		}

		// ================== 2. 生成物流序号映射 ==================
		// Map<物流单号, Map<日期, 当天的序号>>
		// 用于给相同物流单号在同一天创建的记录生成自增序号
		Map<String, Map<LocalDate, Integer>> trackingNumberToSequence = getTrackingNumberSequences(saveRequestVOs);

		// ================== 3. 清理历史记录 ==================
		// 先查询并保存所有相关记录的信息
		Map<String, Long> shippedOperatorMap = new HashMap<>(); // key: orderNo + "_" + shopId + "_" + trackingNumber, value: shippedOperatorId
		Map<String, String> originalExpressImageUrlMap = new HashMap<>(); // key: orderNo + "_" + shopId + "_" + trackingNumber, value: expressImageUrl
		Map<String, String> originalExpressOutsideImageUrlMap = new HashMap<>(); // key: orderNo + "_" + shopId + "_" + trackingNumber, value: expressOutsideImageUrl

		int totalDeleteCount = 0; // 总删除记录数
		for (TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO : saveRequestVOs) {
			// 先按orderNo和shopId查询旧记录
			List<TemuOrderShippingInfoDO> oldList = shippingInfoMapper.selectList(
					new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
							.eq(TemuOrderShippingInfoDO::getOrderNo, saveRequestVO.getOrderNo())
							.eq(TemuOrderShippingInfoDO::getShopId, saveRequestVO.getShopId())
			);
			if (oldList != null && !oldList.isEmpty()) {
				for (TemuOrderShippingInfoDO old : oldList) {
					if (old.getShippingStatus() != null && old.getShippingStatus() == 1) {
						String key = old.getOrderNo() + "_" + old.getShopId() + "_" + old.getTrackingNumber();
						shippedOperatorMap.put(key, old.getShippedOperatorId());
					}
					// 收集原有的加急单和面单信息
					String imageKey = old.getOrderNo() + "_" + old.getShopId() + "_" + old.getTrackingNumber();
					if (old.getExpressImageUrl() != null) {
						originalExpressImageUrlMap.put(imageKey, old.getExpressImageUrl());
					}
					if (old.getExpressOutsideImageUrl() != null) {
						originalExpressOutsideImageUrlMap.put(imageKey, old.getExpressOutsideImageUrl());
					}
				}
			}
			// 物理删除所有匹配的历史记录
			int deleteCount = shippingInfoMapper.physicalDeleteByOrderNoAndShopId(saveRequestVO.getOrderNo(), saveRequestVO.getShopId());
			totalDeleteCount += deleteCount;
		}
		log.info("[batchSaveOrderShippingV2][物理删除物流信息] 总删除记录数：{}", totalDeleteCount);

		// ================== 4. 准备新数据 ==================
		// 检查是否还有需要保存的记录
		if (!saveRequestVOs.isEmpty()) {
			// 转换VO为DO对象，sorting_sequence 先设为0
			LocalDateTime now = LocalDateTime.now();
			List<TemuOrderShippingInfoDO> toSaveList = saveRequestVOs.stream()
					.map(vo -> {
						// 创建实体对象
						TemuOrderShippingInfoDO info = new TemuOrderShippingInfoDO();
						// 设置基础字段
						info.setOrderNo(vo.getOrderNo());
						info.setTrackingNumber(vo.getTrackingNumber());

						// 处理图片URL：如果新数据中没有，则使用原有的
						String imageKey = vo.getOrderNo() + "_" + vo.getShopId() + "_" + vo.getTrackingNumber();
						if (vo.getExpressImageUrl() != null) {
							info.setExpressImageUrl(vo.getExpressImageUrl());
						} else {
							info.setExpressImageUrl(originalExpressImageUrlMap.get(imageKey));
						}
						if (vo.getExpressOutsideImageUrl() != null) {
							info.setExpressOutsideImageUrl(vo.getExpressOutsideImageUrl());
						} else {
							info.setExpressOutsideImageUrl(originalExpressOutsideImageUrlMap.get(imageKey));
						}

						info.setExpressSkuImageUrl(vo.getExpressSkuImageUrl());
						info.setShopId(vo.getShopId());
						info.setIsUrgent(vo.getIsUrgent()); // 设置是否加急

						// 判断是否需要设置为已发货
						String key = vo.getOrderNo() + "_" + vo.getShopId() + "_" + vo.getTrackingNumber();
						if (shippedOperatorMap.containsKey(key)) {
							info.setShippingStatus(1); // 已发货
							info.setShippedOperatorId(shippedOperatorMap.get(key));
						} else {
							info.setShippingStatus(0); // 新保存的记录默认为未发货状态
						}

						// 确定物流信息的创建时间： 优先使用VO中的发货时间 / 无发货时间则使用当前系统时间
						LocalDateTime createTime;
						if (vo.getShippingTime() != null) {
							createTime = LocalDateTime.parse(vo.getShippingTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
						} else {
							createTime = now;
						}
						info.setCreateTime(createTime);
						info.setUpdateTime(now);

						// ===== 设置物流单号的每日序号 =====
						// 从映射中获取该物流单号对应的日期序号表
						Map<LocalDate, Integer> dateSequenceMap = trackingNumberToSequence.get(vo.getTrackingNumber());
						if (dateSequenceMap != null) {
							// 设置当前物流单号的序号\tkey=日期，value=序号
							info.setDailySequence(dateSequenceMap.get(createTime.toLocalDate()));
						}

						return info;
					})
					.collect(Collectors.toList());

			// ================== 5. 批量保存 ==================
			try {
				int affectedRows = shippingInfoMapper.insertBatch(toSaveList);
				log.info("批量保存成功，数量：{}", affectedRows);

				// 从配置中获取是否开启物流信息校验功能
				String isValidate = configApi.getConfigValueByKey("temu.is_validate");
				log.info("是否开启物流信息校验功能: {}", isValidate);
				boolean flag = false; // 默认值
				if (StrUtil.isNotEmpty(isValidate)) {
					try {
						flag = Boolean.parseBoolean(isValidate);
					} catch (Exception e) {
						log.warn("是否开启物流信息校验功能的配置格式错误，使用默认值");
					}
				}
				if(flag) {
					// 收集所有的物流单号并按shopId分组（只收集最近5天内的记录）
					Map<String, Set<String>> shopTrackingNumbers = new HashMap<>();
					String fiveDaysAgo = LocalDateTime.now().minusDays(5)
							.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
					
					for (TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO vo : saveRequestVOs) {
						if (vo.getTrackingNumber() != null && StringUtils.hasText(vo.getTrackingNumber())) {
							// 如果发货时间为空或者在5天内，则添加到校验列表
							if (vo.getShippingTime() == null || vo.getShippingTime().compareTo(fiveDaysAgo) > 0) {
								String shopId = String.valueOf(vo.getShopId());
								shopTrackingNumbers.computeIfAbsent(shopId, k -> new HashSet<>())
										.add(vo.getTrackingNumber());
							}
						}
					}
					// 物流单号不为空时发布校验事件
					if (!shopTrackingNumbers.isEmpty()) {
						// 发布物流单号校验事件（核心事件驱动机制）
						// 注意：此处发布的事件是同步触发的，但实际处理可能异步执行
						eventPublisher.publishEvent(new TrackingNumberValidationEvent(shopTrackingNumbers));
						log.info("[batchSaveOrderShipping] 已发布物流单号校验事件，shopTrackingNumbers={}", shopTrackingNumbers);
					}
				}
				return affectedRows;
			} catch (Exception e) {
				log.error("批量保存失败", e);
				throw new ServiceException("批量保存失败：" + e.getMessage());
			}
		}
		return 0;
	}
	
	/**
	 * 发送紧急物流告警
	 * 
	 * @param reqVO 告警请求信息
	 * @return 是否发送成功
	 */
	public Boolean sendUrgentAlert(TemuOrderUrgentAlertReqVO reqVO) {
		try {
			if (reqVO == null) {
				return true;
			}

			// 只处理小于24小时的订单
			if (!Boolean.TRUE.equals(reqVO.getIsLessThan24h())) {
				return true;
			}

			// 参数校验
			if (StrUtil.hasBlank(reqVO.getOrderNo(), reqVO.getTrackingNumber()) || reqVO.getShopId() == null) {
				return false;
			}

			// 1. 获取shopId为66666666L的店铺webhook地址
			TemuShopDO webhookShop = temuShopMapper.selectByShopId(66666666L);
			if (webhookShop == null || webhookShop.getWebhook() == null) {
				return false;
			}

			// 2. 获取订单所属店铺信息
			TemuShopDO orderShop = temuShopMapper.selectByShopId(reqVO.getShopId());
			String shopName = orderShop != null ? orderShop.getShopName() : "未知店铺";

			// 3. 构建告警消息
			StringBuilder messageBuilder = new StringBuilder();
			messageBuilder.append("⚠️ 紧急物流告警！\n\n");
			
			// 添加订单信息
			messageBuilder.append("店铺：").append(shopName).append("\n");
			messageBuilder.append("订单号：").append(reqVO.getOrderNo()).append("\n");
			messageBuilder.append("物流单号：").append(reqVO.getTrackingNumber()).append("\n");
			messageBuilder.append("\n⏰ 该订单剩余时间小于24小时，请尽快处理！");

			// 4. 发送企业微信告警
			weiXinProducer.sendMessage(webhookShop.getWebhook(), messageBuilder.toString());
			return true;

		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * 查询近三天的批次信息
	 * 
	 * @return 批次信息列表
	 */
	public List<TemuOrderBatchRespVO> getRecentBatches() {
		// 计算三天前的时间
		LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
		
		// 查询近三天的批次信息
		List<TemuOrderBatchDO> batchList = temuOrderBatchMapper.selectList(
			new LambdaQueryWrapperX<TemuOrderBatchDO>()
				.ge(TemuOrderBatchDO::getCreateTime, threeDaysAgo)
				.eq(TemuOrderBatchDO::getDeleted, false)
				.orderByDesc(TemuOrderBatchDO::getCreateTime)
		);

		if (CollUtil.isEmpty(batchList)) {
			return new ArrayList<>();
		}

		// 获取所有批次类目ID
		Set<String> batchCategoryIds = batchList.stream()
				.map(TemuOrderBatchDO::getBatchCategoryId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		// 查询批次类目信息
		Map<String, List<String>> batchCategoryIdToCategories = new HashMap<>();
		if (!batchCategoryIds.isEmpty()) {
			// 查询所有相关的批次类目记录
			List<TemuOrderBatchCategoryDO> categories = temuOrderBatchCategoryMapper.selectList(
				new LambdaQueryWrapperX<TemuOrderBatchCategoryDO>()
					.in(TemuOrderBatchCategoryDO::getBatchCategoryId, batchCategoryIds)
			);
			
			// 按 batchCategoryId 分组
			for (TemuOrderBatchCategoryDO category : categories) {
				String batchCategoryId = category.getBatchCategoryId();
				String categoryId = category.getCategoryId();
				if (batchCategoryId != null && categoryId != null) {
					batchCategoryIdToCategories.computeIfAbsent(
						batchCategoryId, 
						k -> new ArrayList<>()
					).add(categoryId);
				}
			}
		}

		// 转换为 VO 对象，并设置 categoryIds
		List<TemuOrderBatchRespVO> voList = new ArrayList<>(batchList.size());
		for (TemuOrderBatchDO batch : batchList) {
			TemuOrderBatchRespVO vo = BeanUtils.toBean(batch, TemuOrderBatchRespVO.class);
			// 设置 categoryIds
			if (batch.getBatchCategoryId() != null) {
				vo.setCategoryIds(batchCategoryIdToCategories.getOrDefault(
					batch.getBatchCategoryId(), new ArrayList<>()
				));
			} else {
				vo.setCategoryIds(new ArrayList<>());
			}
			voList.add(vo);
		}

		return voList;
	}

	/**
	 * 物流单号校验事件处理器 - 采用事务绑定的异步处理模式
	 * @param event 包含待校验物流单号的包装事件
	 */
	@Async("taskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleTrackingNumberValidation(TrackingNumberValidationEvent event) {
		try {
			// 从事件中提取物流单号集合
			Map<String, Set<String>> shopTrackingNumbers = event.getShopTrackingNumbers();
			log.info("[handleTrackingNumberValidation] 开始异步校验物流单号，shopTrackingNumbers={}", shopTrackingNumbers);
			
			// 调用temuApi验证物流单号与erp是否一致
			TemuOrderTrackingValidateRespVO validateResult = temuDeliveryOrderConvertService.validateTrackingNumber(shopTrackingNumbers);
			if (!validateResult.getSuccess()) {
				log.warn("[handleTrackingNumberValidation] 物流单号校验失败：{}", validateResult.getErrorMessage());
			} else {
				log.info("[handleTrackingNumberValidation] 物流单号校验成功");
			}
		} catch (Exception e) {
			log.error("[handleTrackingNumberValidation] 物流单号校验异常", e);
		}
	}

}