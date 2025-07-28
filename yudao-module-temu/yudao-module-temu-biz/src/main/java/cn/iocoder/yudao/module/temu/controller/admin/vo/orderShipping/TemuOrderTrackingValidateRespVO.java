package cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - Temu物流单号验证结果 Response VO")
@Data
public class TemuOrderTrackingValidateRespVO {
    
    @Schema(description = "验证结果，true表示验证通过，false表示验证失败")
    private Boolean success;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "物流单号与订单号的映射关系")
    private Map<String, List<String>> trackingNumberToOrderNos;

    @Schema(description = "物流单号与SKU信息的映射关系")
    private Map<String, List<SkuInfo>> trackingNumberToSkus;

    @Data
    public static class SkuInfo {
        @Schema(description = "SKU ID")
        private Long productSkuId;

        @Schema(description = "SKU数量")
        private Integer skuNum;
    }
}