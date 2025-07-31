package cn.iocoder.yudao.module.temu.controller.admin.vo.rework;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "管理后台 - 完成返工 Request VO")
public class TemuOrderReworkFinishReqVO {
    @NotBlank
    @Schema(description = "定制SKU", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customSku;
} 