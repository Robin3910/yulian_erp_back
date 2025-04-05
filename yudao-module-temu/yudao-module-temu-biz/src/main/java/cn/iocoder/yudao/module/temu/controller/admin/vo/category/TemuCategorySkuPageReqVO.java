package cn.iocoder.yudao.module.temu.controller.admin.vo.category;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 商品品类SKU关系分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemuCategorySkuPageReqVO extends PageParam {

    @Schema(description = "商品品类ID", example = "2048")
    private Long categoryId;

    @Schema(description = "SKU", example = "ABC123456")
    private String sku;

    @Schema(description = "店铺ID", example = "3072")
    private Long shopId;
} 