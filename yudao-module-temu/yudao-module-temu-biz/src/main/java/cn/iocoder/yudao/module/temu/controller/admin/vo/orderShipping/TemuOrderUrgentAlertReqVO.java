package cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - Temu订单紧急告警 Request VO")
@Data
public class TemuOrderUrgentAlertReqVO {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "物流单号")
    private String trackingNumber;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "是否小于24小时")
    private Boolean isLessThan24h;
} 