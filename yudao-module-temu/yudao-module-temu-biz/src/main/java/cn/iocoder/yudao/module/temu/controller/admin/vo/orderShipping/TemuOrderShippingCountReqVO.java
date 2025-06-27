package cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "Temu管理 - 加急未发货订单总数查询 Request VO")
@Data
public class TemuOrderShippingCountReqVO {
    
    @Schema(description = "创建时间开始", example = "2024-05-30")
    private LocalDate createTimeStart;
    
    @Schema(description = "创建时间结束", example = "2024-05-30")
    private LocalDate createTimeEnd;
} 