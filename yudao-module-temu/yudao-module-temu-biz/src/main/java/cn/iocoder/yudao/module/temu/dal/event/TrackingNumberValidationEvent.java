package cn.iocoder.yudao.module.temu.dal.event;

import lombok.Data;
import java.util.List;

/**
 * 物流单号校验事件
 */
@Data
public class TrackingNumberValidationEvent {
    private final List<String> trackingNumbers;

    public TrackingNumberValidationEvent(List<String> trackingNumbers) {
        this.trackingNumbers = trackingNumbers;
    }
} 