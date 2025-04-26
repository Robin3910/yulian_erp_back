package cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 订单批次分页VO
 */
@Data
public class TemuOrderBatchPageVO extends PageParam {
	@Schema(description = "批次编号")
	private String batchNo;
	@Schema(description = "订单状态")
	private Integer status;
	@Schema(description = "创建时间")
	@DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
	private LocalDateTime[] createTime;
	@Schema(description = "是否按批次分组")
	private Boolean groupByBatch;
}
