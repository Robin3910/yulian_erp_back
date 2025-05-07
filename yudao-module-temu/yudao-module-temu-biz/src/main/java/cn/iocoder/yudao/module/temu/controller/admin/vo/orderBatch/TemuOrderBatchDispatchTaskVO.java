package cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Temu订单批量分配任务")
public class TemuOrderBatchDispatchTaskVO {
	@NotNull
	@Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long[] orderIds;
	@NotNull
	@Schema(description = "作图人员ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long artStaffUserId;
	@NotNull
	@Schema(description = "生产人员ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long productionStaffUserId;
}
