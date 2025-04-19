package cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class TemuOrderBatchUpdateFileVO {
	@NotNull(message = "批次id不能为空")
	@Schema(description = "批次id",requiredMode = Schema.RequiredMode.REQUIRED)
	private Long id;
	
	@NotNull(message = "文件地址不能为空")
	@Schema(description = "文件地址",requiredMode = Schema.RequiredMode.REQUIRED)
	private String fileUrl;
}
