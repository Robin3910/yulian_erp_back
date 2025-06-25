package cn.iocoder.yudao.module.temu.utils.alertrule;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuAlertRuleDO;
import cn.iocoder.yudao.module.temu.enums.TemuAlertCompareTypeEnum;
import cn.iocoder.yudao.module.temu.enums.TemuAlertRuleStatusEnum;
import cn.iocoder.yudao.module.temu.mq.producer.weixin.WeiXinProducer;
import cn.iocoder.yudao.module.temu.service.alertRule.TemuAlertRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

/**
 * 告警检查 Job
 * 修改为支持单个规则执行
 *
 * @author 芋道源码
 */
@Slf4j
public class TemuAlertCheckJob implements JobHandler {

    private final TemuAlertRuleService alertRuleService;
    private final JdbcTemplate jdbcTemplate;
    private final WeiXinProducer weiXinProducer;

    /**
     * 构造函数
     */
    public TemuAlertCheckJob(TemuAlertRuleService alertRuleService, JdbcTemplate jdbcTemplate,
            WeiXinProducer weiXinProducer) {
        this.alertRuleService = alertRuleService;
        this.jdbcTemplate = jdbcTemplate;
        this.weiXinProducer = weiXinProducer;
    }

    @Override
    @TenantJob
    public String execute(String param) throws Exception {
        // 判断是否有规则ID参数
        if (StrUtil.isNotEmpty(param)) {
            try {
                // 执行单个规则
                Long ruleId = Long.parseLong(param);
                TemuAlertRuleDO rule = alertRuleService.getAlertRule(ruleId);

                // 检查规则是否存在且处于启用状态
                if (rule == null) {
                    log.warn("[execute][规则({})不存在]", ruleId);
                    return "规则不存在";
                }

                if (!TemuAlertRuleStatusEnum.ENABLE.getStatus().equals(rule.getStatus())) {
                    return "规则未启用";
                }

                // 执行规则检查
                boolean isAlert = checkRule(rule);

                // 更新规则的最后执行时间
                alertRuleService.updateLastExecuteTime(rule.getId(), LocalDateTime.now());

                return String.format("规则(%s)执行完成，是否告警: %s", rule.getName(), isAlert);
            } catch (NumberFormatException e) {
                log.error("[execute][参数解析异常: {}]", param, e);
                return "参数解析异常";
            }
        }

        // 参数为空，不执行任何规则
        return "未指定规则ID";
    }

    /**
     * 检查规则是否需要告警
     *
     * @param rule 告警规则
     * @return 是否触发了告警
     */
    private boolean checkRule(TemuAlertRuleDO rule) {
        // 1. 执行SQL查询
        Integer result;
        try {
            result = executeQuery(rule.getSqlContent());
        } catch (Exception e) {
            log.error("[checkRule][规则({}) 执行SQL异常]", rule.getName(), e);
            return false;
        }

        // 2. 比较结果与阈值
        boolean needAlert = compareResult(result, rule.getThreshold(), rule.getCompareType());
        if (!needAlert) {
            return false;
        }

        // 3. 发送企业微信通知
        try {
            sendWeixinNotification(rule, result);
            return true;
        } catch (Exception e) {
            log.error("[checkRule][规则({}) 发送告警通知异常]", rule.getName(), e);
            return false;
        }
    }

    /**
     * 执行SQL查询
     */
    private Integer executeQuery(String sql) {
        if (StrUtil.isEmpty(sql)) {
            throw new RuntimeException("SQL不能为空");
        }
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    /**
     * 比较结果与阈值
     */
    private boolean compareResult(Integer value, Integer threshold, Integer compareType) {
        return TemuAlertCompareTypeEnum.isMatch(compareType, value, threshold);
    }

    /**
     * 发送企业微信通知
     */
    private void sendWeixinNotification(TemuAlertRuleDO rule, Integer resultValue) {
        // 构建通知内容
        String content = rule.getNotifyTemplate()
                .replace("${ruleName}", rule.getName())
                .replace("${threshold}", String.valueOf(rule.getThreshold()))
                .replace("${resultValue}", String.valueOf(resultValue))
                .replace("${alertTime}", DateUtil.formatLocalDateTime(LocalDateTime.now()));

        // 处理可能的多个Webhook地址
        String webhookUrls = rule.getWebhookUrl();
        if (StrUtil.isNotBlank(webhookUrls)) {
            // 按逗号分割Webhook地址
            String[] urls = webhookUrls.split(",");
            for (String url : urls) {
                String trimmedUrl = url.trim();
                if (StrUtil.isNotBlank(trimmedUrl)) {
                    // 向每个地址发送通知
                    weiXinProducer.sendMessage(trimmedUrl, content);
                }
            }
        }
    }
}