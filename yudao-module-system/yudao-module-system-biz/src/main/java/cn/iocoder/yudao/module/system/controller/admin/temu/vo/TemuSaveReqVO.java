package cn.iocoder.yudao.module.system.controller.admin.temu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Temu保存请求VO")
@Data
public class TemuSaveReqVO {
    
    @Schema(description = "店铺ID", required = true)
    @NotEmpty(message = "店铺ID不能为空")
    private String shopId;

    @Schema(description = "店铺名称", required = false)
    private String shopName;

    @Schema(description = "订单列表", required = true)
    @NotNull(message = "订单列表不能为空")
    private List<OrderVO> orders;

    @Data
    public static class OrderVO {
        @Schema(description = "订单ID")
        private String orderId;

        @Schema(description = "商品标题")
        private String title;

        @Schema(description = "创建时间")
        private String creationTime;

        @Schema(description = "发货截止时间")
        private String shippingDeadline;

        @Schema(description = "交付截止时间")
        private String deliveryDeadline;

        @Schema(description = "物流信息")
        private ShippingInfoVO shippingInfo;

        @Schema(description = "SKU信息")
        private SkuVO skus;

        @Schema(description = "价格")
        private Double price;

        @Schema(description = "数量")
        private Integer quantity;

        @Schema(description = "状态")
        private String status;
    }

    @Data
    public static class ShippingInfoVO {
        private String shippingTime;
        private String shippingNumber;
        private String receivingTime;
        private String actualWarehouse;
    }

    @Data
    public static class SkuVO {
        private String property;
        private String skuId;
        private String customSku;
    }
} 