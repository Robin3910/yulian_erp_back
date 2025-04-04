package cn.iocoder.yudao.module.temu.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 商品品类分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemuCategoryPageReqVO extends PageParam {

    @Schema(description = "品类名称，模糊匹配", example = "木质")
    private String categoryName;

    @Schema(description = "品类ID", example = "1024")
    private Long categoryId;
} 