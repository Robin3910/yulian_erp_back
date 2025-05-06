package cn.iocoder.yudao.module.temu.controller.admin.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "Temu订单保存请求VO")
@AllArgsConstructor
@NoArgsConstructor
public class TemuOrderSaveOrderRemarkReqVO {
	@Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long orderId;
	@Schema(description = "订单备注", requiredMode = Schema.RequiredMode.REQUIRED)
	private String remark;
}
