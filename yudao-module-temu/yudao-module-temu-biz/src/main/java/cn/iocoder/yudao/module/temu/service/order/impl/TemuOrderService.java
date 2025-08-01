package cn.iocoder.yudao.module.temu.service.order.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.ServerException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.pay.dal.dataobject.wallet.PayWalletDO;
import cn.iocoder.yudao.module.pay.enums.wallet.PayWalletBizTypeEnum;
import cn.iocoder.yudao.module.pay.service.wallet.PayWalletService;
import cn.iocoder.yudao.module.system.dal.dataobject.dict.DictTypeDO;
import cn.iocoder.yudao.module.system.dal.mysql.dict.DictTypeMapper;
import cn.iocoder.yudao.module.temu.api.category.IPriceRule;
import cn.iocoder.yudao.module.temu.api.category.factory.PriceRuleFactory;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.*;
import cn.iocoder.yudao.module.temu.dal.dataobject.*;
import cn.iocoder.yudao.module.temu.dal.mysql.*;
import cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.temu.enums.TemuOrderStatusEnum;
import cn.iocoder.yudao.module.temu.service.order.ITemuOrderService;
import cn.iocoder.yudao.module.temu.service.orderBatch.impl.TemuOrderBatchCategoryService;
import cn.iocoder.yudao.module.temu.service.oss.TemuOssService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuUserShopDO;
import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.scheduling.annotation.Async;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Resource;

import cn.iocoder.yudao.module.temu.service.pdf.AsyncPdfProcessService;
import cn.iocoder.yudao.module.temu.mq.producer.weixin.WeiXinProducer;

import static cn.iocoder.yudao.framework.common.enums.UserTypeEnum.ADMIN;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser;
import static cn.iocoder.yudao.module.pay.enums.ErrorCodeConstants.WALLET_NOT_FOUND;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;
import cn.hutool.core.collection.CollUtil;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;

import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderPlacementRecordDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderPlacementRecordMapper;
import java.time.LocalDate;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.OrderSkuPageRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.OrderGroupBySortingSequenceVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.OrderSkuPageItemVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderShippingInfoDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderShippingMapper;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderReturnDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderReturnMapper;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuWorkerTaskDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuWorkerTaskMapper;

@Service
@Slf4j
public class TemuOrderService implements ITemuOrderService {
	@Resource
	private TemuOrderMapper temuOrderMapper;
	@Resource
	private TemuShopMapper temuShopMapper;
	
	@Resource
	TemuUserShopMapper temuUserShopMapper;
	
	@Resource
	private TemuOrderBatchRelationMapper temuOrderBatchRelationMapper;

	@Resource
	private WeiXinProducer weiXinProducer;
	
	@Resource
	TemuProductCategoryMapper temuProductCategoryMapper;
	@Resource
	private TemuProductCategorySkuMapper temuProductCategorySkuMapper;
	@Resource
	private TemuShopOldTypeSkcMapper temuShopOldTypeSkcMapper;
	
	@Resource
	private TemuOssService temuOssService;
	
	@Resource
	private AsyncPdfProcessService asyncPdfService;
	
	@Resource
	private DictTypeMapper dictTypeMapper;
	@Resource
	private PayWalletService payWalletService;

	@Resource
	private TemuOrderBatchCategoryService temuOrderBatchCategoryService;
	
	@Resource
	private TemuOrderPlacementRecordMapper temuOrderPlacementRecordMapper;

	@Resource
	private ConfigApi configApi;
	
	@Resource
	private TemuOrderShippingMapper shippingInfoMapper;
	
	@Resource
	private TemuOrderReturnMapper temuOrderReturnMapper;
	
	@Resource
	private TemuWorkerTaskMapper temuWorkerTaskMapper;

	@Resource
	private TemuVipOrderPlacementRecordMapper temuVipOrderPlacementRecordMapper;

	@Resource
	private TemuVipProductCategoryMapper temuVipProductCategoryMapper;
	
	@Override
	public PageResult<TemuOrderDetailDO> list(TemuOrderRequestVO temuOrderRequestVO) {
		return temuOrderMapper.selectPage(temuOrderRequestVO);
	}
	
	@Override
	public TemuOrderStatisticsRespVO statistics(TemuOrderRequestVO temuOrderRequestVO) {
		return temuOrderMapper.statistics(temuOrderRequestVO);
	}
	
	@Override
	public TemuOrderStatisticsRespVO statistics(TemuOrderRequestVO temuOrderRequestVO, Long userId) {
		List<TemuUserShopDO> list = temuUserShopMapper.selectList(TemuUserShopDO::getUserId, userId);
		ArrayList<String> shopIdList = new ArrayList<>();
		if (!list.isEmpty()) {
			list.forEach(temuUserShopDO -> {
				shopIdList.add(temuUserShopDO.getShopId().toString());
			});
			return temuOrderMapper.statistics(temuOrderRequestVO, shopIdList);
		}
		return new TemuOrderStatisticsRespVO();
		
	}
	
	/**
	 * 根据给定的查询条件和用户ID，分页查询Temu订单详情列表。
	 *
	 * @param temuOrderRequestVO 包含查询条件的请求对象，用于过滤订单详情数据。
	 * @param userId             用户ID，用于标识当前操作的用户（尽管在此方法中未直接使用，但可能用于后续扩展或权限控制）。
	 * @return 返回分页查询结果，包含符合条件的Temu订单详情数据列表。
	 */
	@Override
	public PageResult<TemuOrderDetailDO> list(TemuOrderRequestVO temuOrderRequestVO, Long userId) {
		
		List<TemuUserShopDO> list = temuUserShopMapper.selectList(TemuUserShopDO::getUserId, userId);
		ArrayList<String> shopIdList = new ArrayList<>();
		if (!list.isEmpty()) {
			list.forEach(temuUserShopDO -> {
				shopIdList.add(temuUserShopDO.getShopId().toString());
			});
			return temuOrderMapper.selectPage(temuOrderRequestVO, shopIdList);
		} else {
			return new PageResult<>();
		}
		
	}
	
	@Override
	@Transactional
	@LogRecord(
			success = "id：{{#user.id}} 更新了{{#orderSize}}条数据,提交的数据是{{#orderString}}",
			type = "TEMU订单操作", bizNo = "{{#user.id}}")
	public Boolean beatchUpdateStatus(List<TemuOrderDO> requestVO) {
		// 必须在方法开始时设置LogRecord上下文变量，以便SpEL表达式能够正确解析
		LogRecordContext.putVariable("user", getLoginUser());
		LogRecordContext.putVariable("orderSize", requestVO.size());
		HashMap<String, String> stringStringHashMap = new HashMap<>();
		requestVO.iterator().forEachRemaining(temuOrderDO -> {
			stringStringHashMap.put("id", String.valueOf(temuOrderDO.getId()));
			stringStringHashMap.put("orderStatus", String.valueOf(temuOrderDO.getOrderStatus()));
		});
		LogRecordContext.putVariable("orderString", JsonUtils.toJsonString(stringStringHashMap));
		
		for (TemuOrderDO temuOrderDO : requestVO) {
			// 先查询原始订单数据
			TemuOrderDO originalOrder = temuOrderMapper.selectById(temuOrderDO.getId());
			//如果当前订单 已做图或者已生产 则无法取消订单
			if ((originalOrder.getIsCompleteDrawTask().equals(1)) || originalOrder.getIsCompleteDrawTask().equals(1)) {
				return false;
			}

			//作图和生产任务标识重置为未完成
			temuOrderDO.setIsCompleteDrawTask(0);
			temuOrderDO.setIsCompleteProducerTask(0);

			if (TemuOrderStatusEnum.UNDELIVERED == temuOrderDO.getOrderStatus()) {

				if (originalOrder != null && originalOrder.getOriginalQuantity() != null) {
					// 将制作数量重置为与官网一致的原始数量
					temuOrderDO.setQuantity(originalOrder.getOriginalQuantity());
				}
			}

			// 订单取消时退回金额
			if (temuOrderDO.getOrderStatus() == TemuOrderStatusEnum.UNDELIVERED) {
				// 只对未退过款的订单进行退款（可加退款标记字段，当前简单实现）
				if (originalOrder != null && originalOrder.getTotalPrice() != null && originalOrder.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
					LoginUser loginUser = getLoginUser();
					if (loginUser != null) {
						PayWalletDO wallet = payWalletService.getOrCreateWallet(loginUser.getId(), ADMIN.getValue());
						if (wallet != null) {
							payWalletService.addWalletBalance(wallet.getId(), String.valueOf(temuOrderDO.getId()), PayWalletBizTypeEnum.PAYMENT_REFUND, originalOrder.getTotalPrice().multiply(new BigDecimal(100)).intValue());
							log.info("订单{}已取消，退回金额{}到用户{}的钱包", temuOrderDO.getOrderNo(), originalOrder.getTotalPrice(), loginUser.getId());
						}
					}
				}
			}
		}

		Boolean result = temuOrderMapper.updateBatch(requestVO);

		// 如果更新成功，删除对应的批次关系记录
		if (result) {
			for (TemuOrderDO temuOrderDO : requestVO) {
				temuOrderBatchRelationMapper.deleteByOrderId(temuOrderDO.getId());
			}
		}

		return result;
	}
	
