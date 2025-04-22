package cn.iocoder.yudao.module.temu.controller.admin.vo.category;

import cn.hutool.json.JSONObject;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 商品品类 Response VO")
@Data
public class TemuCategoryRespVO {
	
	@Schema(description = "品类编号", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	private Long id;
	
	@Schema(description = "商品品类ID", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	private Long categoryId;
	
	@Schema(description = "商品名称", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
	
	@Schema(description = "创建时间", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	private LocalDateTime createTime;
	@Schema(description = "更新时间", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	private LocalDateTime updateTime;
	@Schema(description = "定价类型", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	private Integer ruleType;
	@Schema(description = "价格规则", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	private Object unitPrice;
	@Schema(description = "合规单类型", example = "1")
	private String oldType;
	
	
} 