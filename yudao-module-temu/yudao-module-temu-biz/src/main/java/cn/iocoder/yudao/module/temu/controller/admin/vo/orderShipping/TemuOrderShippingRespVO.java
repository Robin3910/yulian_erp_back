package cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "temu管理 - 待发货列表 Response VO")
@Data
public class TemuOrderShippingRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "物流单号")
    private String trackingNumber;

    @Schema(description = "快递面单图片URL")
    private String expressImageUrl;

    @Schema(description = "快递面单外单图片URL")
    private String expressOutsideImageUrl;

    @Schema(description = "快递面SKU图片URL")
    private String expressSkuImageUrl;

    @Schema(description = "店铺ID")
    private Long shopId;

    // 联表查询字段
    @Schema(description = "订单id")
    private Long orderId;

    @Schema(description = "商品图片URL")
    private String productImgUrl;

    @Schema(description = "SKU编号")
    private String sku;

    @Schema(description = "SKC编号")
    private String skc;

    @Schema(description = "定制SKU")
    private String customSku;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "定制图片列表URL")
    private String customImageUrls;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "商品标题")
    private String productTitle;

    @Schema(description = "商品属性")
    private String productProperties;

    @Schema(description = "预定单创建时间")
    private LocalDateTime createTime;

    @Schema(description = "定制文字列表")
    private String customTextList;

    @Schema(description = "订单状态")
    private Integer orderStatus;

    @Schema(description = "合成预览图")
    private String effectiveImgUrl;

    @Schema(description = "Temu管理 - 待发货列表保存请求 VO")
    @Data
    public static class TemuOrderShippingSaveRequestVO {

        @Schema(description = "订单Id", required = true)
        private String orderId;

        @Schema(description = "物流单号")
        private String trackingNumber;

        @Schema(description = "快递面单图片URL", required = true)
        private String expressImageUrl;

        @Schema(description = "快递面单外单图片URL", required = true)
        private String expressOutsideImageUrl;

        @Schema(description = "快递面单SKU图片URL", required = true)
        private String expressSkuImageUrl;

        @Schema(description = "店铺id", required = true)
        private Long shopId;

    }
}
