package cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 修改返工原因 Request VO")
@Data
public class OrderReturnUpdateReasonReqVO {

    @Schema(description = "返单记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "返单记录ID不能为空")
    private Long id;

    @Schema(description = "返工原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "返工原因不能为空")
    private Integer repeatReason;
} 