package cn.iocoder.yudao.module.temu.utils.alertrule;

import cn.iocoder.yudao.module.temu.mq.producer.weixin.WeiXinProducer;
import cn.iocoder.yudao.module.temu.service.alertRule.TemuAlertRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 告警任务配置类
 * 确保TemuAlertCheckJob组件在所有依赖项初始化完成后再创建
 */
@Configuration
@Slf4j
public class TemuAlertJobConfig {

    @Bean("temuAlertCheckJob")
    public TemuAlertCheckJob temuAlertCheckJob(
            TemuAlertRuleService alertRuleService,
            JdbcTemplate jdbcTemplate,
            WeiXinProducer weiXinProducer) {
        log.info("[temuAlertCheckJob][初始化TemuAlertCheckJob组件]");
        return new TemuAlertCheckJob(alertRuleService, jdbcTemplate, weiXinProducer);
    }
}