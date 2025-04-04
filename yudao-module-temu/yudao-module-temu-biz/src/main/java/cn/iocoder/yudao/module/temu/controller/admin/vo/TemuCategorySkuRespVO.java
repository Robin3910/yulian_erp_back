package cn.iocoder.yudao.module.temu.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 商品品类SKU关系 Response VO")
@Data
public class TemuCategorySkuRespVO {

    @Schema(description = "编号", required = true, example = "1024")
    private Long id;

    @Schema(description = "商品品类ID", required = true, example = "2048")
    private Long categoryId;
    
    @Schema(description = "商品品类名称", required = true, example = "定制木质商品")
    private String categoryName;

    @Schema(description = "SKU", required = true, example = "ABC123456")
    private String sku;

    @Schema(description = "店铺ID", required = true, example = "3072")
    private Long shopId;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
} 