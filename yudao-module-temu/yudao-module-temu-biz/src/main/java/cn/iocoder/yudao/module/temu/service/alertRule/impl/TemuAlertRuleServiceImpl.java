package cn.iocoder.yudao.module.temu.service.alertRule.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.quartz.core.enums.JobDataKeyEnum;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandlerInvoker;
import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRuleCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRulePageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRuleUpdateReqVO;
import cn.iocoder.yudao.module.temu.utils.alertrule.TemuAlertRuleConvert;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuAlertRuleDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuAlertRuleMapper;
import cn.iocoder.yudao.module.temu.enums.TemuAlertRuleStatusEnum;
import cn.iocoder.yudao.module.temu.service.alertRule.TemuAlertRuleService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.ALERT_RULE_NOT_EXISTS;

/**
 * 告警规则 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class TemuAlertRuleServiceImpl implements TemuAlertRuleService {

    @Resource
    private TemuAlertRuleMapper alertRuleMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private SchedulerManager schedulerManager;

    @Autowired(required = false)
    private Scheduler scheduler;

    @Override
    public Long createAlertRule(TemuAlertRuleCreateReqVO createReqVO) {
        // 插入
        TemuAlertRuleDO alertRule = TemuAlertRuleConvert.INSTANCE.convert(createReqVO);
        alertRuleMapper.insert(alertRule);

        // 如果规则是启用状态，则创建定时任务
        if (TemuAlertRuleStatusEnum.ENABLE.getStatus().equals(alertRule.getStatus())) {
            try {
                createScheduleJob(alertRule);
            } catch (Exception e) {
                // createScheduleJob 内部已经处理了异常，这里不应该再有异常抛出
                log.error("[createAlertRule][规则({})创建定时任务时发生意外异常]", alertRule.getName(), e);
            }
        }

        // 返回
        return alertRule.getId();
    }

    @Override
    public void updateAlertRule(TemuAlertRuleUpdateReqVO updateReqVO) {
        // 校验存在
        TemuAlertRuleDO oldRule = validateAlertRuleExists2(updateReqVO.getId());

        // 更新
        TemuAlertRuleDO updateObj = TemuAlertRuleConvert.INSTANCE.convert(updateReqVO);
        alertRuleMapper.updateById(updateObj);

        // 处理定时任务
        // 如果状态从启用变为禁用，则删除任务
        if (TemuAlertRuleStatusEnum.ENABLE.getStatus().equals(oldRule.getStatus()) &&
                TemuAlertRuleStatusEnum.DISABLE.getStatus().equals(updateObj.getStatus())) {
            try {
                deleteScheduleJob(updateObj.getId());
            } catch (Exception e) {
                // deleteScheduleJob 内部已经处理了异常，这里不应该再有异常抛出
                log.error("[updateAlertRule][规则({})删除定时任务时发生意外异常]", updateObj.getName(), e);
            }
        }
        // 如果状态从禁用变为启用，则创建任务
        else if (TemuAlertRuleStatusEnum.DISABLE.getStatus().equals(oldRule.getStatus()) &&
                TemuAlertRuleStatusEnum.ENABLE.getStatus().equals(updateObj.getStatus())) {
            try {
                createScheduleJob(updateObj);
            } catch (Exception e) {
                // createScheduleJob 内部已经处理了异常，这里不应该再有异常抛出
                log.error("[updateAlertRule][规则({})创建定时任务时发生意外异常]", updateObj.getName(), e);
            }
        }
        // 如果CRON表达式变更，则更新任务
        else if (TemuAlertRuleStatusEnum.ENABLE.getStatus().equals(updateObj.getStatus()) &&
                !oldRule.getCronExpression().equals(updateObj.getCronExpression())) {
            try {
                updateScheduleJob(updateObj);
            } catch (Exception e) {
                // updateScheduleJob 内部已经处理了异常，这里不应该再有异常抛出
                log.error("[updateAlertRule][规则({})更新定时任务时发生意外异常]", updateObj.getName(), e);
            }
        }
    }

    @Override
    public void deleteAlertRule(Long id) {
        // 校验存在
        TemuAlertRuleDO rule = validateAlertRuleExists2(id);

        // 删除规则
        alertRuleMapper.deleteById(id);

        // 删除定时任务
        if (TemuAlertRuleStatusEnum.ENABLE.getStatus().equals(rule.getStatus())) {
            try {
                deleteScheduleJob(id);
            } catch (Exception e) {
                // deleteScheduleJob 内部已经处理了异常，这里不应该再有异常抛出
                log.error("[deleteAlertRule][规则({})删除定时任务时发生意外异常]", rule.getName(), e);
            }
        }
    }

    private TemuAlertRuleDO validateAlertRuleExists2(Long id) {
        TemuAlertRuleDO rule = alertRuleMapper.selectById(id);
        if (rule == null) {
            throw exception(ALERT_RULE_NOT_EXISTS);
        }
        return rule;
    }

    @Override
    public TemuAlertRuleDO getAlertRule(Long id) {
        return alertRuleMapper.selectById(id);
    }

    @Override
    public PageResult<TemuAlertRuleDO> getAlertRulePage(TemuAlertRulePageReqVO pageReqVO) {
        return alertRuleMapper.selectPage(pageReqVO);
    }

    @Override
    public List<TemuAlertRuleDO> getEnabledRules() {
        return alertRuleMapper.selectListByStatus(TemuAlertRuleStatusEnum.ENABLE.getStatus());
    }

    @Override
    public List<TemuAlertRuleDO> getAllRules() {
        return alertRuleMapper.selectList();
    }

    @Override
    public void updateLastExecuteTime(Long id, LocalDateTime lastExecuteTime) {
        alertRuleMapper.updateLastExecuteTime(id, lastExecuteTime);
    }

    @Override
    public void updateAlertRuleStatus(Long id, Integer status) {
        // 校验存在
        TemuAlertRuleDO rule = validateAlertRuleExists2(id);

        // 如果状态相同，则不处理
        if (status.equals(rule.getStatus())) {
            return;
        }

        // 更新状态
        TemuAlertRuleDO updateObj = new TemuAlertRuleDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        alertRuleMapper.updateById(updateObj);

        // 处理定时任务
        if (TemuAlertRuleStatusEnum.ENABLE.getStatus().equals(status)) {
            // 启用规则，创建定时任务
            rule.setStatus(status); // 更新状态
            try {
                createScheduleJob(rule);
            } catch (Exception e) {
                // createScheduleJob 内部已经处理了异常，这里不应该再有异常抛出
                log.error("[updateAlertRuleStatus][规则({})创建定时任务时发生意外异常]", rule.getName(), e);
            }
        } else {
            // 禁用规则，删除定时任务
            try {
                deleteScheduleJob(id);
            } catch (Exception e) {
                // deleteScheduleJob 内部已经处理了异常，这里不应该再有异常抛出
                log.error("[updateAlertRuleStatus][规则({})删除定时任务时发生意外异常]", rule.getName(), e);
            }
        }
    }

    @Override
    public Integer testAlertRule(Long id) {
        // 校验存在
        TemuAlertRuleDO alertRule = validateAlertRuleExists2(id);
        // 执行SQL
        try {
            return executeQuery(alertRule.getSqlContent());
        } catch (Exception e) {
            log.error("[testAlertRule][规则({}) 执行SQL({}) 发生异常]", id, alertRule.getSqlContent(), e);
            throw new RuntimeException("SQL执行失败: " + e.getMessage());
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
     * 创建定时任务
     */
    private void createScheduleJob(TemuAlertRuleDO rule) throws SchedulerException {
        if (scheduler == null) {
            log.warn("[createScheduleJob][定时任务功能已被禁用，规则({})无法创建定时任务]", rule.getName());
            return;
        }

        try {
            // 为每个规则创建唯一的JobKey
            String jobName = "temuAlert_" + rule.getId();
            JobKey jobKey = new JobKey(jobName, "TEMU_ALERT");

            // 先尝试删除可能存在的任务
            try {
                scheduler.deleteJob(jobKey);
                log.info("[createScheduleJob][规则({})删除旧任务成功]", rule.getName());
            } catch (Exception e) {
                // 忽略不存在的任务导致的异常
                if (e.getMessage() != null && !e.getMessage().contains("doesn't exist")) {
                    log.warn("[createScheduleJob][规则({})删除旧任务异常: {}]", rule.getName(), e.getMessage());
                }
            }

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

            log.info("[createScheduleJob][规则({})创建定时任务成功，CRON: {}]", rule.getName(), rule.getCronExpression());
        } catch (Exception e) {
            log.warn("[createScheduleJob][规则({})创建定时任务失败: {}]", rule.getName(), e.getMessage());
            // 不抛出异常，允许在禁用 Quartz 的环境中继续执行
        }
    }

    /**
     * 更新定时任务
     */
    private void updateScheduleJob(TemuAlertRuleDO rule) throws SchedulerException {
        if (scheduler == null) {
            log.warn("[updateScheduleJob][定时任务功能已被禁用，规则({})无法更新定时任务]", rule.getName());
            return;
        }

        try {
            // 为每个规则创建唯一的JobKey和TriggerKey
            String jobName = "temuAlert_" + rule.getId();
            TriggerKey triggerKey = new TriggerKey(jobName + "_trigger", "TEMU_ALERT");

            // 检查触发器是否存在
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            if (trigger == null) {
                // 如果触发器不存在，则创建新任务
                createScheduleJob(rule);
                return;
            }

            // 创建新的触发器
            CronTrigger newTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(rule.getCronExpression()))
                    .build();

            // 更新触发器
            scheduler.rescheduleJob(triggerKey, newTrigger);

            log.info("[updateScheduleJob][规则({})更新定时任务成功，新CRON: {}]", rule.getName(), rule.getCronExpression());
        } catch (Exception e) {
            log.warn("[updateScheduleJob][规则({})更新定时任务失败: {}]", rule.getName(), e.getMessage());
            // 不抛出异常，允许在禁用 Quartz 的环境中继续执行
        }
    }

    /**
     * 删除定时任务
     */
    private void deleteScheduleJob(Long ruleId) throws SchedulerException {
        if (scheduler == null) {
            log.warn("[deleteScheduleJob][定时任务功能已被禁用，规则ID({})无法删除定时任务]", ruleId);
            return;
        }

        try {
            // 为规则创建唯一的JobKey
            String jobName = "temuAlert_" + ruleId;
            JobKey jobKey = new JobKey(jobName, "TEMU_ALERT");

            // 删除任务
            boolean result = scheduler.deleteJob(jobKey);
            if (result) {
                log.info("[deleteScheduleJob][规则ID({})删除定时任务成功]", ruleId);
            } else {
                log.warn("[deleteScheduleJob][规则ID({})删除定时任务失败，可能任务不存在]", ruleId);
            }
        } catch (Exception e) {
            log.warn("[deleteScheduleJob][规则ID({})删除定时任务异常: {}]", ruleId, e.getMessage());
            // 不抛出异常，允许在禁用 Quartz 的环境中继续执行
        }
    }
}
