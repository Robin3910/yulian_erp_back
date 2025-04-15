package cn.iocoder.yudao.module.temu.controller.admin.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "Temu订单批量下单请求VO")

@Data
public class TemuOrderBatchOrderReqVO {
	@Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "订单ID不能为空")
	private Long id;
	
	@Schema(description = "订单数量", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "订单数量不能为空")
	private Integer quantity;
	
}
