package cn.iocoder.yudao.module.temu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警比较类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum TemuAlertCompareTypeEnum {

    GT(1, "大于"),
    EQ(2, "等于"),
    LT(3, "小于"),
    GTE(4, "大于等于"),
    LTE(5, "小于等于"),
    NEQ(6, "不等于"),
    ;

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 描述
     */
    private final String desc;

    /**
     * 比较值是否满足告警条件
     *
     * @param type      比较类型
     * @param value     实际值
     * @param threshold 阈值
     * @return 是否满足条件
     */
    public static boolean isMatch(Integer type, Integer value, Integer threshold) {
        if (GT.getType().equals(type)) {
            return value > threshold;
        } else if (EQ.getType().equals(type)) {
            return value.equals(threshold);
        } else if (LT.getType().equals(type)) {
            return value < threshold;
        } else if (GTE.getType().equals(type)) {
            return value >= threshold;
        } else if (LTE.getType().equals(type)) {
            return value <= threshold;
        } else if (NEQ.getType().equals(type)) {
            return !value.equals(threshold);
        }
        return false;
    }
}