	@Override
	public int saveOrders(String shopId, String shopName, List<Map<String, Object>> ordersList, String originalJson) {
		if (ordersList == null || ordersList.isEmpty()) {
			return 0;
		}
		try {
			// 上传图片到阿里云 以图搜图
			// 创建HTTP客户端
			HttpClient httpClient = HttpClients.createDefault();
			HttpPost httpPost = new HttpPost("http://39.106.136.96:8088/upload");

			// 设置请求头
			httpPost.setHeader("Content-Type", "application/json");

			// 设置请求体
			StringEntity entity = new StringEntity(originalJson, "UTF-8");
			httpPost.setEntity(entity);

			// 发送请求并获取响应
			HttpResponse response = httpClient.execute(httpPost);

			// 检查响应状态
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200 && statusCode != 202) {
				log.error("上传原始JSON失败，状态码: {}", statusCode);
			}
		} catch (Exception e) {
			log.error("上传原始JSON时发生错误: {}", e.getMessage(), e);
		}

		int count = 0;
		Long shopIdLong = Long.parseLong(shopId);
		
		// 1. 收集所有的SKC
		Set<String> skcSet = ordersList.stream()
				.map(orderMap -> convertToString(orderMap.get("skc")))
				.filter(skc -> !skc.isEmpty())
				.collect(Collectors.toSet());
		
		// 2. 批量查询合规单URL
		Map<String, TemuShopOldTypeSkcDO> skcToOldTypeMap = new HashMap<>();
		if (!skcSet.isEmpty()) {
			LambdaQueryWrapper<TemuShopOldTypeSkcDO> queryWrapper = new LambdaQueryWrapper<TemuShopOldTypeSkcDO>()
					.eq(TemuShopOldTypeSkcDO::getShopId, shopIdLong)
					.in(TemuShopOldTypeSkcDO::getSkc, skcSet);
			List<TemuShopOldTypeSkcDO> oldTypeSkcList = temuShopOldTypeSkcMapper.selectList(queryWrapper);
			skcToOldTypeMap = oldTypeSkcList.stream()
					.collect(Collectors.toMap(TemuShopOldTypeSkcDO::getSkc, oldType -> oldType));
		}
		
		// 3. 批量生成sorting_sequence（基于ordersList数据，不查询数据库）
		generateSortingSequenceBatch(ordersList);

		// 从配置中获取是否开启可动态配置批次生成时间段
		String isSortingSequence = configApi.getConfigValueByKey("temu_is_sortingSequence");
		log.info("中包序号功能是否开启: {}", isSortingSequence);
		boolean flag = false; // 默认值
		if (StrUtil.isNotEmpty(isSortingSequence)) {
			try {
				flag = Boolean.parseBoolean(isSortingSequence);
			} catch (Exception e) {
				log.warn("中包序号功能是否开启，使用默认值");
			}
		}
		if(flag){
			// 4. 验证生成的序号一致性
			validateSortingSequenceConsistency(ordersList);
		}
		
