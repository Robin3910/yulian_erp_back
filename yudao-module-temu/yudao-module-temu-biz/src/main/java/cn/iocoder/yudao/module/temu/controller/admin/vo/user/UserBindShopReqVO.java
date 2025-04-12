package cn.iocoder.yudao.module.temu.controller.admin.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 用户绑定店铺 Request VO")
@Data
public class UserBindShopReqVO {
	@Schema(description = "店铺ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "用户ID不能为空")
	private Long userId;
	@Schema(description = "店铺ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "店铺ID不能为空")
	private Long shopId;
	@Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "租户ID不能为空")
	private Long tenantId;
}
