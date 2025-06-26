package cn.iocoder.yudao.module.temu.utils.alertrule;

import cn.iocoder.yudao.framework.quartz.core.enums.JobDataKeyEnum;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandlerInvoker;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuAlertRuleDO;
import cn.iocoder.yudao.module.temu.service.alertRule.TemuAlertRuleService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统启动时，初始化告警规则定时任务
 */
@Component
@Slf4j
@Order(Integer.MAX_VALUE) // 确保在所有ApplicationRunner之后执行
public class TemuAlertRuleInitializer implements ApplicationRunner {

    @Autowired
    private TemuAlertRuleService alertRuleService;

    @Autowired
    private TemuAlertCheckJob alertCheckJob;

    @Autowired(required = false)
    private Scheduler scheduler;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("[run][开始初始化告警规则定时任务]");

        // 设置忽略租户，这样就不会进行租户过滤
        Boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setIgnore(true);

            // 查询所有启用的告警规则
            List<TemuAlertRuleDO> rules = alertRuleService.getEnabledRules();
            if (rules.isEmpty()) {
                log.info("[run][没有启用的告警规则，无需初始化]");
                return;
            }

            // 检查是否存在告警检查Job组件
            if (alertCheckJob == null) {
                log.error("[run][未找到TemuAlertCheckJob组件，无法初始化告警规则]");
                return;
            }

            // 检查定时任务功能是否被禁用
            if (scheduler == null) {
                log.warn("[run][定时任务功能已被禁用，无法初始化告警规则定时任务]");
                return;
            }

            // 首先清理所有与告警相关的任务，避免冲突
            try {
                clearAllAlertJobs();
            } catch (Exception e) {
                log.warn("[run][清理现有告警任务异常]", e);
            }

            // 为每条规则注册独立的定时任务
            int successCount = 0;
            for (TemuAlertRuleDO rule : rules) {
                try {
                    registerRuleJob(rule);
                    successCount++;
                } catch (Exception e) {
                    log.error("[run][规则({})注册定时任务异常]", rule.getName(), e);
                }
            }

            log.info("[run][初始化告警规则定时任务完成，成功注册 {} 个任务]", successCount);
        } finally {
            // 恢复原来的租户设置
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    /**
     * 清理所有告警相关的任务
     */
    private void clearAllAlertJobs() throws SchedulerException {
        if (scheduler == null) {
            return;
        }

        // 清理所有任务组中的任务
        for (String groupName : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                try {
                    scheduler.deleteJob(jobKey);
                } catch (Exception e) {
                    log.warn("[clearAllAlertJobs][删除任务失败: {}.{}]", jobKey.getGroup(), jobKey.getName(), e);
                }
            }
        }
    }

    /**
     * 为单个规则注册定时任务
     */
    private void registerRuleJob(TemuAlertRuleDO rule) throws Exception {
        if (scheduler == null) {
            log.warn("[registerRuleJob][定时任务功能已被禁用，规则({})无法注册定时任务]", rule.getName());
            return;
        }

        try {
            // 为每个规则创建唯一的JobKey
            String jobName = "temuAlert_" + rule.getId();
            JobKey jobKey = new JobKey(jobName, "TEMU_ALERT");

            // 创建JobDetail，使用JobHandlerInvoker作为Job类
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(JobDataKeyEnum.JOB_ID.name(), rule.getId());
            jobDataMap.put(JobDataKeyEnum.JOB_HANDLER_NAME.name(), "temuAlertJobHandler");
            jobDataMap.put(JobDataKeyEnum.JOB_HANDLER_PARAM.name(), rule.getId().toString());

            JobDetail jobDetail = JobBuilder.newJob(JobHandlerInvoker.class)
                    .withIdentity(jobKey)
                    .usingJobData(jobDataMap)
                    .build();

            // 创建Trigger
            TriggerKey triggerKey = new TriggerKey(jobName + "_trigger", "TEMU_ALERT");
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(rule.getCronExpression()))
                    .build();

            // 注册任务
            scheduler.scheduleJob(jobDetail, trigger);

            log.info("[registerRuleJob][规则({})注册定时任务成功，CRON: {}]", rule.getName(), rule.getCronExpression());
        } catch (Exception e) {
            log.error("[registerRuleJob][规则({})注册定时任务异常]", rule.getName(), e);
            throw e;
        }
    }
}
