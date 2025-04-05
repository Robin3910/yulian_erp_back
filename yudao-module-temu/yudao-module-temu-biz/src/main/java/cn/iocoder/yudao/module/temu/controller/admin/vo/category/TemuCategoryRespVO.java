package cn.iocoder.yudao.module.temu.controller.admin.vo.category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 商品品类 Response VO")
@Data
public class TemuCategoryRespVO {

    @Schema(description = "品类编号", required = true, example = "1024")
    private Long id;

    @Schema(description = "商品品类ID", required = true, example = "2048")
    private Long categoryId;

    @Schema(description = "商品名称", required = true, example = "定制木质商品")
    private String categoryName;
    
    @Schema(description = "长度(cm)", example = "10.5")
    private BigDecimal length;
    
    @Schema(description = "宽度(cm)", example = "5.2")
    private BigDecimal width;
    
    @Schema(description = "高度(cm)", example = "2.0")
    private BigDecimal height;
    
    @Schema(description = "重量(g)", example = "120.5")
    private BigDecimal weight;
    
    @Schema(description = "主图URL", example = "https://example.com/image.jpg")
    private String mainImageUrl;
} 