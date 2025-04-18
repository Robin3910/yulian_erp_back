package cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch;

import lombok.*;

import javax.validation.constraints.NotEmpty;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class TemuOrderBatchCreateVO {
	@NotEmpty(message = "订单id不能为空")
	private List<Long> orderIds;
}
