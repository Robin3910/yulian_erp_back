package cn.iocoder.yudao.module.temu.controller.admin.vo.skuconfirmation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - SKU确认请求 Request VO")
@Data
public class TemuSkuConfirmationReqVO {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
}