/*
 * @Author: 徐佳德 1404577549@qq.com
 * @Date: 2025-07-28 10:39:05
 * @LastEditors: 徐佳德 1404577549@qq.com
 * @LastEditTime: 2025-07-28 10:45:37
 * @FilePath: \yulian_erp_back\yudao-module-temu\yudao-module-temu-biz\src\main\java\cn\iocoder\yudao\module\temu\enums\ReturnReasonEnum.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package cn.iocoder.yudao.module.temu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 返工原因枚举
 *
 * @author wujunlin
 */
@Getter
@AllArgsConstructor
public enum ReturnReasonEnum {

    WRONG_LABEL(0, "贴错标"),
    WRONG_IMAGE(1, "图做错"),
    WRONG_SIZE(2, "尺寸错"),
    PACKAGING_ISSUE(3, "包装问题"),
    TEMU_ISSUE(4, "temu问题");

    /**
     * 枚举值
     */
    private final Integer value;
    
    /**
     * 枚举描述
     */
    private final String description;

    /**
     * 根据值获取枚举
     */
    public static ReturnReasonEnum getByValue(Integer value) {
        for (ReturnReasonEnum reason : values()) {
            if (reason.getValue().equals(value)) {
                return reason;
            }
        }
        return null;
    }
} 