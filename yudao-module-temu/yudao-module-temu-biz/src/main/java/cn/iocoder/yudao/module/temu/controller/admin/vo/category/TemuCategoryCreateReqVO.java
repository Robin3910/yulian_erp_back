package cn.iocoder.yudao.module.temu.controller.admin.vo.category;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 商品品类创建 Request VO")
@Data
public class TemuCategoryCreateReqVO {
	@Schema(description = "ID", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "2048")
	private Long id;
	@Schema(description = "商品名称", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "定制木质商品")
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
	
	@Schema(description = "价格规则类型", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	private String ruleType;
	
	@Schema(description = "价格规则", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	private Object unitPrice;
	@Schema(description = "合规单类型", example = "1")
	private String oldType;
} 