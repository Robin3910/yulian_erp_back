package cn.iocoder.yudao.module.temu.dal.dataobject;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 订单 DO
 *
 * @author 芋道源码
 */
@TableName("temu_order")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TemuOrderDO extends BaseDO {
	
	/**
	 * 订单ID
	 */
	@TableId
	private Long id;
	/**
	 * 订单编号
	 */
	private String orderNo;
	/**
	 * 商品标题
	 */
	private String productTitle;
	/**
	 * 订单状态
	 */
	private Integer orderStatus;
	/**
	 * SKU编号
	 */
	private String sku;
	/**
	 * SKC编号
	 */
	private String skc;
	/**
	 * 申报价格
	 */
	private BigDecimal salePrice;
	/**
	 * 定制SKU
	 */
	private String customSku;
	/**
	 * 数量
	 */
	private Integer quantity;
	/**
	 * 商品属性
	 */
	private String productProperties;
	/**
	 * 预定单创建时间
	 */
	private LocalDateTime bookingTime;
	/**
	 * 店铺ID
	 */
	private Long shopId;
	/**
	 * 定制图片列表URL
	 */
	private String customImageUrls;
	/**
	 * 定制文字列表
	 */
	private String customTextList;
	/**
	 * 商品图片URL
	 */
	private String productImgUrl;
	/**
	 * 类目ID
	 */
	private String categoryId;
	/**
	 * 类目名称
	 */
	private String categoryName;
	/**
	 * 物流信息JSON字符串
	 */
	private String shippingInfo;
	/**
	 * 接口接收的源信息
	 */
	private String originalInfo;
	
}