package cn.iocoder.yudao.module.temu.controller.admin.vo.skuconfirmation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - SKU确认 Response VO")
@Data
public class TemuSkuConfirmationRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "店铺ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String shopId;

    @Schema(description = "SKU编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sku;

    @Schema(description = "状态：0-未确认，1-已确认", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}