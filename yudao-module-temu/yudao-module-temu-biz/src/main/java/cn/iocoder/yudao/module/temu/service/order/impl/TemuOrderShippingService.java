package cn.iocoder.yudao.module.temu.service.order.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderListRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderNoListRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.*;
import cn.iocoder.yudao.module.temu.dal.mysql.*;
import cn.iocoder.yudao.module.temu.service.order.ITemuOrderShippingService;
import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;

import java.util.Objects;

/**
 * Temu订单物流 Service 实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TemuOrderShippingService implements ITemuOrderShippingService {
	
	private final TemuOrderShippingMapper shippingInfoMapper;
	private final TemuOrderMapper orderMapper;
	private final TemuShopMapper shopMapper;
	private final TemuProductCategoryMapper categoryMapper;
	private final TemuUserShopMapper userShopMapper;
	
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
		List<TemuOrderDO> matchedOrders = getMatchedOrders(pageVO.getOrderStatus(), pageVO.getOrderNo(), pageVO.getCustomSku());
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
		if (StringUtils.hasText(pageVO.getOrderNo()) || pageVO.getOrderStatus() != null) {
			allRelatedShippings = list;
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
								TemuOrderDO::getEffectiveImgUrl, TemuOrderDO::getComplianceUrl, TemuOrderDO::getOriginalQuantity)
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
		List<TemuOrderShippingRespVO> voList = buildOrderShippingRespList(allRelatedShippings, orderMap, shopMap,
				categoryMap);
		log.info("[getOrderShippingPage] [步骤6] 组装结果: 耗时={}ms, 结果数量={}",
				System.currentTimeMillis() - step6StartTime, voList.size());
		
		// ==================== 查询完成，输出统计信息 ====================
		log.info("[getOrderShippingPage] ==================== 查询完成 ====================");
		log.info("[getOrderShippingPage] 统计信息: 总耗时={}ms, 总记录数={}, 当前页记录数={}, 页码={}, 每页大小={}",
				System.currentTimeMillis() - totalStartTime, total, voList.size(),
				pageVO.getPageNo(), pageVO.getPageSize());
		
		return new PageResult<>(voList, total, pageVO.getPageNo(), pageVO.getPageSize());
	}
	
	// 批量保存待发货订单
	
	/**
	 * 物流单号可以重复
	 * 已发货状态的记录都会被保留
	 * 只有当物流单号、shopId和orderNo都匹配时，才会从 saveRequestVOs 中移除对应记录（避免重复保存）
	 * 未发货状态的记录会被删除并重新保存
	 * 新保存的记录默认为未发货状态
	 *
	 * @param saveRequestVOs
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public int batchSaveOrderShipping(List<TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO> saveRequestVOs) {
		if (CollectionUtils.isEmpty(saveRequestVOs)) {
			return 0;
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
		}
		
		// 2. 提取所有 trackingNumber
		Set<String> trackingNumbers = saveRequestVOs.stream()
				.map(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO::getTrackingNumber)
				.collect(Collectors.toSet());
		
		// 3. 查询已存在的记录
		List<TemuOrderShippingInfoDO> existingShippings = shippingInfoMapper.selectList(
				new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
						.in(TemuOrderShippingInfoDO::getTrackingNumber, trackingNumbers));
		
		// 4. 处理已存在的记录
		if (!existingShippings.isEmpty()) {
			// 收集需要删除的记录ID
			List<Long> toDeleteIds = new ArrayList<>();
			// 创建一个Set来存储已发货且匹配的记录的唯一标识（物流单号+shopId+orderNo）
			Set<String> matchedShippedRecords = new HashSet<>();
			
			for (TemuOrderShippingInfoDO existing : existingShippings) {
				if (existing.getShippingStatus() != null && existing.getShippingStatus() == 1) {
					// 已发货状态的记录都保留（不删除）
					// 检查是否有完全匹配的新记录
					for (TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO newRecord : saveRequestVOs) {
						if (existing.getTrackingNumber().equals(newRecord.getTrackingNumber())
								&& existing.getShopId().equals(newRecord.getShopId())
								&& existing.getOrderNo().equals(newRecord.getOrderNo())) {
							// 如果找到完全匹配的记录，将其标记为已匹配
							matchedShippedRecords.add(generateMatchKey(newRecord));
						}
					}
				} else {
					// 未发货状态的记录需要删除
					toDeleteIds.add(existing.getId());
				}
			}
			
			// 删除未发货状态的记录
			if (!toDeleteIds.isEmpty()) {
				shippingInfoMapper.deleteBatchIds(toDeleteIds);
			}
			
			// 过滤掉已发货且完全匹配的记录
			if (!matchedShippedRecords.isEmpty()) {
				saveRequestVOs = saveRequestVOs.stream()
						.filter(vo -> !matchedShippedRecords.contains(generateMatchKey(vo)))
						.collect(Collectors.toList());
			}
		}
		
		// 5. 如果还有需要保存的记录
		if (!saveRequestVOs.isEmpty()) {
			// 转换所有请求为 DO
			LocalDateTime now = LocalDateTime.now();
			List<TemuOrderShippingInfoDO> toSaveList = saveRequestVOs.stream()
					.map(vo -> {
						TemuOrderShippingInfoDO info = new TemuOrderShippingInfoDO();
						info.setOrderNo(vo.getOrderNo());
						info.setTrackingNumber(vo.getTrackingNumber());
						info.setExpressImageUrl(vo.getExpressImageUrl());
						info.setExpressOutsideImageUrl(vo.getExpressOutsideImageUrl());
						info.setExpressSkuImageUrl(vo.getExpressSkuImageUrl());
						info.setShopId(vo.getShopId());
						info.setShippingStatus(0); // 新保存的记录默认为未发货状态
						if (vo.getShippingTime() != null) {
							info.setCreateTime(LocalDateTime.parse(vo.getShippingTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
						} else {
							info.setCreateTime(now);
						}
						info.setUpdateTime(now);
						return info;
					})
					.collect(Collectors.toList());
			
			// 6. 批量保存
			try {
				int affectedRows = shippingInfoMapper.insertBatch(toSaveList);
				log.info("批量保存成功，数量：{}", affectedRows);
				return affectedRows;
			} catch (Exception e) {
				log.error("批量保存失败", e);
				throw new ServiceException("批量保存失败：" + e.getMessage());
			}
		}
		return 0;
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
				
				// 更新物流订单发货状态
				TemuOrderShippingInfoDO updateShipping = new TemuOrderShippingInfoDO();
				updateShipping.setShippingStatus(1); // 设置为已发货
				updateShipping.setUpdateTime(LocalDateTime.now());
				
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
					
					log.info("[batchUpdateOrderStatus] 物流订单状态更新完成, 更新数量: {}", shippingUpdateCount);
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
	private List<TemuOrderDO> getMatchedOrders(Integer orderStatus, String orderNo, String customSku) {
		if (orderStatus == null && !StringUtils.hasText(orderNo)&& !StringUtils.hasText(customSku)) {
			return null;
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
		
		return orderMapper.selectList(orderWrapper);
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
		
		// 处理物流单号
		if (StringUtils.hasText(pageVO.getTrackingNumber())) {
			subQuery.append(" AND tracking_number LIKE '%").append(pageVO.getTrackingNumber()).append("%'");
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
	                                                                 Map<String, TemuProductCategoryDO> categoryMap) {
		// 按物流单号分组
		Map<String, List<TemuOrderShippingInfoDO>> shippingGroupMap = shippingList.stream()
				.collect(Collectors.groupingBy(TemuOrderShippingInfoDO::getTrackingNumber));
		
		List<TemuOrderShippingRespVO> voList = new ArrayList<>();
		
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
	
}