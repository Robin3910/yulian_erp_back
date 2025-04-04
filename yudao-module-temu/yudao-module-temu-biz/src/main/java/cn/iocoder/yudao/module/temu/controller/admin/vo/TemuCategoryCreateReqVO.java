package cn.iocoder.yudao.module.temu.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 商品品类创建 Request VO")
@Data
public class TemuCategoryCreateReqVO {

    @Schema(description = "商品品类ID", required = true, example = "2048")
    @NotNull(message = "商品品类ID不能为空")
    private Long categoryId;

    @Schema(description = "商品名称", required = true, example = "定制木质商品")
    @NotEmpty(message = "商品名称不能为空")
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