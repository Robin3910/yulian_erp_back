package cn.iocoder.yudao.module.temu.service.order.impl;

import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.client.TemuOrderRequestVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopMapper;
import cn.iocoder.yudao.module.temu.service.order.ITemuOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TemuOrderService implements ITemuOrderService {
	@Resource
	private TemuOrderMapper temuOrderMapper;
	@Resource 
	private TemuShopMapper temuShopMapper;

	@Override
	public PageResult<TemuOrderDO> list(TemuOrderRequestVO temuOrderRequestVO) {
		return temuOrderMapper.selectPage(temuOrderRequestVO);
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
				
				// 设置SKU相关信息
				order.setSkc(convertToString(orderMap.get("skc")));
				
				Map<String, Object> skusMap = (Map<String, Object>) orderMap.get("skus");
				if (skusMap != null) {
					order.setSku(convertToString(skusMap.get("skuId")));
					order.setCustomSku(convertToString(skusMap.get("customSku")));
					order.setProductProperties(convertToString(skusMap.get("property")));
				}
				
				// 设置价格和数量
				order.setSalePrice(new BigDecimal(convertToString(orderMap.get("price"))));
				order.setQuantity(Integer.valueOf(convertToString(orderMap.get("quantity"))));
				
				// 设置订单状态
				String status = convertToString(orderMap.get("status"));
				if ("待发货".equals(status)) {
					order.setOrderStatus(1); // 使用Integer而非byte
				} else {
					order.setOrderStatus(0); // 使用Integer而非byte
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
					// 更新现有订单
					order.setId(existingOrder.getId());
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
