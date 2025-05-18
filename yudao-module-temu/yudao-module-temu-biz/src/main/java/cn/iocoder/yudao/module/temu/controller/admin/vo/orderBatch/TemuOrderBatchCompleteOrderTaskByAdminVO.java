package cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemuOrderBatchCompleteOrderTaskByAdminVO {
	@NotNull(message = "批次id不能为空")
	@Schema(description = "批次id", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long id;
	@NotNull(message = "订单id不能为空")
	@Schema(description = "订单id", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long orderId;
	@NotNull(message = "任务类型不能为空")
	@Schema(description = "任务类型", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer taskType;
	
	
}
