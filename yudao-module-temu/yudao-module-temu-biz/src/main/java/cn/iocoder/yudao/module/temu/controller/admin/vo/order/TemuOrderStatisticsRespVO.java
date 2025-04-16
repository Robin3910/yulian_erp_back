package cn.iocoder.yudao.module.temu.controller.admin.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TemuOrderStatisticsRespVO {
	@Schema(description = "订单总金额")
	private BigDecimal totalPrice   ;
}
