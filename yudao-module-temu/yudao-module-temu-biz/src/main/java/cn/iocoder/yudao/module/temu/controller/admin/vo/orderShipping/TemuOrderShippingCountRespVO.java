package cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Temu管理 - 加急未发货订单总数查询 Response VO")
@Data
public class TemuOrderShippingCountRespVO {
    
    @Schema(description = "加急未发货订单总数", example = "123")
    private Long total;
} 