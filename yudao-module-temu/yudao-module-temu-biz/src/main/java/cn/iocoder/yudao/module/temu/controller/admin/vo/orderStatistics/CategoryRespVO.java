package cn.iocoder.yudao.module.temu.controller.admin.vo.orderStatistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CategoryRespVO {
    @Schema(description = "类目ID")
    private String categoryId;

    @Schema(description = "类目名称")
    private String categoryName;
}