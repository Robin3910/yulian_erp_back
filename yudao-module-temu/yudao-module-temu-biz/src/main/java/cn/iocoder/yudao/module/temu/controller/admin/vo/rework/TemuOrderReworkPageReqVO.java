package cn.iocoder.yudao.module.temu.controller.admin.vo.rework;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理后台 - 返工订单分页 Request VO")
public class TemuOrderReworkPageReqVO {
    @Schema(description = "页码", example = "1")
    private Integer pageNo = 1;

    @Schema(description = "每页数量", example = "10")
    private Integer pageSize = 10;

    // 可扩展更多查询条件，如customSku、isFinished等
    @Schema(description = "定制SKU", example = "CUSTOM_SKU_001")
    private String customSku;

    @Schema(description = "是否完成", example = "1")
    private Integer isFinished;
} 