package cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class TemuOrderBatchUpdateStatusByTaskVO {
	@NotNull(message = "任务Id不可以为空")
	@Schema(description = "任务Id", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long taskId;
	@NotNull(message = "批次id不能为空")
	@Schema(description = "批次id", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long id;
}
