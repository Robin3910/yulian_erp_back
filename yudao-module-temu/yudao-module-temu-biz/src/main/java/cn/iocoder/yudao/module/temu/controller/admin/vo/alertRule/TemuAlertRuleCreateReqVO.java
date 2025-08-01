package cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 告警规则创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemuAlertRuleCreateReqVO extends TemuAlertRuleBaseVO {

}
