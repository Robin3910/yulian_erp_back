package cn.iocoder.yudao.module.temu.controller.admin.vo.workertask;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

@Schema(description = "管理后台 - 工作人员任务记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class TemuWorkerTaskRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "600")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "工作人员ID，关联system_users表id", requiredMode = Schema.RequiredMode.REQUIRED, example = "21754")
    @ExcelProperty("工作人员ID，关联system_users表id")
    private Long workerId;

    @Schema(description = "工作人员姓名，关联system_users表nickname", requiredMode = Schema.RequiredMode.REQUIRED, example = "赵六")
    @ExcelProperty("工作人员姓名，关联system_users表nickname")
    private String workerName;

    @Schema(description = "任务类型 1:作图 2:生产 3:发货", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("任务类型 1:作图 2:生产 3:发货")
    private Byte taskType;

    @Schema(description = "任务状态 0:待处理 1:已完成 2:已取消", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("任务状态 0:待处理 1:已完成 2:已取消")
    private Byte taskStatus;

    @Schema(description = "关联订单ID，对应temu_order表id", example = "1660")
    @ExcelProperty("关联订单ID，对应temu_order表id")
    private Long orderId;

    @Schema(description = "订单编号，对应temu_order表order_no")
    @ExcelProperty("订单编号，对应temu_order表order_no")
    private String orderNo;

    @Schema(description = "批次订单ID，对应temu_order_batch_task表batch_order_id", example = "6170")
    @ExcelProperty("批次订单ID，对应temu_order_batch_task表batch_order_id")
    private Long batchOrderId;

    @Schema(description = "定制SKU，对应temu_order表custom_sku")
    @ExcelProperty("定制SKU，对应temu_order表custom_sku")
    private String customSku;

    @Schema(description = "处理SKU数量，对应temu_order表quantity")
    @ExcelProperty("处理SKU数量，对应temu_order表quantity")
    private Integer skuQuantity;

    @Schema(description = "任务完成时间")
    @ExcelProperty("任务完成时间")
    private LocalDateTime taskCompleteTime;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "店铺ID，对应temu_order表shop_id")
    @ExcelProperty("店铺ID")
    private Long shopId;

    @Schema(description = "店铺名称，对应temu_shop表shop_name")
    @ExcelProperty("店铺名称")
    private String shopName;

}