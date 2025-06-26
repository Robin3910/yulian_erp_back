package cn.iocoder.yudao.module.temu.utils.alertrule;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 告警任务处理器
 * 统一处理所有告警规则的定时任务
 */
@Component
public class TemuAlertJobHandler implements JobHandler {

    @Autowired
    private TemuAlertCheckJob alertCheckJob;

    @Override
    public String execute(String param) throws Exception {
        // param就是规则ID
        return alertCheckJob.execute(param);
    }
}