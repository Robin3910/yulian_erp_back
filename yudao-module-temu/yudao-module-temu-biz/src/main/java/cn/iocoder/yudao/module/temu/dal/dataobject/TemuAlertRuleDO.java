package cn.iocoder.yudao.module.temu.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.temu.enums.TemuAlertCompareTypeEnum;
import cn.iocoder.yudao.module.temu.enums.TemuAlertRuleStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 告警规则 DO
 * 
 * @author 芋道源码
 */
@TableName("temu_alert_rule")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemuAlertRuleDO extends BaseDO {

    /**
     * 规则编号
     */
    @TableId
    private Long id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则描述
     */
    private String description;

    /**
     * SQL查询内容
     */
    private String sqlContent;

    /**
     * 阈值
     */
    private Integer threshold;

    /**
     * 比较类型
     *
     * 枚举 {@link TemuAlertCompareTypeEnum}
     */
    private Integer compareType;

    /**
     * CRON 表达式
     */
    private String cronExpression;

    /**
     * 企业微信 Webhook 地址
     * 支持配置多个地址，以逗号分隔
     */
    private String webhookUrl;

    /**
     * 通知模板
     */
    private String notifyTemplate;

    /**
     * 状态
     * 
     * 枚举 {@link TemuAlertRuleStatusEnum}
     */
    private Integer status;

    /**
     * 上次执行时间
     */
    private LocalDateTime lastExecuteTime;
}
