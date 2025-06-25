package cn.iocoder.yudao.module.temu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警规则状态枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum TemuAlertRuleStatusEnum {

    DISABLE(0, "禁用"),
    ENABLE(1, "启用"),
    ;

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 描述
     */
    private final String desc;
}
