package cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - Temu订单批次信息 Response VO")
@Data
public class TemuOrderBatchRespVO {

    @Schema(description = "批次ID")
    private Long id;

    @Schema(description = "批次编号")
    private String batchNo;

    @Schema(description = "批次所属类目id")
    private String batchCategoryId;

    @Schema(description = "商品品类ID列表")
    private List<String> categoryIds;

} 