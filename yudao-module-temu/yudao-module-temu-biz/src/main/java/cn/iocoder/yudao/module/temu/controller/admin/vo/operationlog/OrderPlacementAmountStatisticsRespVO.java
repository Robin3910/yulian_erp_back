package cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "批量下单记录总金额统计 Response VO")
public class OrderPlacementAmountStatisticsRespVO {
    
    @Schema(description = "总金额")
    private BigDecimal totalPrice;
} 