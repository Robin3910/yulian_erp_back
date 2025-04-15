package cn.iocoder.yudao.module.temu.service.order.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.ServerException;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.TemuOrderBatchOrderReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.TemuOrderRequestVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.TemuOrderUpdateCategoryReqVo;
import cn.iocoder.yudao.module.temu.dal.dataobject.*;
import cn.iocoder.yudao.module.temu.dal.mysql.*;
import cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.temu.enums.TemuOrderStatusEnum;
import cn.iocoder.yudao.module.temu.service.order.ITemuOrderService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuUserShopDO;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
	TemuProductCategoryMapper temuProductCategoryMapper;
	@Resource
	private TemuProductCategorySkuMapper temuProductCategorySkuMapper;
	
	
	@Override
	public PageResult<TemuOrderDetailDO> list(TemuOrderRequestVO temuOrderRequestVO) {
		return temuOrderMapper.selectPage(temuOrderRequestVO);
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
		Boolean result = temuOrderMapper.updateBatch(requestVO);
		LogRecordContext.putVariable("user", SecurityFrameworkUtils.getLoginUser());
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
		
		for (Map<String, Object> orderMap : ordersList) {
			try {
				TemuOrderDO order = new TemuOrderDO();
				
				// 设置基本信息
				order.setShopId(shopIdLong);
				order.setOrderNo(convertToString(orderMap.get("orderId")));
				order.setProductTitle(convertToString(orderMap.get("title")));
				order.setProductImgUrl(convertToString(orderMap.get("product_img_url")));
				order.setEffectiveImgUrl(convertToString(orderMap.get("effective_image_url"))); // 写入合成预览图url信息
				
				// 设置SKU相关信息
				order.setSkc(convertToString(orderMap.get("skc")));
				
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
				
//				// 如果没有SKU信息，查询历史订单
//				if (sku.isEmpty() && !properties.isEmpty()) {
//					LambdaQueryWrapper<TemuOrderDO> queryWrapper = new LambdaQueryWrapper<>();
//					queryWrapper.eq(TemuOrderDO::getShopId, shopIdLong)
//							.eq(TemuOrderDO::getProductProperties, properties)
//							.last("LIMIT 1"); // 只取一条记录
//
//					TemuOrderDO historicalOrder = temuOrderMapper.selectOne(queryWrapper);
//					if (historicalOrder != null && historicalOrder.getSku() != null) {
//						// 将历史订单的SKU信息赋值给当前订单
//						order.setSku(historicalOrder.getSku());
//						sku = historicalOrder.getSku(); // 更新sku变量，用于后续的分类查询
//					}
//				}
				
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
				
				// 设置价格和数量
				order.setSalePrice(new BigDecimal(convertToString(orderMap.get("price"))));
				order.setQuantity(Integer.valueOf(convertToString(orderMap.get("quantity"))));
				
				// 设置订单状态
				// todo 前端上传上来使用枚举值，不要使用string
				String status = convertToString(orderMap.get("status"));
				if ("待发货".equals(status)) {
					order.setOrderStatus(0);
				} else {
					order.setOrderStatus(1);
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
				if (existingOrder != null) {
					// 更新现有订单，只更新非空字段
					order.setId(existingOrder.getId());
					// 订单的状态不能随意更新，保持原有状态
					order.setOrderStatus(existingOrder.getOrderStatus());
					
					// 比对并填充字段
					if (order.getOrderNo() == null) order.setOrderNo(existingOrder.getOrderNo());
					if (order.getProductTitle() == null) order.setProductTitle(existingOrder.getProductTitle());
					if (order.getSku() == null) order.setSku(existingOrder.getSku());
					if (order.getSkc() == null) order.setSkc(existingOrder.getSkc());
					if (order.getSalePrice() == null) order.setSalePrice(existingOrder.getSalePrice());
					if (order.getCustomSku() == null) order.setCustomSku(existingOrder.getCustomSku());
					if (order.getQuantity() == null) order.setQuantity(existingOrder.getQuantity());
					if (order.getProductProperties() == null) order.setProductProperties(existingOrder.getProductProperties());
					if (order.getBookingTime() == null) order.setBookingTime(existingOrder.getBookingTime());
					if (order.getShopId() == null) order.setShopId(existingOrder.getShopId());
					if (order.getCustomImageUrls() == null) order.setCustomImageUrls(existingOrder.getCustomImageUrls());
					if (order.getCustomTextList() == null) order.setCustomTextList(existingOrder.getCustomTextList());
					if (order.getProductImgUrl() == null) order.setProductImgUrl(existingOrder.getProductImgUrl());
					if (order.getCategoryId() == null) order.setCategoryId(existingOrder.getCategoryId());
					if (order.getCategoryName() == null) order.setCategoryName(existingOrder.getCategoryName());
					if (order.getShippingInfo() == null) order.setShippingInfo(existingOrder.getShippingInfo());
					if (order.getOriginalInfo() == null) order.setOriginalInfo(existingOrder.getOriginalInfo());
					if (order.getEffectiveImgUrl() == null) order.setEffectiveImgUrl(existingOrder.getEffectiveImgUrl());
					if (order.getUnitPrice() == null) order.setUnitPrice(existingOrder.getUnitPrice());
					if (order.getTotalPrice() == null) order.setTotalPrice(existingOrder.getTotalPrice());
					
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
		//根据查询订单是否存在
		TemuOrderDO temuOrderDO = temuOrderMapper.selectById(requestVO.getId());
		if (temuOrderDO == null) {
			throw new ServerException(ErrorCodeConstants.ORDER_NOT_EXISTS);
		}
		//检查分类id是否存在
		List<TemuProductCategoryDO> list = temuProductCategoryMapper.selectByMap(MapUtil.of("category_id", requestVO.getCategoryId()));
		if (list == null || list.isEmpty()) {
			throw new ServerException(ErrorCodeConstants.CATEGORY_NOT_EXISTS);
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
		return temuOrderMapper.updateById(BeanUtils.toBean(requestVO, TemuOrderDO.class));
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
		int processCount = 0;
		for (TemuOrderBatchOrderReqVO temuOrderBatchOrderReqVO : requestVO) {
			//检查订单是否存在
			TemuOrderDO temuOrderDO = temuOrderMapper.selectById(temuOrderBatchOrderReqVO.getId());
			if (temuOrderDO == null) {
				throw ServiceExceptionUtil.exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
			}
			//根据订单的关联分类id查询分类信息
			TemuProductCategoryDO temuProductCategoryDO = temuProductCategoryMapper.selectById(temuOrderDO.getCategoryId());
			if (temuProductCategoryDO == null) {
				throw ServiceExceptionUtil.exception(ErrorCodeConstants.CATEGORY_NOT_EXISTS);
			}
			//检查订单状态
			if (temuOrderDO.getOrderStatus() != TemuOrderStatusEnum.UNDELIVERED) {
				throw ServiceExceptionUtil.exception(ErrorCodeConstants.ORDER_STATUS_ERROR);
			}
			//检查是否存在价格规则
			if (temuProductCategoryDO.getUnitPrice() == null || temuProductCategoryDO.getUnitPrice().isEmpty()) {
				throw ServiceExceptionUtil.exception(ErrorCodeConstants.CATEGORY_PRICE_NOT_EXISTS);
			}
			//获取分类价格按照max 字段进行排序
			List<TemuProductCategoryDO.UnitPrice> unitPriceList = temuProductCategoryDO.getUnitPrice();
			unitPriceList.sort(Comparator.comparingInt(TemuProductCategoryDO.UnitPrice::getMax));
			//设置默认价格
			BigDecimal unitPrice = temuProductCategoryDO.getDefaultPrice();
			
			//匹配价格
			for (TemuProductCategoryDO.UnitPrice price : unitPriceList) {
				if (temuOrderBatchOrderReqVO.getQuantity() <= price.getMax()) {
					unitPrice = price.getPrice();
					break;
				}
			}
			//更新数量
			temuOrderDO.setQuantity(temuOrderBatchOrderReqVO.getQuantity());
			//记录默认价格
			temuOrderDO.setDefaultPrice(temuProductCategoryDO.getDefaultPrice());
			//更新订单
			temuOrderDO.setUnitPrice(unitPrice);
			//更新总价
			temuOrderDO.setTotalPrice(unitPrice.multiply(new BigDecimal(temuOrderBatchOrderReqVO.getQuantity())));
			//记录应用规则
			ArrayList<TemuOrderDO.UnitPrice> arrayList = new ArrayList<>();
			unitPriceList.iterator().forEachRemaining(categoryUnitPrice -> {
				TemuOrderDO.UnitPrice orderPriceRule = new TemuOrderDO.UnitPrice();
				orderPriceRule.setMax(categoryUnitPrice.getMax());
				orderPriceRule.setPrice(categoryUnitPrice.getPrice());
				arrayList.add(orderPriceRule);
			});
			temuOrderDO.setPriceRule(arrayList);
			//修改订单状态
			temuOrderDO.setOrderStatus(TemuOrderStatusEnum.ORDERED);
			//更新订单信息
			temuOrderMapper.updateById(temuOrderDO);
			processCount++;
		}
		return processCount;
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
}
