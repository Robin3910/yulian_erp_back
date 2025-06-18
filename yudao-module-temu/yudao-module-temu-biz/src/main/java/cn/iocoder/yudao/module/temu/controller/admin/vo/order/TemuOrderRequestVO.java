package cn.iocoder.yudao.module.temu.controller.admin.vo.order;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import javax.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemuOrderRequestVO extends PageParam {
	// 店铺id
	private Long[] shopId;
	private String sku;
	private String skc;
	private String customSku;
	private String orderStatus;
	private Long[] categoryId;
	private Integer hasCategory;
	@DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
	private LocalDateTime[] bookingTime;
	private String orderNo;

	@Schema(description = "是否为返单")
	private Integer isReturnOrder;
}
