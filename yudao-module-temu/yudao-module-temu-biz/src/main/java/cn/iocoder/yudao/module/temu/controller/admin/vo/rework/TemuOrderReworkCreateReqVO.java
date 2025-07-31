package cn.iocoder.yudao.module.temu.controller.admin.vo.rework;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 订单返工创建 Request VO")
@Data
public class TemuOrderReworkCreateReqVO {

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER_001")
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    @Schema(description = "定制SKU", requiredMode = Schema.RequiredMode.REQUIRED, example = "CUSTOM_SKU_001")
    @NotBlank(message = "定制SKU不能为空")
    private String customSku;

    @Schema(description = "返工原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "产品质量问题，需要重新生产")
    @NotBlank(message = "返工原因不能为空")
    private String reworkReason;

} 