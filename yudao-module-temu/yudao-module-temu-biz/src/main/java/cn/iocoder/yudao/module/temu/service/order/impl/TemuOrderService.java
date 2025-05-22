package cn.iocoder.yudao.module.temu.service.order.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.ServerException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.pay.dal.dataobject.wallet.PayWalletDO;
import cn.iocoder.yudao.module.pay.enums.wallet.PayWalletBizTypeEnum;
import cn.iocoder.yudao.module.pay.service.wallet.PayWalletService;
import cn.iocoder.yudao.module.system.dal.dataobject.dict.DictTypeDO;
import cn.iocoder.yudao.module.system.dal.mysql.dict.DictTypeMapper;
import cn.iocoder.yudao.module.system.enums.DictTypeConstants;
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
import static cn.iocoder.yudao.framework.common.enums.UserTypeEnum.MEMBER;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser;
import static cn.iocoder.yudao.module.pay.enums.ErrorCodeConstants.WALLET_NOT_FOUND;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;

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
	@LogRecord(success = "id：{{#user.id}} 更新了{{#orderSize}}条数据,提交的数据是{{#orderString}}", type = "TEMU订单操作", bizNo = "{{#user.id}}")
	public Boolean beatchUpdateStatus(List<TemuOrderDO> requestVO) {
		for (TemuOrderDO temuOrderDO : requestVO) {
			if (TemuOrderStatusEnum.UNDELIVERED == temuOrderDO.getOrderStatus()) {
				// 先查询原始订单数据，确保有正确的originalQuantity值
				TemuOrderDO originalOrder = temuOrderMapper.selectById(temuOrderDO.getId());
				if (originalOrder != null && originalOrder.getOriginalQuantity() != null) {
					// 将制作数量重置为与官网一致的原始数量
					temuOrderDO.setQuantity(originalOrder.getOriginalQuantity());
				}
			}
		}
		Boolean result = temuOrderMapper.updateBatch(requestVO);
		LogRecordContext.putVariable("user", getLoginUser());
		LogRecordContext.putVariable("orderSize", requestVO.size());
		HashMap<String, String> stringStringHashMap = new HashMap<>();
		requestVO.iterator().forEachRemaining(temuOrderDO -> {
			stringStringHashMap.put("id", String.valueOf(temuOrderDO.getId()));
			stringStringHashMap.put("orderStatus", String.valueOf(temuOrderDO.getOrderStatus()));
		});
		LogRecordContext.putVariable("orderString", JsonUtils.toJsonString(stringStringHashMap));
		return result;
	}

	@Override
	public int saveOrders(String shopId, String shopName, List<Map<String, Object>> ordersList, String originalJson) {
		if (ordersList == null || ordersList.isEmpty()) {
			return 0;
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

		for (Map<String, Object> orderMap : ordersList) {
			try {
				TemuOrderDO order = new TemuOrderDO();

				// 设置基本信息
				order.setShopId(shopIdLong);
				order.setOrderNo(convertToString(orderMap.get("orderId")));
				order.setProductTitle(convertToString(orderMap.get("title")));
				order.setProductImgUrl(convertToString(orderMap.get("product_img_url")));
				order.setEffectiveImgUrl(convertToString(orderMap.get("effective_image_url"))); // 写入合成预览图url信息

				// 设置商品条形码图片URL到goods_sn字段
				order.setGoodsSn(convertToString(orderMap.get("barcode_image_url")));

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
						}
					});
				} else if (StrUtil.isAllNotBlank(complianceUrl, goodsSnUrl)) {
					// 如果没有customSku但有两个PDF，直接合并
					CompletableFuture<String> mergedUrlFuture = asyncPdfService.processPdfAsync(
							complianceUrl,
							goodsSnUrl,
							temuOssService);

					// 设置回调更新订单
					mergedUrlFuture.thenAccept(url -> {
						if (url != null) {
							// 更新订单合并后的PDF地址（如合规+条码组合文件）
							updateOrderMergedUrl(order.getId(), url);

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

				// 检查订单是否已存在
				TemuOrderDO existingOrder = temuOrderMapper.selectByCustomSku(order.getCustomSku());
				// bookingTime 要识别一下，bookingTime不一样的话，就插入新的数据
				// 1、不是返单，只是更新 => 拿到数据正常更新
				// 2、bookingTime不一样，属于返单 => 需要删除batch_relation表中的记录，并将status置为0，往企业微信发消息
				if (existingOrder != null) {
					// 更新现有订单，只更新非空字段
					order.setId(existingOrder.getId());
					// 订单的状态不能随意更新，保持原有状态
					order.setOrderStatus(existingOrder.getOrderStatus());

					// 比对并填充字段
					if (!StringUtils.hasText(order.getOrderNo()))
						order.setOrderNo(existingOrder.getOrderNo());
					if (!StringUtils.hasText(order.getProductTitle()))
						order.setProductTitle(existingOrder.getProductTitle());
					if (!StringUtils.hasText(order.getSku()))
						order.setSku(existingOrder.getSku());
					if (!StringUtils.hasText(order.getSkc()))
						order.setSkc(existingOrder.getSkc());
					if (order.getSalePrice() == null)
						order.setSalePrice(existingOrder.getSalePrice());
					if (!StringUtils.hasText(order.getCustomSku()))
						order.setCustomSku(existingOrder.getCustomSku());
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

					// 如果bookingTime不一样，说明是返单
					if (order.getBookingTime() != null
							&& !order.getBookingTime().equals(existingOrder.getBookingTime())) {
						// 删除关联关系
						temuOrderBatchRelationMapper.deleteByOrderId(existingOrder.getId());
						// 将状态置为0
						order.setOrderStatus(0);
						// 发送企业微信消息
						String message = String.format("订单：%s 定制SKU：%s 发生返单，原预约时间: %s, 新预约时间: %s",
								order.getOrderNo(),
								order.getCustomSku(),
								DateUtil.format(existingOrder.getBookingTime(), "yyyy-MM-dd HH:mm:ss"),
								DateUtil.format(order.getBookingTime(), "yyyy-MM-dd HH:mm:ss"));
						// 获取shopId为88888888的店铺webhook地址
						TemuShopDO shop = temuShopMapper.selectByShopId(88888888L);
						if (shop != null && StringUtils.hasText(shop.getWebhook())) {
							weiXinProducer.sendMessage(shop.getWebhook(), message.toString());
						}
					}
					// 如果bookingTime为空，则使用existingOrder的bookingTime,这种是上传定制图片的场景
					if (order.getBookingTime() == null) {
						order.setBookingTime(existingOrder.getBookingTime());
					}
					if (order.getShopId() == null)
						order.setShopId(existingOrder.getShopId());
					if (!StringUtils.hasText(order.getCustomImageUrls()))
						order.setCustomImageUrls(existingOrder.getCustomImageUrls());
					if (!StringUtils.hasText(order.getCustomTextList()))
						order.setCustomTextList(existingOrder.getCustomTextList());
					if (!StringUtils.hasText(order.getProductImgUrl()))
						order.setProductImgUrl(existingOrder.getProductImgUrl());
					if (!StringUtils.hasText(order.getCategoryId()))
						order.setCategoryId(existingOrder.getCategoryId());
					if (!StringUtils.hasText(order.getCategoryName()))
						order.setCategoryName(existingOrder.getCategoryName());
					if (!StringUtils.hasText(order.getShippingInfo()))
						order.setShippingInfo(existingOrder.getShippingInfo());
					if (!StringUtils.hasText(order.getOriginalInfo()))
						order.setOriginalInfo(existingOrder.getOriginalInfo());
					if (!StringUtils.hasText(order.getEffectiveImgUrl()))
						order.setEffectiveImgUrl(existingOrder.getEffectiveImgUrl());
					if (order.getUnitPrice() == null)
						order.setUnitPrice(existingOrder.getUnitPrice());
					if (order.getTotalPrice() == null)
						order.setTotalPrice(existingOrder.getTotalPrice());
					if (order.getGoodsSn() == null)
						order.setGoodsSn(existingOrder.getGoodsSn());
					if (order.getComplianceUrl() == null)
						order.setComplianceUrl(existingOrder.getComplianceUrl());
					if (order.getComplianceImageUrl() == null)
						order.setComplianceImageUrl(existingOrder.getComplianceImageUrl());

					temuOrderMapper.updateById(order);
				} else {
					// 插入新订单
					temuOrderMapper.insert(order);
				}
				count++;

			} catch (Exception e) {
				log.error("保存订单失败: {}", e.getMessage(), e);
				// 继续处理下一个订单
			}
		}

		// 同时保存或更新店铺信息
		saveShopInfo(shopIdLong, shopName);

		return count;
	}

	@Override
	public int updateCategory(TemuOrderUpdateCategoryReqVo requestVO) {
		// 根据查询订单是否存在
		TemuOrderDO temuOrderDO = temuOrderMapper.selectById(requestVO.getId());
		if (temuOrderDO == null) {
			throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
		}
		// 检查分类id是否存在
		List<TemuProductCategoryDO> list = temuProductCategoryMapper
				.selectByMap(MapUtil.of("category_id", requestVO.getCategoryId()));
		if (list == null || list.isEmpty()) {
			throw exception(ErrorCodeConstants.CATEGORY_NOT_EXISTS);
		}
		// 检查temu_product_category_sku表是否选择记录
		HashMap<String, Object> map = MapUtil.of("sku", temuOrderDO.getSku());
		map.put("shop_id", temuOrderDO.getShopId());
		List<TemuProductCategorySkuDO> temuProductCategorySkuDOList = temuProductCategorySkuMapper.selectByMap(map);
		// 如果存在记录 更新分类id
		if (temuProductCategorySkuDOList != null && !temuProductCategorySkuDOList.isEmpty()) {
			// 更新类目id
			temuProductCategorySkuDOList.forEach(temuProductCategorySkuDO -> {
				temuProductCategorySkuDO.setCategoryId(Long.parseLong(requestVO.getCategoryId()));
				temuProductCategorySkuDO.setCategoryName(list.get(0).getCategoryName());
				temuProductCategorySkuMapper.updateById(temuProductCategorySkuDO);
			});
		} else {
			// 插入新数据
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
			// 根据分类规则加载不同的对象
			BigDecimal unitPrice;
			IPriceRule rule;
			// 根据规则类型加载不同的对象
			rule = PriceRuleFactory.createPriceRule(temuProductCategoryDO.getRuleType(),
					temuProductCategoryDO.getUnitPrice());
			unitPrice = rule.calcUnitPrice(temuOrderBatchOrderReqVO.getQuantity());
			// 更新数量
			temuOrderDO.setQuantity(temuOrderBatchOrderReqVO.getQuantity());
			// 更新单价
			temuOrderDO.setUnitPrice(unitPrice);
			// 更新总价
			temuOrderDO.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(temuOrderBatchOrderReqVO.getQuantity())));
			// 设置订单规则
			temuOrderDO.setPriceRule(rule);
			// 修改订单状态
			temuOrderDO.setOrderStatus(TemuOrderStatusEnum.ORDERED);
			//// 更新订单信息
			// temuOrderMapper.updateById(temuOrderDO);
			temuOrderDOList.add(temuOrderDO);
			processCount++;
		}
		// 批量更新订单
		temuOrderMapper.updateBatch(temuOrderDOList);
		// 批量支付订单
		payOrderBatch(temuOrderDOList);

		// 获取batchCategoryId和订单id的映射
		Map<String, List<Long>> batchCategoryOrderMap = temuOrderBatchCategoryService
				.getBatchCategoryOrderMap(categoryOrderMap);
		temuOrderBatchCategoryService.processBatchAndRelations(batchCategoryOrderMap);
		return processCount;
	}

	private void payOrderBatch(ArrayList<TemuOrderDO> temuOrderDOList) {
		// 检查当前订单是否允许被支付
		DictTypeDO dictTypeDO = dictTypeMapper.selectByType("temu_order_batch_pay_order_status");
		// 如果状态开启那么开始处理支付
		if (dictTypeDO.getStatus() == 0) {
			// 统计订单总金额
			BigDecimal totalPrice = temuOrderDOList.stream().map(TemuOrderDO::getTotalPrice).reduce(BigDecimal::add)
					.orElse(BigDecimal.ZERO);
			// 检查当前用户是否存在余额
			LoginUser loginUser = getLoginUser();
			if (loginUser == null) {
				throw exception(USER_NOT_EXISTS);
			}
			// 获得用户钱包
			PayWalletDO wallet = payWalletService.getOrCreateWallet(loginUser.getId(), ADMIN.getValue());
			if (wallet == null) {
				throw exception(WALLET_NOT_FOUND);
			}
			// 检查用户余额是否充足
			if (new BigDecimal(wallet.getBalance() / 100).compareTo(totalPrice) < 0) {
				throw exception(ErrorCodeConstants.WALLET_NOT_ENOUGH);
			}
			temuOrderDOList.forEach(temuOrderDO -> {
				payWalletService.reduceWalletBalance(wallet.getId(), temuOrderDO.getId(),
						PayWalletBizTypeEnum.PAYMENT_TEMU_ORDER,
						temuOrderDO.getTotalPrice().multiply(new BigDecimal(100)).intValue());
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
		TemuProductCategoryDO temuProductCategoryDO = temuProductCategoryMapper
				.selectByCategoryId(Long.valueOf(temuOrderDO.getCategoryId()));
		if (temuProductCategoryDO == null) {
			throw exception(ErrorCodeConstants.CATEGORY_NOT_EXISTS);
		}
		// 获取店铺信息
		TemuShopDO temuShopDO = temuShopMapper.selectByShopId(temuOrderDO.getShopId());
		if (temuShopDO == null) {
			throw exception(ErrorCodeConstants.SHOP_NOT_EXISTS);
		}
		// 根据分类类型匹配合规单
		Map<String, Object> oldTypeUrl = temuShopDO.getOldTypeUrl();

		return new TemuOrderExtraInfoRespVO(temuOrderDO.getGoodsSn(),
				oldTypeUrl != null ? convertToString(oldTypeUrl.get(temuProductCategoryDO.getOldType())) : "");
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

}
