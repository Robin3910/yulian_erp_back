package cn.iocoder.yudao.module.temu.controller.admin.vo.workertask;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 工作人员任务记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemuWorkerTaskPageReqVO extends PageParam {

    @Schema(description = "工作人员ID，关联system_users表id", example = "21754")
    private Long workerId;

    @Schema(description = "工作人员姓名，关联system_users表nickname", example = "赵六")
    private String workerName;

    @Schema(description = "任务类型 1:作图 2:生产 3:发货", example = "1")
    private Byte taskType;

    @Schema(description = "任务状态 0:待处理 1:已完成 2:已取消", example = "1")
    private Byte taskStatus;

    @Schema(description = "关联订单ID，对应temu_order表id", example = "1660")
    private Long orderId;

    @Schema(description = "订单编号，对应temu_order表order_no")
    private String orderNo;

    @Schema(description = "批次订单ID，对应temu_order_batch_task表batch_order_id", example = "6170")
    private Long batchOrderId;

    @Schema(description = "定制SKU，对应temu_order表custom_sku")
    private String customSku;

    @Schema(description = "处理SKU数量，对应temu_order表quantity")
    private Integer skuQuantity;

    @Schema(description = "任务完成时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] taskCompleteTime;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "店铺ID，对应temu_order表shop_id")
    private Long shopId;

    @Schema(description = "店铺名称，对应temu_shop表shop_name")
    private String shopName;

}