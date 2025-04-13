package cn.iocoder.yudao.module.temu.controller.admin.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "Temu订单更新分类请求VO")
@Data
public class TemuOrderUpdateCategoryReqVo {
	@Schema(description = "分类ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "分类ID不能为空")
	private String categoryId;
	@Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "订单ID不能为空")
	private String id;
}