		for (Map<String, Object> orderMap : ordersList) {
			try {
				TemuOrderDO order = new TemuOrderDO();
				
				// 设置基本信息

				// 如果订单详情中带有shopId，说明是合并店铺发货，则使用订单详情中的shopId，最外层的shopId就可以忽略掉
				String orderShopId = convertToString(orderMap.get("shopId"));
				order.setShopId(StrUtil.isNotEmpty(orderShopId) ? Long.parseLong(orderShopId) : shopIdLong);
				
				order.setOrderNo(convertToString(orderMap.get("orderId")));
				order.setProductTitle(convertToString(orderMap.get("title")));
				order.setProductImgUrl(convertToString(orderMap.get("product_img_url")));
				order.setEffectiveImgUrl(convertToString(orderMap.get("effective_image_url"))); // 写入合成预览图url信息

				// 设置SKU相关信息
				String skc = convertToString(orderMap.get("skc"));
				order.setSkc(skc);
				
				// 设置合规单URL
				if (!skc.isEmpty()) {
					TemuShopOldTypeSkcDO oldTypeSkcDO = skcToOldTypeMap.get(skc);
					if (oldTypeSkcDO != null) {
						order.setComplianceUrl(oldTypeSkcDO.getOldTypeUrl());
						order.setComplianceImageUrl(oldTypeSkcDO.getOldTypeImageUrl());
					}
				}
				
				Map<String, Object> skusMap = (Map<String, Object>) orderMap.get("skus");
				String sku = "";
				String properties = "";
				if (skusMap != null) {
					sku = convertToString(skusMap.get("skuId"));
					order.setSku(sku);
					order.setCustomSku(convertToString(skusMap.get("customSku")));
					properties = convertToString(skusMap.get("property"));
					order.setProductProperties(properties);
				}
				
				// 如果没有SKU信息，查询历史订单
				if (sku.isEmpty() && !properties.isEmpty()) {
					LambdaQueryWrapper<TemuOrderDO> queryWrapper = new LambdaQueryWrapper<>();
					queryWrapper.eq(TemuOrderDO::getShopId, shopIdLong)
							.eq(TemuOrderDO::getProductProperties, properties)
							.last("LIMIT 1"); // 只取一条记录
					
					TemuOrderDO historicalOrder = temuOrderMapper.selectOne(queryWrapper);
					if (historicalOrder != null && historicalOrder.getSku() != null) {
						// 将历史订单的SKU信息赋值给当前订单
						order.setSku(historicalOrder.getSku());
						sku = historicalOrder.getSku(); // 更新sku变量，用于后续的分类查询
					}
				}
				
				// 查询商品分类信息
				if (!sku.isEmpty()) {
					HashMap<String, Object> queryMap = MapUtil.of("sku", sku);
					queryMap.put("shop_id", shopIdLong);
					List<TemuProductCategorySkuDO> categorySkuList = temuProductCategorySkuMapper.selectByMap(queryMap);
					if (categorySkuList != null && !categorySkuList.isEmpty()) {
						TemuProductCategorySkuDO categorySku = categorySkuList.get(0);
						order.setCategoryId(String.valueOf(categorySku.getCategoryId()));
						order.setCategoryName(categorySku.getCategoryName());
					}
				}
				
				// 设置商品条形码图片URL到goods_sn字段
				order.setGoodsSn(convertToString(orderMap.get("barcode_image_url")));
				// 新增PDF合并逻辑（确保customSku已赋值）
				String complianceUrl = order.getComplianceUrl();
				String goodsSnUrl = order.getGoodsSn();
				String currentCustomSku = order.getCustomSku();
				
				// 若customSku为空但查询到历史订单的sku，用历史sku补充
				if (StrUtil.isBlank(currentCustomSku) && StrUtil.isNotBlank(sku)) {
					currentCustomSku = sku;
					order.setCustomSku(currentCustomSku); // 更新订单对象
				}

				// 从商品条形码PDF提取指定页面
				if (StrUtil.isNotBlank(goodsSnUrl) && StrUtil.isNotBlank(currentCustomSku)) {
					CompletableFuture<String> goodsSnFuture = asyncPdfService.extractPageAsync(
							goodsSnUrl,
							currentCustomSku,
							temuOssService);

					// 处理提取结果，并在成功后进行PDF合并
					goodsSnFuture.thenAccept(extractedUrl -> {
						if (extractedUrl != null) {
							// 更新订单的商品条码URL
							updateOrderGoodsSn(order.getId(), extractedUrl);

							// 判断合规单URL是否存在，存在则进行合并
							if (StrUtil.isNotBlank(complianceUrl)) {
								// 使用提取后的页面URL进行合并
								CompletableFuture<String> mergedUrlFuture = asyncPdfService.processPdfAsync(
										complianceUrl,
										extractedUrl,
										temuOssService);

								// 设置回调更新订单
								mergedUrlFuture.thenAccept(mergedUrl -> {
									if (mergedUrl != null) {
										// 更新订单合并后的PDF地址
										updateOrderMergedUrl(order.getId(), mergedUrl);
									}
								});
							}
						} else {
							// order.setGoodsSn(null);
							// 条码是错的,删除条形码PDF
							updateOrderGoodsSn(order.getId(), null);
						}
					});
				} 

				// 设置价格和数量
				order.setSalePrice(new BigDecimal(convertToString(orderMap.get("price"))));
				order.setQuantity(Integer.valueOf(convertToString(orderMap.get("quantity"))));
				order.setOriginalQuantity(Integer.valueOf(convertToString(orderMap.get("quantity"))));
				
				
				// 设置订单状态
				// todo 前端上传上来使用枚举值，不要使用string
				String status = convertToString(orderMap.get("status"));
				if ("待发货".equals(status)) {
					order.setOrderStatus(0);
				}
				
				// 设置时间
				String creationTime = convertToString(orderMap.get("creationTime"));
				if (creationTime != null && !creationTime.isEmpty()) {
					order.setBookingTime(parseDateTime(creationTime));
				}
				
				// 处理自定义图片和文字
				List<String> customImages = (List<String>) orderMap.get("customImages");
				if (customImages != null && !customImages.isEmpty()) {
					order.setCustomImageUrls(String.join(",", customImages));
				}
				
				List<String> customTexts = (List<String>) orderMap.get("customTexts");
				if (customTexts != null && !customTexts.isEmpty()) {
					order.setCustomTextList(String.join(",", customTexts));
				}
				
				// 处理物流信息
				if (orderMap.get("shippingInfo") != null) {
					order.setShippingInfo(JSONUtil.toJsonStr(orderMap.get("shippingInfo")));
				}
				
				// 保存原始信息
				order.setOriginalInfo(JSONUtil.toJsonStr(orderMap));
				
				// 设置sorting_sequence
				order.setSortingSequence(convertToString(orderMap.get("sorting_sequence")));
				
				// 检查订单是否已存在
				List<TemuOrderDO> existingOrders = temuOrderMapper.selectByCustomSku(order.getCustomSku());
				// 返单的订单，备货单ID会不一样
				TemuOrderDO existingOrder = null;
				// 用定制sku和订单号一起查询当前订单的isReturnOrder
				TemuOrderDO currentOrderInDb = temuOrderMapper.selectByCustomSkuAndOrderNo(order.getCustomSku(), order.getOrderNo());
				boolean alreadyReturn = currentOrderInDb != null && currentOrderInDb.getIsReturnOrder() != null && currentOrderInDb.getIsReturnOrder() == 1;
				
				// 如果是返单订单，插入返单记录表

				if (CollUtil.isNotEmpty(existingOrders)) {
					for (TemuOrderDO tempOrder : existingOrders) {
						if (!tempOrder.getOrderNo().equals(order.getOrderNo())) {
							// bookingTime更晚的订单，为返单
							if (order.getBookingTime() != null && order.getBookingTime().isAfter(tempOrder.getBookingTime())) {
								if (!alreadyReturn) {
									
										// 查询工人任务信息
										String drawUserName = null;
										String produceUserName = null;
										String shipUserName = null;
										// 使用customSku查询temu_worker_task表
										LambdaQueryWrapperX<TemuWorkerTaskDO> queryWrapper = new LambdaQueryWrapperX<>();
										queryWrapper.eq(TemuWorkerTaskDO::getCustomSku, order.getCustomSku());
										List<TemuWorkerTaskDO> workerTasks = temuWorkerTaskMapper.selectList(queryWrapper);
										if (workerTasks != null && !workerTasks.isEmpty()) {
											for (TemuWorkerTaskDO workerTask : workerTasks) {
												if (workerTask.getTaskType() != null) {
													switch (workerTask.getTaskType()) {
														case 1:
															drawUserName = workerTask.getWorkerName();
														case 2:
															produceUserName = workerTask.getWorkerName();
														case 3:
															shipUserName = workerTask.getWorkerName();
													}
												}
											}
										}

										// 创建返单记录
										TemuOrderReturnDO returnOrder = new TemuOrderReturnDO();
										returnOrder.setOrderNo(order.getOrderNo());
										returnOrder.setCreatedAt(order.getBookingTime());
										returnOrder.setShopId(order.getShopId());
										returnOrder.setProductTitle(order.getProductTitle());
										returnOrder.setSku(order.getSku());
										returnOrder.setSkc(order.getSkc());
										returnOrder.setCustomSku(order.getCustomSku());
										returnOrder.setProductImgUrl(order.getProductImgUrl());
										returnOrder.setProductProperties(order.getProductProperties());
										returnOrder.setDrawUserName(drawUserName);
										returnOrder.setProduceUserName(produceUserName);
										returnOrder.setShipUserName(shipUserName);
										returnOrder.setRepeatReason(1);
										returnOrder.setAliasName(shopName);
										// 插入返单记录
										temuOrderReturnMapper.insert(returnOrder);
									// 发送企业微信告警，提醒返单
									 String message = String.format("警告：发现返单情况，请检查订单：\n定制SKU: %s\n原订单号: %s\n新订单号: %s\n原订单日期: %s\n新订单日期: %s\n店铺: %s",
									 	order.getCustomSku(), tempOrder.getOrderNo(), order.getOrderNo(),
									 	tempOrder.getBookingTime(), order.getBookingTime(), shopName);
									TemuShopDO shop = temuShopMapper.selectByShopId(88888888L);
									order.setIsReturnOrder(1);
									if (shop != null && StrUtil.isNotEmpty(shop.getWebhook())) {
										 weiXinProducer.sendMessage(shop.getWebhook(), message);
									}
								}
							}
						} else {
							existingOrder = tempOrder;
						}
					}
				}

				if (existingOrder != null) {
					// 更新现有订单，只更新非空字段
					order.setId(existingOrder.getId());
					// 订单的状态不能随意更新，保持原有状态
					order.setOrderStatus(existingOrder.getOrderStatus());
					
					// 比对并填充字段
					if (!StringUtils.hasText(order.getOrderNo())) order.setOrderNo(existingOrder.getOrderNo());
					if (!StringUtils.hasText(order.getProductTitle()))
						order.setProductTitle(existingOrder.getProductTitle());
					if (!StringUtils.hasText(order.getSku())) order.setSku(existingOrder.getSku());
					if (!StringUtils.hasText(order.getSkc())) order.setSkc(existingOrder.getSkc());
					if (order.getSalePrice() == null) order.setSalePrice(existingOrder.getSalePrice());
					if (!StringUtils.hasText(order.getCustomSku())) order.setCustomSku(existingOrder.getCustomSku());
					
					// 先保存原始订单数量，用于后续判断是否为返单
					// Integer newOrderQuantity = order.getQuantity();
					// Integer newOrderOriginalQuantity = order.getOriginalQuantity();
					
					if (existingOrder.getQuantity() != null && existingOrder.getQuantity() > 0) {
						// 如果数据库现存的order中quantity存在且大于0,则保持原值不更新
						order.setQuantity(existingOrder.getQuantity());
					}
					if (existingOrder.getOriginalQuantity() != null && existingOrder.getOriginalQuantity() > 0) {
						// 如果originalQuantity存在且大于0,则保持原值不更新
						order.setOriginalQuantity(existingOrder.getOriginalQuantity());
					}
					if (!StringUtils.hasText(order.getProductProperties()))
						order.setProductProperties(existingOrder.getProductProperties());

					// 标记是否为返单
					// boolean isReturnOrder = false;
					
					// 如果bookingTime不一样，说明是返单
					// if (order.getBookingTime() != null && order.getBookingTime().isAfter(existingOrder.getBookingTime())) {
					// 	// 标记为返单
					// 	isReturnOrder = true;
					// 	// 删除关联关系
					// 	temuOrderBatchRelationMapper.deleteByOrderId(existingOrder.getId());
					// 	// 将状态置为0
					// 	order.setOrderStatus(0);
					// 	// 对于返单，使用新上传的订单数量，而不是保留原有数量
					// 	// 恢复新订单的数量值，覆盖前面的保留逻辑
					// 	if (newOrderQuantity != null) {
					// 		order.setQuantity(newOrderQuantity);
					// 	}
					// 	if (newOrderOriginalQuantity != null) {
					// 		order.setOriginalQuantity(newOrderOriginalQuantity);
					// 	}
						
					// 	// 将作图完成和生产完成状态回退为未完成
					// 	order.setIsCompleteDrawTask(0);
					// 	order.setIsCompleteProducerTask(0);
						
					// 	// 确保使用新订单的价格信息
					// 	// 先将价格明确设置为0，而不是null，确保能覆盖数据库中的旧值
					// 	// 后续batchSaveOrder时会根据订单数量重新计算价格
					// 	order.setUnitPrice(BigDecimal.ZERO);
					// 	order.setTotalPrice(BigDecimal.ZERO);

					// 	// 发送企业微信消息
					// 	String message = String.format("店铺：%s 订单：%s 定制SKU：%s 发生返单，原预约时间: %s, 新预约时间: %s", 
					// 		shopName,
					// 		order.getOrderNo(),	
					// 		order.getCustomSku(),
					// 		DateUtil.format(existingOrder.getBookingTime(), "yyyy-MM-dd HH:mm:ss"),
					// 		DateUtil.format(order.getBookingTime(), "yyyy-MM-dd HH:mm:ss"));
					// 	// 获取shopId为88888888的店铺webhook地址
					// 	TemuShopDO shop = temuShopMapper.selectByShopId(88888888L);
					// 	if (shop != null && StringUtils.hasText(shop.getWebhook())) {
					// 		weiXinProducer.sendMessage(shop.getWebhook(), message.toString());
					// 	}
					// }
					// 如果bookingTime为空，则使用existingOrder的bookingTime,这种是上传定制图片的场景
					if (order.getBookingTime() == null) {
						order.setBookingTime(existingOrder.getBookingTime());
					}
					if (order.getShopId() == null) order.setShopId(existingOrder.getShopId());
					if (!StringUtils.hasText(order.getCustomImageUrls()))
						order.setCustomImageUrls(existingOrder.getCustomImageUrls());
					if (!StringUtils.hasText(order.getCustomTextList()))
						order.setCustomTextList(existingOrder.getCustomTextList());
					if (!StringUtils.hasText(order.getProductImgUrl()))
						order.setProductImgUrl(existingOrder.getProductImgUrl());
					if (!StringUtils.hasText(order.getCategoryId())) order.setCategoryId(existingOrder.getCategoryId());
					if (!StringUtils.hasText(order.getCategoryName()))
						order.setCategoryName(existingOrder.getCategoryName());
					if (!StringUtils.hasText(order.getShippingInfo()))
						order.setShippingInfo(existingOrder.getShippingInfo());
					if (!StringUtils.hasText(order.getOriginalInfo()))
						order.setOriginalInfo(existingOrder.getOriginalInfo());
					if (!StringUtils.hasText(order.getEffectiveImgUrl()))
						order.setEffectiveImgUrl(existingOrder.getEffectiveImgUrl());
					if (order.getUnitPrice() == null) order.setUnitPrice(existingOrder.getUnitPrice());
					if (order.getTotalPrice() == null) order.setTotalPrice(existingOrder.getTotalPrice());
					
					// 如果不是返单，才从旧订单中填充价格信息
					// if (!isReturnOrder) {
					// 	if (order.getUnitPrice() == null) order.setUnitPrice(existingOrder.getUnitPrice());
					// 	if (order.getTotalPrice() == null) order.setTotalPrice(existingOrder.getTotalPrice());
					// }
					
					if (order.getGoodsSn() == null) order.setGoodsSn(existingOrder.getGoodsSn());
					if (order.getComplianceUrl() == null) order.setComplianceUrl(existingOrder.getComplianceUrl());
					if (order.getComplianceImageUrl() == null)
						order.setComplianceImageUrl(existingOrder.getComplianceImageUrl());
					if (order.getIsCompleteDrawTask() == null) order.setIsCompleteDrawTask(existingOrder.getIsCompleteDrawTask());
					if (order.getIsCompleteProducerTask() == null) order.setIsCompleteProducerTask(existingOrder.getIsCompleteProducerTask());
					if (order.getIsReturnOrder() == null) order.setIsReturnOrder(existingOrder.getIsReturnOrder());

					if(flag){
						// 序号处理：优先保留原有序号，确保SKU-序号对应关系稳定
						if (StringUtils.hasText(existingOrder.getSortingSequence())) {
							// 保留原有序号，避免重新排序导致的分拣混乱
							order.setSortingSequence(existingOrder.getSortingSequence());
							log.debug("保留原有序号: orderNo={}, sku={}, existingSortingSequence={}",
									order.getOrderNo(), order.getSku(), existingOrder.getSortingSequence());
						} else if (!StringUtils.hasText(order.getSortingSequence())) {
							// 如果原有序号为空且新序号也为空，则使用新生成的序号
							String newSortingSequence = convertToString(orderMap.get("sorting_sequence"));
							order.setSortingSequence(newSortingSequence);
							log.debug("使用新生成序号: orderNo={}, sku={}, newSortingSequence={}",
									order.getOrderNo(), order.getSku(), newSortingSequence);
						}
					}

					temuOrderMapper.updateById(order);
				} else {
					// 插入新订单
					temuOrderMapper.insert(order);
				}
				count++;

				// ========== sorting_sequence 赋值逻辑 ===========
				// 新的方案：使用订单编号后6位 + 后缀的方式
				// generateSortingSequence(order);
			} catch (Exception e) {
				log.error("保存订单失败: {}", e.getMessage(), e);
				// 继续处理下一个订单
			}
		}
		
