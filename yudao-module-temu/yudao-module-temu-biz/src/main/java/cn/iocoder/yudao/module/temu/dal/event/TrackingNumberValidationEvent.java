package cn.iocoder.yudao.module.temu.dal.event;

import lombok.Data;
import java.util.Map;
import java.util.Set;

/**
 * 物流单号校验事件
 */
@Data
public class TrackingNumberValidationEvent {
    /**
     * 按shopId分组的物流单号列表，key为shopId，value为该shopId下的物流单号列表
     */
    private final Map<String, Set<String>> shopTrackingNumbers;

    public TrackingNumberValidationEvent(Map<String, Set<String>> shopTrackingNumbers) {
        this.shopTrackingNumbers = shopTrackingNumbers;
    }
} 