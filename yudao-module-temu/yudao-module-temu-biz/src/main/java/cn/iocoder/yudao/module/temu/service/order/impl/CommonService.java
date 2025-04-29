package cn.iocoder.yudao.module.temu.service.order.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.api.openapi.dto.OrderInfoDTO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuProductCategoryMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopMapper;
import cn.iocoder.yudao.module.temu.enums.TemuOrderStatusEnum;
import cn.iocoder.yudao.module.temu.enums.openapi.OrderInfoEnum;
import cn.iocoder.yudao.module.temu.mq.message.weixin.WeiXinNotifyMessage;
import cn.iocoder.yudao.module.temu.mq.producer.weixin.WeiXinProducer;
import cn.iocoder.yudao.module.temu.service.order.ICommonService;

import cn.iocoder.yudao.module.temu.utils.openapi.TemuOpenApiBuilder;
import cn.iocoder.yudao.module.temu.utils.openapi.TemuOpenApiUtil;
import cn.iocoder.yudao.module.temu.utils.weixin.WeiXinWebHookNotifyUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommonService implements ICommonService {
	
	@Resource
	private TemuProductCategoryMapper temuProductCategoryMapper;
	@Resource
	private TemuShopMapper temuShopMapper;
	@Resource
	private TemuOpenApiBuilder temuOpenApiBuilder;
	
	@Resource
	WeiXinProducer weiXinProducer;
	@Autowired
	private TemuOrderMapper temuOrderMapper;
	
	@Override
	public PageResult<TemuProductCategoryDO> list() {
		return temuProductCategoryMapper.selectPage();
	}
	
	@Override
	public PageResult<TemuShopDO> listShop() {
		return temuShopMapper.selectPage();
	}
	
	@Override
	public PageResult<TemuShopDO> listShop(Long loginUserId) {
		return temuShopMapper.selectPage(loginUserId);
	}
	/**
	 * 测试对接TEMU开放平台API的功能，处理订单数据同步与状态更新逻辑。
	 *
	 * <p>此方法通过访问令牌构建TEMU API客户端，拉取全量订单列表后，
	 * 根据定制SKU信息与本地数据库进行比对，执行订单状态更新或新建操作。</p>
	 *
	 * @return 处理后的首个子订单对象，若前置校验失败或无订单数据则返回null
	 */
	@Override
	@Transactional
	public Object testTemuOpenApi() {
		TemuShopDO temuShopDO = temuShopMapper.selectById(25);
		if (temuShopDO == null) return null;
		log.info("temuShopDO{}", temuShopDO);
		TreeMap<String, Object> map = new TreeMap<>();
		TemuOpenApiUtil builder = temuOpenApiBuilder.builder(temuShopDO.getAccessToken());
		log.info("builder对象{}", builder);
		List<OrderInfoDTO.SubOrderForSupplier> fullOrderList = builder.getFullOrderList(map);
		if (fullOrderList == null || fullOrderList.isEmpty()) {
			return null;
		}
		
		//根据定制sku信息查询所有订单列表
		List<TemuOrderDO> selectListByCustomSku = temuOrderMapper.selectListByCustomSku(fullOrderList.stream()
				.filter(order -> order.getSkuQuantityDetailList() != null && !order.getSkuQuantityDetailList().isEmpty())
				.flatMap(order -> order.getSkuQuantityDetailList().stream())
				.map(sku -> sku.getFulfilmentProductSkuId().toString())
				.collect(Collectors.toList()));
		//根据定制SKU查询关联订单 如果没有记录那么就新建一条记录 状态是0 ，如果存在记录检查状态 如果是已发货 那么 检查数据库的订单是否需要变更
		fullOrderList.forEach(subOrderForSupplier -> {
			if (subOrderForSupplier.getSkuQuantityDetailList() != null && !subOrderForSupplier.getSkuQuantityDetailList().isEmpty()) {
				subOrderForSupplier.getSkuQuantityDetailList().forEach(skuQuantityDetail -> {
					//	获取定制sku信息根据定制SKU 信息查询订单信息
					Optional<TemuOrderDO> first = selectListByCustomSku
							.stream()
							.filter(order -> order.getCustomSku().equals(skuQuantityDetail.getFulfilmentProductSkuId().toString()))
							.findFirst();
					if (first.isPresent()) {
						TemuOrderDO temuOrderDO = first.get();
						log.info("存在记录\n{}", temuOrderDO);
						// 判断temu的订单状态 如果是已发货状态  更新temu订单状态 为已发货
						if (subOrderForSupplier.getStatus() == OrderInfoEnum.STATUS_DELIVERED) {
							//更新为已发货状态
							temuOrderDO.setOrderStatus(TemuOrderStatusEnum.COMPLETED);
						}
						//temu 状态为作废状态
						if (subOrderForSupplier.getStatus() == OrderInfoEnum.STATUS_CANCELLED) {
							//更新为已取消状态
							temuOrderDO.setOrderStatus(TemuOrderStatusEnum.CANCELLED);
						}
						temuOrderMapper.updateById(temuOrderDO);
					} else {
						log.info("不存在记录\n{}", subOrderForSupplier);
						if (ListUtil.toList(OrderInfoEnum.STATUS_ACCEPTED_PENDING_SHIPMENT, OrderInfoEnum.STATUS_DELIVERED).contains(subOrderForSupplier.getStatus())) {
							TemuOrderDO orderData = new TemuOrderDO();
							//订单编号
							orderData.setOrderNo(subOrderForSupplier.getSubPurchaseOrderSn());
							//商品标题
							orderData.setProductTitle("【测试】" + subOrderForSupplier.getProductName());
							//订单状态
							orderData.setOrderStatus(TemuOrderStatusEnum.UNDELIVERED);
							//sku
							orderData.setSku(String.valueOf(skuQuantityDetail.getProductSkuId()));
							//skc
							orderData.setSkc(String.valueOf(subOrderForSupplier.getProductSkcId()));
							//申报价格(取不到)
							orderData.setSalePrice(new BigDecimal(0));
							//custom_sku
							orderData.setCustomSku(skuQuantityDetail.getFulfilmentProductSkuId().toString());
							//数量
							orderData.setQuantity(skuQuantityDetail.getPurchaseQuantity());
							//商品属性
							orderData.setProductProperties(skuQuantityDetail.getClassName());
							//预订单创建时间
							orderData.setBookingTime(LocalDateTimeUtil.ofUTC(subOrderForSupplier.getPurchaseTime()));
							//店铺ID
							orderData.setShopId(temuShopDO.getShopId());
							//定制图片列表
							orderData.setCustomImageUrls(String.join(",", skuQuantityDetail.getThumbUrlList()));
							//定制文字列表
							orderData.setCustomTextList("");
							//商品图片地址
							orderData.setProductImgUrl(subOrderForSupplier.getProductSkcPicture());
							//记录原始数据
							orderData.setOriginalInfo(JSONUtil.toJsonStr(subOrderForSupplier));
							//发货信息
							orderData.setShippingInfo(JSONUtil.toJsonStr(subOrderForSupplier.getDeliverInfo()));
							temuOrderMapper.insert(orderData);
						}
					}
					
				});
			}
		});
		return fullOrderList.get(0);
	}
	
	@Override
	public void doWeiXinNotifyMessage(WeiXinNotifyMessage message) {
		if (message != null) {
			WeiXinWebHookNotifyUtil instance = WeiXinWebHookNotifyUtil.getInstance(message.getNotifyUrl());
			if (message.getToUser() != null && message.getToUser().length > 0) {
				instance.sendTextMessage(message.getContent(), Arrays.asList(message.getToUser()));
			} else {
				instance.sendTextMessage(message.getContent());
			}
		}
	}
	
}