		// 同时保存或更新店铺信息
		saveShopInfo(shopIdLong, shopName);
		// 异步插入临时物流信息（开关控制）
		String isTempShippingEnabled = configApi.getConfigValueByKey("is_TempShipping_Enabled");
		boolean tempShippingEnabled = false;
		if (StringUtils.hasText(isTempShippingEnabled)) {
			try {
				tempShippingEnabled = Boolean.parseBoolean(isTempShippingEnabled);
			} catch (Exception e) {}
		}
		if (tempShippingEnabled) {
			saveTempShippingInfoBatch(shopIdLong, ordersList);
		}
		return count;
	}
	
	@Override
	public int updateCategory(TemuOrderUpdateCategoryReqVo requestVO) {
		//根据查询订单是否存在
		TemuOrderDO temuOrderDO = temuOrderMapper.selectById(requestVO.getId());
		if (temuOrderDO == null) {
			throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
		}
		//检查分类id是否存在
		List<TemuProductCategoryDO> list = temuProductCategoryMapper.selectByMap(MapUtil.of("category_id", requestVO.getCategoryId()));
		if (list == null || list.isEmpty()) {
			throw exception(ErrorCodeConstants.CATEGORY_NOT_EXISTS);
		}
		//检查temu_product_category_sku表是否选择记录
		HashMap<String, Object> map = MapUtil.of("sku", temuOrderDO.getSku());
		map.put("shop_id", temuOrderDO.getShopId());
		List<TemuProductCategorySkuDO> temuProductCategorySkuDOList = temuProductCategorySkuMapper.selectByMap(map);
		//如果存在记录 更新分类id
		if (temuProductCategorySkuDOList != null && !temuProductCategorySkuDOList.isEmpty()) {
			//	更新类目id
			temuProductCategorySkuDOList.forEach(temuProductCategorySkuDO -> {
				temuProductCategorySkuDO.setCategoryId(Long.parseLong(requestVO.getCategoryId()));
				temuProductCategorySkuDO.setCategoryName(list.get(0).getCategoryName());
				temuProductCategorySkuMapper.updateById(temuProductCategorySkuDO);
			});
		} else {
			//	插入新数据
			TemuProductCategorySkuDO temuProductCategorySkuDO = new TemuProductCategorySkuDO();
			temuProductCategorySkuDO.setCategoryId(Long.parseLong(requestVO.getCategoryId()));
			temuProductCategorySkuDO.setCategoryName(list.get(0).getCategoryName());
			temuProductCategorySkuDO.setSku(temuOrderDO.getSku());
			temuProductCategorySkuDO.setShopId(temuOrderDO.getShopId());
			temuProductCategorySkuMapper.insert(temuProductCategorySkuDO);
		}
		temuOrderDO.setCategoryId(requestVO.getCategoryId());
		temuOrderDO.setCategoryName(list.get(0).getCategoryName());
		return temuOrderMapper.updateById(temuOrderDO);
	}
	
	/**
	 * 批量保存订单信息，并根据订单数量匹配相应的价格规则。
	 * 该函数会遍历传入的订单列表，检查每个订单是否存在，并根据订单的分类信息获取价格规则。
	 * 最终更新订单的价格信息并保存到数据库中。
	 *
	 * @param requestVO 包含多个订单信息的请求对象列表
	 * @return 成功处理的订单数量
	 * @throws ServerException 如果订单不存在、分类不存在或分类价格规则不存在时抛出异常
	 */
	@Override
	@Transactional
	public int batchSaveOrder(List<TemuOrderBatchOrderReqVO> requestVO) {
		// 1. 获取所有订单ID
		List<Long> orderIds = requestVO.stream()
				.map(TemuOrderBatchOrderReqVO::getId)
				.collect(Collectors.toList());

		// 2. 批量查询订单，只获取id和categoryId字段
		List<TemuOrderDO> orders = temuOrderMapper.selectBatchIds(orderIds);

		// 3. 使用Map存储categoryId和对应的订单id列表
		Map<String, List<Long>> categoryOrderMap = orders.stream()
				.collect(Collectors.groupingBy(
						TemuOrderDO::getCategoryId,
						Collectors.mapping(TemuOrderDO::getId, Collectors.toList())));

		ArrayList<TemuOrderDO> temuOrderDOList = new ArrayList<>();
		int processCount = 0;
		LoginUser loginUser = getLoginUser();
		String operator = SecurityFrameworkUtils.getLoginUserNickname();
		Long operatorId = loginUser != null ? loginUser.getId() : null;
		LocalDateTime now = LocalDateTime.now();
		for (TemuOrderBatchOrderReqVO temuOrderBatchOrderReqVO : requestVO) {
			//检查订单是否存在
			TemuOrderDO temuOrderDO = temuOrderMapper.selectById(temuOrderBatchOrderReqVO.getId());
			if (temuOrderDO == null) {
				throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
			}
			//根据订单的关联分类id查询分类信息
			TemuProductCategoryDO temuProductCategoryDO = temuProductCategoryMapper.selectById(temuOrderDO.getCategoryId());
			if (temuProductCategoryDO == null) {
				throw exception(ErrorCodeConstants.CATEGORY_NOT_EXISTS);
			}
			//检查订单状态
			if (temuOrderDO.getOrderStatus() != TemuOrderStatusEnum.UNDELIVERED) {
				throw exception(ErrorCodeConstants.ORDER_STATUS_ERROR);
			}
			
			//更新数量
			temuOrderDO.setQuantity(temuOrderBatchOrderReqVO.getQuantity());
			
			// 判断是否为返单，返单直接设置价格为0，不进行价格计算
			if (temuOrderDO.getIsReturnOrder() != null && temuOrderDO.getIsReturnOrder() == 1) {
				// 返单订单：单价和总价都设为0
				temuOrderDO.setUnitPrice(BigDecimal.ZERO);
				temuOrderDO.setTotalPrice(BigDecimal.ZERO);
				log.info("返单订单 {} 价格设置为0，不进行扣费", temuOrderDO.getOrderNo());
			} else {
				// 非返单订单：正常计算价格
				BigDecimal unitPrice;
				IPriceRule rule;
				//根据规则类型加载不同的对象
				rule = PriceRuleFactory.createPriceRule(temuProductCategoryDO.getRuleType(), temuProductCategoryDO.getUnitPrice());
				unitPrice = rule.calcUnitPrice(temuOrderBatchOrderReqVO.getQuantity());
				temuOrderDO.setUnitPrice(unitPrice);
				temuOrderDO.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(temuOrderBatchOrderReqVO.getQuantity())));
			}
			
			//修改订单状态
			temuOrderDO.setOrderStatus(TemuOrderStatusEnum.ORDERED);
			temuOrderDOList.add(temuOrderDO);
			processCount++;

			//插入下单记录表
			TemuOrderPlacementRecordDO record = new TemuOrderPlacementRecordDO();
			record.setOrderNo(temuOrderDO.getOrderNo());
			record.setShopId(temuOrderDO.getShopId());
			TemuShopDO shop = temuShopMapper.selectByShopId(temuOrderDO.getShopId());
			record.setShopName(shop != null ? shop.getShopName() : null);
			record.setProductTitle(temuOrderDO.getProductTitle());
			record.setProductProperties(temuOrderDO.getProductProperties());
			if (temuOrderDO.getCategoryId() != null) {
				try {
					record.setCategoryId(Long.valueOf(temuOrderDO.getCategoryId()));
				} catch (Exception ignore) {}
			}
			record.setCategoryName(temuOrderDO.getCategoryName());
			record.setOriginalQuantity(temuOrderDO.getOriginalQuantity());
			record.setQuantity(temuOrderDO.getQuantity());
			record.setUnitPrice(temuOrderDO.getUnitPrice());
			record.setTotalPrice(temuOrderDO.getTotalPrice());
			record.setSku(temuOrderDO.getSku());
			record.setSkc(temuOrderDO.getSkc());
			record.setCustomSku(temuOrderDO.getCustomSku());
			record.setOperationTime(now);
			record.setIsReturnOrder(temuOrderDO.getIsReturnOrder() != null && temuOrderDO.getIsReturnOrder() == 1);
			record.setOperatorId(operatorId);
			record.setOperator(operator);
			temuOrderPlacementRecordMapper.insert(record);
		}
		//批量更新订单
		temuOrderMapper.updateBatch(temuOrderDOList);
		//批量支付订单
		payOrderBatch(temuOrderDOList);

		//检查当前订单是否允许自动打批次
		DictTypeDO dictTypeDO = dictTypeMapper.selectByType("temu_order_batch_category_status");
		//如果状态开启那么开始自动打批次
		if(dictTypeDO.getStatus() == 0){
			// 获取batchCategoryId和订单id的映射
			Map<String, List<Long>> batchCategoryOrderMap = temuOrderBatchCategoryService
					.getBatchCategoryOrderMap(categoryOrderMap);
			temuOrderBatchCategoryService.processBatchAndRelations(batchCategoryOrderMap);
		}

		return processCount;
	}
	
	private void payOrderBatch(ArrayList<TemuOrderDO> temuOrderDOList) {
		//检查当前订单是否允许被支付
		DictTypeDO dictTypeDO = dictTypeMapper.selectByType("temu_order_batch_pay_order_status");
		//如果状态开启那么开始处理支付
		if (dictTypeDO.getStatus() == 0) {
			// 分离返单和非返单订单
			List<TemuOrderDO> returnOrders = temuOrderDOList.stream()
					.filter(order -> order.getIsReturnOrder() != null && order.getIsReturnOrder() == 1)
					.collect(Collectors.toList());
			
			List<TemuOrderDO> normalOrders = temuOrderDOList.stream()
					.filter(order -> order.getIsReturnOrder() == null || order.getIsReturnOrder() != 1)
					.collect(Collectors.toList());
			
			// 记录返单信息
			if (!returnOrders.isEmpty()) {
				log.info("发现 {} 个返单订单，跳过扣费：{}", 
					returnOrders.size(), 
					returnOrders.stream().map(TemuOrderDO::getOrderNo).collect(Collectors.joining(", ")));
			}
			
			// 如果没有正常订单需要扣费，直接返回
			if (normalOrders.isEmpty()) {
				log.info("所有订单都是返单，无需扣费");
				return;
			}
			
			//统计正常订单总金额（排除返单订单）
			BigDecimal totalPrice = normalOrders.stream()
					.map(TemuOrderDO::getTotalPrice)
					.reduce(BigDecimal::add)
					.orElse(BigDecimal.ZERO);
			
			log.info("需要扣费的订单数量：{}，总金额：{}", normalOrders.size(), totalPrice);
			
			//检查当前用户是否存在余额
			LoginUser loginUser = getLoginUser();
			if (loginUser == null) {
				throw exception(USER_NOT_EXISTS);
			}
			// 获得用户钱包
			PayWalletDO wallet = payWalletService.getOrCreateWallet(loginUser.getId(), ADMIN.getValue());
			if (wallet == null) {
				throw exception(WALLET_NOT_FOUND);
			}
			//检查用户余额是否充足
			if (new BigDecimal(wallet.getBalance() / 100).compareTo(totalPrice) < 0) {
				throw exception(ErrorCodeConstants.WALLET_NOT_ENOUGH);
			}
			
			// 只对非返单订单进行扣费
			normalOrders.forEach(temuOrderDO -> {
				log.info("扣费订单：{}，金额：{}", temuOrderDO.getOrderNo(), temuOrderDO.getTotalPrice());
				payWalletService.reduceWalletBalance(wallet.getId(), temuOrderDO.getId(), PayWalletBizTypeEnum.PAYMENT_TEMU_ORDER, temuOrderDO.getTotalPrice().multiply(new BigDecimal(100)).intValue());
			});
		}
	}
	
	@Override
	public TemuOrderExtraInfoRespVO getOrderExtraInfo(String orderId) {
		TemuOrderDO temuOrderDO = temuOrderMapper.selectById(orderId);
		if (temuOrderDO == null) {
			throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
		}
		// 根据订单的关联分类id查询分类信息
		TemuProductCategoryDO temuProductCategoryDO = temuProductCategoryMapper.selectByCategoryId(Long.valueOf(temuOrderDO.getCategoryId()));
		if (temuProductCategoryDO == null) {
			throw exception(ErrorCodeConstants.CATEGORY_NOT_EXISTS);
		}
		//获取店铺信息
		TemuShopDO temuShopDO = temuShopMapper.selectByShopId(temuOrderDO.getShopId());
		if (temuShopDO == null) {
			throw exception(ErrorCodeConstants.SHOP_NOT_EXISTS);
		}
		//根据分类类型匹配合规单
		Map<String, Object> oldTypeUrl = temuShopDO.getOldTypeUrl();
		
		return new TemuOrderExtraInfoRespVO(temuOrderDO.getGoodsSn(), oldTypeUrl != null ? convertToString(oldTypeUrl.get(temuProductCategoryDO.getOldType())) : "");
	}
	
	@Override
	public Boolean saveOrderRemark(TemuOrderSaveOrderRemarkReqVO requestVO) {
		TemuOrderDO temuOrderDO = temuOrderMapper.selectById(requestVO.getOrderId());
		if (temuOrderDO == null) {
			throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
		}
		temuOrderDO.setRemark(requestVO.getRemark());
		return temuOrderMapper.updateById(temuOrderDO) > 0;
	}
	
	@Override
	public Boolean updateOrderCustomImages(Long orderId, String customImageUrls) {
		TemuOrderDO temuOrderDO = new TemuOrderDO();
		temuOrderDO.setId(orderId);
		temuOrderDO.setCustomImageUrls(customImageUrls);
		return temuOrderMapper.updateById(temuOrderDO) > 0;
	}

	//批量更新订单状态
	@Override
	@Transactional
	@LogRecord(
			success = "批量更新订单状态，更新数量：{{#reqVOList.size()}}",
			type = "TEMU订单操作", bizNo = "{{#reqVOList[0].id}}")
	public Boolean updateOrderStatus(List<TemuOrderDO> reqVOList) {
		if (CollUtil.isEmpty(reqVOList)) {
			return true;
		}

		// 1. 参数校验
		List<Long> orderIds = reqVOList.stream()
				.map(TemuOrderDO::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		
		if (CollUtil.isEmpty(orderIds)) {
			throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
		}

		// 2. 校验订单是否存在
		List<TemuOrderDO> existOrders = temuOrderMapper.selectBatchIds(orderIds);
		if (CollUtil.isEmpty(existOrders) || existOrders.size() != orderIds.size()) {
			throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
		}

		// 3. 批量更新订单状态
		List<TemuOrderDO> updateOrders = reqVOList.stream()
				.map(reqVO -> {
					TemuOrderDO updateOrder = new TemuOrderDO();
					updateOrder.setId(reqVO.getId());
					updateOrder.setOrderStatus(reqVO.getOrderStatus());
					return updateOrder;
				})
				.collect(Collectors.toList());

		return temuOrderMapper.updateBatch(updateOrders);
	}

    @Override
    public OrderSkuPageRespVO orderSkuPage(TemuOrderRequestVO req, Integer pageNo, Integer pageSize) {
        // 1. 查询分组信息
        List<Map<String, Object>> groupResults = queryOrderGroups(req);
        
        // 2. 计算总数（分组数量）
        long total = groupResults.size();
        
        // 3. 分页处理分组
        int startIndex = (pageNo - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, groupResults.size());
        
        if (startIndex >= groupResults.size()) {
            return buildEmptyPageResult(pageNo, pageSize);
        }
        
        List<Map<String, Object>> pagedGroups = groupResults.subList(startIndex, endIndex);
        
        // 4. 根据分页后的分组查询对应的订单项
        List<OrderGroupBySortingSequenceVO> groupedList = pagedGroups.stream()
                .map(group -> buildGroupVO(group, req))
                .collect(Collectors.toList());
        
        // 5. 构建返回结果
        return buildPageResult(groupedList, total, pageNo, pageSize);
    }
    
    /**
     * 查询订单分组信息
     */
    private List<Map<String, Object>> queryOrderGroups(TemuOrderRequestVO req) {
        // 执行查询 - 使用QueryWrapper
        QueryWrapper<TemuOrderDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT sorting_sequence, order_no, sku, UNIX_TIMESTAMP(booking_time) * 1000 as booking_time");
        
        // 添加查询条件
        addQueryConditionsToWrapper(queryWrapper, req);
        
        // 按照booking_time降序排列，最新的数据在前面
        queryWrapper.orderByDesc("booking_time");
        
        // 执行查询
        return temuOrderMapper.selectMaps(queryWrapper);
    }
    
    /**
     * 根据分组信息查询对应的订单项
     */
    private OrderGroupBySortingSequenceVO buildGroupVO(Map<String, Object> group, TemuOrderRequestVO req) {
        OrderGroupBySortingSequenceVO groupVO = new OrderGroupBySortingSequenceVO();
        
        // 设置分组级别信息
        groupVO.setSortingSequence((String) group.get("sorting_sequence"));
        groupVO.setOrderNo((String) group.get("order_no"));
        groupVO.setSku((String) group.get("sku"));
        groupVO.setBookingTime((Long) group.get("booking_time"));
        
        // 根据分组条件查询对应的订单项
        List<OrderSkuPageItemVO> orderItems = queryOrderItemsByGroup(group, req);
        groupVO.setOrders(orderItems);
        
        return groupVO;
    }
    
    /**
     * 根据分组条件查询订单项
     */
    private List<OrderSkuPageItemVO> queryOrderItemsByGroup(Map<String, Object> group, TemuOrderRequestVO req) {
        String orderNo = (String) group.get("order_no");
        String sku = (String) group.get("sku");
        Long bookingTime = (Long) group.get("booking_time");
        
        // 构建查询条件
        LambdaQueryWrapperX<TemuOrderDO> queryWrapper = new LambdaQueryWrapperX<>();
        
        // 精确匹配分组条件
        queryWrapper.eq(TemuOrderDO::getOrderNo, orderNo)
                   .eq(TemuOrderDO::getSku, sku);
        
        // 时间匹配
        if (bookingTime != null) {
            LocalDateTime bookingDateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(bookingTime), 
                java.time.ZoneId.systemDefault()
            );
            queryWrapper.eq(TemuOrderDO::getBookingTime, bookingDateTime);
        }
        
        // 应用其他查询条件
        applyQueryConditions(queryWrapper, req);
        
        // 只查询订单项需要的字段
        queryWrapper.select(
            TemuOrderDO::getCustomSku,
            TemuOrderDO::getQuantity,
            TemuOrderDO::getOriginalQuantity,
            TemuOrderDO::getCategoryId,
            TemuOrderDO::getCategoryName,
            TemuOrderDO::getEffectiveImgUrl
        );
        
        // 查询并转换为VO
        return temuOrderMapper.selectList(queryWrapper).stream()
                .map(this::convertToOrderItemVO)
                .collect(Collectors.toList());
    }
    
    /**
     * 添加查询条件到QueryWrapper
     */
    private void addQueryConditionsToWrapper(QueryWrapper<TemuOrderDO> queryWrapper, TemuOrderRequestVO req) {
        if (req.getOrderStatus() != null && !req.getOrderStatus().isEmpty()) {
            try {
                Integer orderStatus = Integer.valueOf(req.getOrderStatus());
                queryWrapper.eq("order_status", orderStatus);
            } catch (NumberFormatException e) {
                log.warn("Invalid orderStatus: {}", req.getOrderStatus());
            }
        }
        
        if (req.getSku() != null && !req.getSku().isEmpty()) {
            queryWrapper.like("sku", req.getSku());
        }
        
        if (req.getSkc() != null && !req.getSkc().isEmpty()) {
            queryWrapper.like("skc", req.getSkc());
        }
        
        if (req.getCustomSku() != null && !req.getCustomSku().isEmpty()) {
            queryWrapper.like("custom_sku", req.getCustomSku());
        }
        
        if (req.getOrderNo() != null && !req.getOrderNo().isEmpty()) {
            queryWrapper.like("order_no", req.getOrderNo());
        }
        
        if (req.getCustomSkuList() != null && !req.getCustomSkuList().isEmpty()) {
            queryWrapper.in("custom_sku", req.getCustomSkuList());
        }
		if (req.getSkuList() != null && !req.getSkuList().isEmpty()) {
			queryWrapper.in("sku", req.getSkuList());
		}
        
        if (req.getShopId() != null && req.getShopId().length > 0) {
            queryWrapper.in("shop_id", Arrays.asList(req.getShopId()));
        }
        
        if (req.getBookingTime() != null && req.getBookingTime().length == 2) {
            queryWrapper.between("booking_time", req.getBookingTime()[0], req.getBookingTime()[1]);
        }
        
        if (req.getCategoryId() != null && req.getCategoryId().length > 0) {
            queryWrapper.in("category_id", Arrays.asList(req.getCategoryId()));
        }
        
        if (req.getHasCategory() != null) {
            if (req.getHasCategory() == 0) {
                queryWrapper.isNull("category_id");
            } else if (req.getHasCategory() == 1) {
                queryWrapper.isNotNull("category_id");
            }
        }
        
        if (req.getIsReturnOrder() != null) {
            queryWrapper.eq("is_return_order", req.getIsReturnOrder());
        }
    }
    
    /**
     * 应用查询条件到LambdaQueryWrapper
     */
    private void applyQueryConditions(LambdaQueryWrapperX<TemuOrderDO> queryWrapper, TemuOrderRequestVO req) {
        if (req.getOrderStatus() != null && !req.getOrderStatus().isEmpty()) {
            try {
                Integer orderStatus = Integer.valueOf(req.getOrderStatus());
                queryWrapper.eq(TemuOrderDO::getOrderStatus, orderStatus);
            } catch (NumberFormatException e) {
                log.warn("Invalid orderStatus: {}", req.getOrderStatus());
            }
        }
        
        queryWrapper.likeIfPresent(TemuOrderDO::getSkc, req.getSkc())
                   .likeIfPresent(TemuOrderDO::getCustomSku, req.getCustomSku());
        
        if (req.getCustomSkuList() != null && !req.getCustomSkuList().isEmpty()) {
            queryWrapper.in(TemuOrderDO::getCustomSku, req.getCustomSkuList());
        }
		if (req.getSkuList() != null && !req.getSkuList().isEmpty()) {
			queryWrapper.in(TemuOrderDO::getSku, req.getSkuList());
		}
        
        if (req.getShopId() != null && req.getShopId().length > 0) {
            queryWrapper.in(TemuOrderDO::getShopId, Arrays.asList(req.getShopId()));
        }
        
        if (req.getCategoryId() != null && req.getCategoryId().length > 0) {
            queryWrapper.in(TemuOrderDO::getCategoryId, Arrays.asList(req.getCategoryId()));
        }
        
        if (req.getHasCategory() != null) {
            switch (req.getHasCategory()) {
                case 0:
                    queryWrapper.isNull(TemuOrderDO::getCategoryId);
                    break;
                case 1:
                    queryWrapper.isNotNull(TemuOrderDO::getCategoryId);
                    break;
            }
        }
        
        if (req.getIsReturnOrder() != null) {
            queryWrapper.eq(TemuOrderDO::getIsReturnOrder, req.getIsReturnOrder());
        }
    }
    
    /**
     * 构建分页结果
     */
    private OrderSkuPageRespVO buildPageResult(List<OrderGroupBySortingSequenceVO> list, long total, int pageNo, int pageSize) {
        OrderSkuPageRespVO result = new OrderSkuPageRespVO();
        result.setList(list);
        result.setTotal(total);
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        return result;
    }
    
    /**
     * 构建空的分页结果
     */
    private OrderSkuPageRespVO buildEmptyPageResult(int pageNo, int pageSize) {
        OrderSkuPageRespVO result = new OrderSkuPageRespVO();
        result.setList(new ArrayList<>());
        result.setTotal(0);
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        return result;
    }
    
    /**
     * 转换为订单项VO
     */
    private OrderSkuPageItemVO convertToOrderItemVO(TemuOrderDO order) {
        OrderSkuPageItemVO itemVO = new OrderSkuPageItemVO();
        itemVO.setCustomSku(order.getCustomSku());
        itemVO.setQuantity(order.getQuantity());
        itemVO.setOriginalQuantity(order.getOriginalQuantity());
        itemVO.setCategoryId(order.getCategoryId());
        itemVO.setCategoryName(order.getCategoryName());
        itemVO.setEffectiveImgUrl(order.getEffectiveImgUrl());
        return itemVO;
    }

	private String convertToString(Object obj) {
		return obj == null ? "" : obj.toString();
	}
	
	private LocalDateTime parseDateTime(String dateTimeStr) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			return LocalDateTime.parse(dateTimeStr, formatter);
		} catch (Exception e) {
			return null;
		}
	}
	
	private void saveShopInfo(Long shopId, String shopName) {
		// 检查店铺是否已存在
		TemuShopDO existingShop = temuShopMapper.selectByShopId(shopId);
		
		if (existingShop == null) {
			// 创建新店铺记录
			TemuShopDO shop = new TemuShopDO();
			shop.setShopId(shopId);
			shop.setShopName(shopName != null ? shopName : "");
			temuShopMapper.insert(shop);
		} else if (shopName != null && !shopName.isEmpty() && !shopName.equals(existingShop.getShopName())) {
			// 更新店铺名称
			existingShop.setShopName(shopName);
			temuShopMapper.updateById(existingShop);
		}
	}
	
	@Async
	public void updateOrderMergedUrl(Long orderId, String url) {
		if (orderId == null || url == null) {
			return;
		}
		TemuOrderDO order = temuOrderMapper.selectById(orderId);
		if (order != null) {
			order.setComplianceGoodsMergedUrl(url);
			temuOrderMapper.updateById(order);
		}
	}
	
	@Async
	public void updateOrderGoodsSn(Long orderId, String url) {
		if (orderId == null || url == null) {
			return;
		}
		TemuOrderDO order = temuOrderMapper.selectById(orderId);
		if (order != null) {
			order.setGoodsSn(url);
			temuOrderMapper.updateById(order);
		}
	}

    @Async
    public void saveTempShippingInfoBatch(Long shopId, List<Map<String, Object>> ordersList) {
        if (ordersList == null || ordersList.isEmpty() || shopId == null) return;
        
        // 使用Set进行去重
        Set<String> processedOrders = new HashSet<>();
        List<Map<String, Object>> uniqueOrders = new ArrayList<>();
        
        // 去重处理
        for (Map<String, Object> orderMap : ordersList) {
            String orderNo = orderMap.get("orderId") == null ? null : orderMap.get("orderId").toString();
            if (orderNo == null || orderNo.isEmpty()) continue;
            
            // 使用shopId和orderNo组合作为唯一标识
            String uniqueKey = shopId + "_" + orderNo;
            if (processedOrders.add(uniqueKey)) {  // 如果添加成功，说明是新的组合
                uniqueOrders.add(orderMap);
            }
        }
        
        LocalDateTime createTime = LocalDate.now().atTime(0, 0, 1);
        List<TemuOrderShippingInfoDO> shippingList = new ArrayList<>();
        
        for (Map<String, Object> orderMap : uniqueOrders) {
            String orderNo = orderMap.get("orderId").toString();
            // 新增：插入前查是否已存在相同订单号+shopId
            Long count = shippingInfoMapper.selectCount(
                new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
                    .eq(TemuOrderShippingInfoDO::getOrderNo, orderNo)
                    .eq(TemuOrderShippingInfoDO::getShopId, shopId)
            );
            if (count != null && count > 0) continue;
            
            TemuOrderShippingInfoDO shipping = new TemuOrderShippingInfoDO();
            shipping.setOrderNo(orderNo);
            shipping.setShopId(shopId);
            shipping.setTrackingNumber("临时虚拟物流" + orderNo);
            shipping.setCreateTime(createTime);
            shipping.setUpdateTime(createTime);
            shipping.setExpressImageUrl("");
            shipping.setExpressOutsideImageUrl("");
            shipping.setExpressSkuImageUrl("");
            shipping.setShippingStatus(0);
            shipping.setIsUrgent(false);
            // 其它字段可不赋值
            shippingList.add(shipping);
        }
        
        if (!shippingList.isEmpty()) {
            shippingInfoMapper.insertBatch(shippingList);
        }
    }


	
	/**
	 * 验证生成的序号一致性
	 */
	private void validateSortingSequenceConsistency(List<Map<String, Object>> ordersList) {
		if (CollUtil.isEmpty(ordersList)) {
			return;
		}
		
		// 按订单编号分组检查序号一致性
		Map<String, List<Map<String, Object>>> orderNoGroupMap = ordersList.stream()
				.collect(Collectors.groupingBy(orderMap -> 
					convertToString(orderMap.get("orderId"))));
		
		for (Map.Entry<String, List<Map<String, Object>>> entry : orderNoGroupMap.entrySet()) {
			String orderNo = entry.getKey();
			List<Map<String, Object>> sameOrderNoList = entry.getValue();
			
			// 检查同一订单号下的SKU-序号映射
			Map<String, String> skuToSequenceMap = new HashMap<>();
			boolean hasInconsistency = false;
			
			for (Map<String, Object> orderMap : sameOrderNoList) {
				String sku = "";
				Map<String, Object> skusMap = (Map<String, Object>) orderMap.get("skus");
				if (skusMap != null) {
					sku = convertToString(skusMap.get("skuId"));
				}
				
				String sortingSequence = convertToString(orderMap.get("sorting_sequence"));
				
				if (StrUtil.isNotBlank(sku) && StrUtil.isNotBlank(sortingSequence)) {
					String existingSequence = skuToSequenceMap.get(sku);
					if (existingSequence != null && !existingSequence.equals(sortingSequence)) {
						log.warn("SKU {} 在同一订单 {} 中存在不同的序号: {} vs {}", 
							sku, orderNo, existingSequence, sortingSequence);
						hasInconsistency = true;
					} else {
						skuToSequenceMap.put(sku, sortingSequence);
					}
				}
			}
			
			if (hasInconsistency) {
				log.warn("订单 {} 的SKU-序号映射存在不一致: {}", orderNo, skuToSequenceMap);
			}
		}
	}
	
	/**
	 * 批量生成sorting_sequence（基于传入的ordersList数据，不查询数据库）
	 * 1. 使用订单编号后6位作为基础编号
	 * 2. 按订单编号+SKU组合进行分组
	 * 3. 相同SKU使用相同的sorting_sequence
	 * 4. 不同SKU使用不同的后缀
	 */
	private void generateSortingSequenceBatch(List<Map<String, Object>> ordersList) {
		if (CollUtil.isEmpty(ordersList)) {
			return;
		}

		// 1. 按订单编号分组
		Map<String, List<Map<String, Object>>> orderNoGroupMap = ordersList.stream()
				.collect(Collectors.groupingBy(orderMap -> 
					convertToString(orderMap.get("orderId"))));

		// 2. 为每个订单编号组生成sorting_sequence
		for (Map.Entry<String, List<Map<String, Object>>> entry : orderNoGroupMap.entrySet()) {
			String orderNo = entry.getKey();
			List<Map<String, Object>> sameOrderNoList = entry.getValue();

			if (StrUtil.isBlank(orderNo)) {
				continue;
			}

			try {
				// 3. 获取订单编号后6位作为基础编号
				String baseSequence;
				if (orderNo.length() >= 6) {
					baseSequence = orderNo.substring(orderNo.length() - 6);
				} else {
					// 如果订单编号不足6位，前面补0
					baseSequence = String.format("%06d", Integer.parseInt(orderNo));
				}

				// 4. 按SKU分组，相同SKU使用相同的sorting_sequence
				Map<String, List<Map<String, Object>>> skuGroupMap = sameOrderNoList.stream()
						.collect(Collectors.groupingBy(orderMap -> {
							Map<String, Object> skusMap = (Map<String, Object>) orderMap.get("skus");
							if (skusMap != null) {
								return convertToString(skusMap.get("skuId"));
							}
							return "";
						}));

				// 5. 为每个SKU组分配sorting_sequence（使用稳定的SKU-序号映射）
				List<String> allSkus = new ArrayList<>(skuGroupMap.keySet());
				
				// 使用SKU的哈希值来确定序号顺序，确保同一SKU总是获得相同的序号
				// 这样即使SKU列表顺序变化，每个SKU的序号也会保持一致
				allSkus.sort((sku1, sku2) -> {
					int hash1 = Math.abs(sku1.hashCode());
					int hash2 = Math.abs(sku2.hashCode());
					if (hash1 != hash2) {
						return Integer.compare(hash1, hash2);
					}
					// 如果哈希值相同，按SKU字符串排序
					return sku1.compareTo(sku2);
				});

				for (int i = 0; i < allSkus.size(); i++) {
					String sku = allSkus.get(i);
					List<Map<String, Object>> sameSkuList = skuGroupMap.get(sku);
					
					// 生成sorting_sequence
					String sortingSequence;
					if (i == 0) {
						// 第一个SKU，直接使用基础编号（保持前导零）
						sortingSequence = baseSequence;
					} else {
						// 其他SKU，添加后缀
						sortingSequence = baseSequence + "_" + String.format("%02d", i + 1);
					}

					// 为所有相同SKU的订单设置相同的sorting_sequence
					sameSkuList.forEach(orderMap -> orderMap.put("sorting_sequence", sortingSequence));
				}

			} catch (NumberFormatException e) {
				log.warn("订单编号格式错误，无法生成sorting_sequence: orderNo={}", orderNo);
			} catch (Exception e) {
				log.error("生成sorting_sequence时发生异常: orderNo={}", orderNo, e);
			}
		}
	}

    @Override
    public Boolean toggleIsFoundAll(Long orderId, Integer isFoundAll) {
        TemuOrderDO order = temuOrderMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        if (isFoundAll != null) {
            order.setIsFoundAll(isFoundAll);
        } else {
            Integer current = order.getIsFoundAll();
            int next = (current != null && current == 1) ? 0 : 1;
            order.setIsFoundAll(next);
        }
        return temuOrderMapper.updateById(order) > 0;
    }

    @Override
    public Boolean batchUpdateSenderIdBySortingSequence(List<String> sortingSequenceList, Long senderId, Boolean conditionFlag) {
        if (sortingSequenceList == null || sortingSequenceList.isEmpty() || senderId == null) {
            return false;
        }
        // 查找所有匹配的订单
        List<TemuOrderDO> orders = temuOrderMapper.selectList(
                new LambdaQueryWrapperX<TemuOrderDO>()
                        .in(TemuOrderDO::getSortingSequence, sortingSequenceList)
        );
        if (orders.isEmpty()) {
            return false;
        }
        boolean updated = false;
        for (TemuOrderDO order : orders) {
            // conditionFlag=true: 强制覆盖；false: 只更新sender_id为空的
            if (Boolean.TRUE.equals(conditionFlag) || order.getSenderId() == null) {
                TemuOrderDO update = new TemuOrderDO();
                update.setId(order.getId());
                update.setSenderId(senderId);
                temuOrderMapper.updateById(update);
                updated = true;
            }
        }
        return updated;
    }

	@Override
	@Transactional
	public int VipBatchSaveOrder(List<TemuOrderBatchOrderReqVO> requestVO) {
		// 1. 获取所有订单ID
		List<Long> orderIds = requestVO.stream()
				.map(TemuOrderBatchOrderReqVO::getId)
				.collect(Collectors.toList());

		// 2. 批量查询订单，只获取id和categoryId字段
		List<TemuOrderDO> orders = temuOrderMapper.selectBatchIds(orderIds);

		// 3. 使用Map存储categoryId和对应的订单id列表
		Map<String, List<Long>> categoryOrderMap = orders.stream()
				.collect(Collectors.groupingBy(
						TemuOrderDO::getCategoryId,
						Collectors.mapping(TemuOrderDO::getId, Collectors.toList())));

		ArrayList<TemuOrderDO> temuOrderDOList = new ArrayList<>();
		int processCount = 0;
		LoginUser loginUser = getLoginUser();
		String operator = SecurityFrameworkUtils.getLoginUserNickname();
		Long operatorId = loginUser != null ? loginUser.getId() : null;
		LocalDateTime now = LocalDateTime.now();
		for (TemuOrderBatchOrderReqVO temuOrderBatchOrderReqVO : requestVO) {
			// 检查订单是否存在
			TemuOrderDO temuOrderDO = temuOrderMapper.selectById(temuOrderBatchOrderReqVO.getId());
			if (temuOrderDO == null) {
				throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
			}
			// 根据订单的关联分类id查询分类信息
			TemuProductCategoryDO temuProductCategoryDO = temuProductCategoryMapper
					.selectById(temuOrderDO.getCategoryId());
			if (temuProductCategoryDO == null) {
				throw exception(ErrorCodeConstants.CATEGORY_NOT_EXISTS);
			}
			// 检查订单状态
			if (temuOrderDO.getOrderStatus() != TemuOrderStatusEnum.UNDELIVERED) {
				throw exception(ErrorCodeConstants.ORDER_STATUS_ERROR);
			}
			// 更新数量
			temuOrderDO.setQuantity(temuOrderBatchOrderReqVO.getQuantity());

			// 判断是否为返单，返单直接设置价格为0，不进行价格计算
			if (temuOrderDO.getIsReturnOrder() != null && temuOrderDO.getIsReturnOrder() == 1) {
				// 返单订单：单价和总价都设为0
				temuOrderDO.setUnitPrice(BigDecimal.ZERO);
				temuOrderDO.setTotalPrice(BigDecimal.ZERO);
				log.info("返单订单 {} 价格设置为0，不进行扣费", temuOrderDO.getOrderNo());
			} else {
				// 非返单订单：正常计算价格
				BigDecimal unitPrice;
				IPriceRule rule;
				String discountStr = configApi.getConfigValueByKey("temu.order.discount");
				BigDecimal discount = new BigDecimal(discountStr); // 例如 0.6

				// 根据规则类型加载不同的对象
				rule = PriceRuleFactory.createPriceRule(temuProductCategoryDO.getRuleType(),
						temuProductCategoryDO.getUnitPrice());
				unitPrice = rule.calcUnitPrice(temuOrderBatchOrderReqVO.getQuantity());
				temuOrderDO.setUnitPrice(unitPrice);
				temuOrderDO
						.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(temuOrderBatchOrderReqVO.getQuantity())));
				// 查询是否存在VIP特殊类目
				TemuVipProductCategoryDO temuVipProductCategoryDO = temuVipProductCategoryMapper
						.selectByCategoryId(Long.valueOf(temuOrderDO.getCategoryId()));

				// 声明VIP价格变量
				BigDecimal vipTotalPrice;
				BigDecimal vipUnitPrice;

				if (temuVipProductCategoryDO != null) {
					// 存在特殊类目，使用特殊类目的价格规则
					IPriceRule vipRule = PriceRuleFactory.createPriceRule(temuVipProductCategoryDO.getRuleType(),
							temuVipProductCategoryDO.getUnitPrice());
					vipUnitPrice = vipRule.calcUnitPrice(temuOrderBatchOrderReqVO.getQuantity());

					// 计算VIP总价
					vipTotalPrice = vipUnitPrice.multiply(BigDecimal.valueOf(temuOrderBatchOrderReqVO.getQuantity()));
					temuOrderDO.setVipunitPrice(vipUnitPrice);
				} else {
					// 计算VIP总价
					vipTotalPrice = unitPrice
							.multiply(BigDecimal.valueOf(temuOrderBatchOrderReqVO.getQuantity()))
							.multiply(discount);
					temuOrderDO.setVipunitPrice(unitPrice);
				}

				// 设置VIP价格信息
				temuOrderDO.setViptotalPrice(vipTotalPrice);
			}

			// 修改订单状态
			temuOrderDO.setOrderStatus(TemuOrderStatusEnum.ORDERED);
			temuOrderDOList.add(temuOrderDO);
			processCount++;

			// 插入下单记录表
			TemuVipOrderPlacementRecordDO record = new TemuVipOrderPlacementRecordDO();
			record.setOrderNo(temuOrderDO.getOrderNo());
			record.setShopId(temuOrderDO.getShopId());
			TemuShopDO shop = temuShopMapper.selectByShopId(temuOrderDO.getShopId());
			record.setShopName(shop != null ? shop.getShopName() : null);
			record.setProductTitle(temuOrderDO.getProductTitle());
			record.setProductProperties(temuOrderDO.getProductProperties());
			if (temuOrderDO.getCategoryId() != null) {
				try {
					record.setCategoryId(Long.valueOf(temuOrderDO.getCategoryId()));
				} catch (Exception ignore) {
				}
			}
			record.setCategoryName(temuOrderDO.getCategoryName());
			record.setOriginalQuantity(temuOrderDO.getOriginalQuantity());
			record.setQuantity(temuOrderDO.getQuantity());
			record.setUnitPrice(temuOrderDO.getVipunitPrice());
			record.setTotalPrice(temuOrderDO.getViptotalPrice());
			record.setSku(temuOrderDO.getSku());
			record.setSkc(temuOrderDO.getSkc());
			record.setCustomSku(temuOrderDO.getCustomSku());
			record.setOperationTime(now);
			record.setIsReturnOrder(temuOrderDO.getIsReturnOrder() != null && temuOrderDO.getIsReturnOrder() == 1);
			record.setOperatorId(operatorId);
			record.setOperator(operator);
			temuVipOrderPlacementRecordMapper.insert(record);
		}
		// 批量更新订单
		temuOrderMapper.updateBatch(temuOrderDOList);
		// 批量支付订单
		payOrderBatch(temuOrderDOList);

		// 检查当前订单是否允许自动打批次
		DictTypeDO dictTypeDO = dictTypeMapper.selectByType("temu_order_batch_category_status");
		// 如果状态开启那么开始自动打批次
		if (dictTypeDO.getStatus() == 0) {
			// 获取batchCategoryId和订单id的映射
			Map<String, List<Long>> batchCategoryOrderMap = temuOrderBatchCategoryService
					.getBatchCategoryOrderMap(categoryOrderMap);
			temuOrderBatchCategoryService.processBatchAndRelations(batchCategoryOrderMap);
		}

		return processCount;
	}
}
