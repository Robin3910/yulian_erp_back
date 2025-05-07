package cn.iocoder.yudao.module.temu.controller.admin.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "管理后台 - 用户精简信息 Response VO")
@AllArgsConstructor
@NoArgsConstructor
public class UserSimpleRespVo {
	@Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
	private String id;
	@Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋道")
	private String nickname;
	@Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "yudao")
	private String username;
}
