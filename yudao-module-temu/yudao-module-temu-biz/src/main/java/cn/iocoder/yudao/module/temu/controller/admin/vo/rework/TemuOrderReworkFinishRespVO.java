package cn.iocoder.yudao.module.temu.controller.admin.vo.rework;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理后台 - 完成返工 Response VO")
public class TemuOrderReworkFinishRespVO {
    @Schema(description = "定制SKU")
    private String customSku;
    @Schema(description = "是否完成")
    private Integer isFinished;
} 