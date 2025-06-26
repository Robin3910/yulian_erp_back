package cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 告警规则更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemuAlertRuleUpdateReqVO extends TemuAlertRuleBaseVO {

    @Schema(description = "规则编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "规则编号不能为空")
    private Long id;

}
