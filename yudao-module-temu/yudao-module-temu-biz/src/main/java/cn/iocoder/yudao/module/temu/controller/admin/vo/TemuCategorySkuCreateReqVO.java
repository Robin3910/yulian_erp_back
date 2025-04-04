package cn.iocoder.yudao.module.temu.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 商品品类SKU关系创建 Request VO")
@Data
public class TemuCategorySkuCreateReqVO {

    @Schema(description = "商品品类ID", required = true, example = "2048")
    @NotNull(message = "商品品类ID不能为空")
    private Long categoryId;

    @Schema(description = "SKU", required = true, example = "ABC123456")
    @NotEmpty(message = "SKU不能为空")
    private String sku;

    @Schema(description = "店铺ID", required = true, example = "3072")
    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    @Schema(description = "商品品类名称", required = true, example = "定制木质商品")
    @NotEmpty(message = "商品品类名称不能为空")
    private String categoryName;
} 