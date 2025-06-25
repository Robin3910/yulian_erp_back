package cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 告警规则 Base VO，提供给添加、修改、详细的子 VO 使用
 * 如果子 VO 存在差异的字段，请不要添加到这里，影响 Swagger 文档生成
 */
@Data
public class TemuAlertRuleBaseVO {

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "订单告警")
    @NotEmpty(message = "规则名称不能为空")
    private String name;

    @Schema(description = "规则描述", example = "检测当天是否有未下单的订单")
    private String description;

    @Schema(description = "SQL查询内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "SELECT COUNT(*) FROM trade_order WHERE status = 0 AND create_time >= CURDATE()")
    @NotEmpty(message = "SQL查询内容不能为空")
    private String sqlContent;

    @Schema(description = "阈值", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "阈值不能为空")
    private Integer threshold;

    @Schema(description = "比较类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    @NotNull(message = "比较类型不能为空")
    private Integer compareType;

    @Schema(description = "CRON表达式", requiredMode = Schema.RequiredMode.REQUIRED, example = "0 0 20 * * ?")
    @NotEmpty(message = "CRON表达式不能为空")
    private String cronExpression;

    @Schema(description = "企业微信Webhook地址，支持配置多个地址，以逗号分隔", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxxx,https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=yyyy")
    @NotEmpty(message = "企业微信Webhook地址不能为空")
    private String webhookUrl;

    @Schema(description = "通知模板", requiredMode = Schema.RequiredMode.REQUIRED, example = "警告：系统检测到当前有${resultValue}个未下单的订单，超过阈值${threshold}，请及时处理！")
    @NotEmpty(message = "通知模板不能为空")
    private String notifyTemplate;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "状态不能为空")
    private Integer status;
}