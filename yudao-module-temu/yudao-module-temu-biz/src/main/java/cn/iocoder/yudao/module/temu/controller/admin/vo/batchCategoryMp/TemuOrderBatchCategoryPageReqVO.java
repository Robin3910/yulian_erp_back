package cn.iocoder.yudao.module.temu.controller.admin.vo.batchCategoryMp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TemuOrderBatchCategoryPageReqVO {
    @Schema(description = "商品类目名称", example = "铃鼓")
    private String categoryName;

    @Schema(description = "页码", example = "1")
    private Integer pageNo;

    @Schema(description = "每页条数", example = "10")
    private Integer pageSize;

    @Schema(description = "批次所属类目id", example = "")
    private String batchCategoryId;

    @Schema(description = "商品品类ID", example = "")
    private Long categoryId;
} 