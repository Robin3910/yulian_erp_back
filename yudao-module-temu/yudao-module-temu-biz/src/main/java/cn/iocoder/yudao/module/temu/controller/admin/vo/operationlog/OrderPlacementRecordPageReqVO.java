package cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Data
public class OrderPlacementRecordPageReqVO extends PageParam {
    @Schema(description = "订单编号")
    private String orderNo;
    @Schema(description = "SKU编号")
    private String sku;
    @Schema(description = "SKC编号")
    private String skc;
    @Schema(description = "定制SKU编号")
    private String customSku;
    @Schema(description = "类目ID")
    private List<Long> categoryId;
    @Schema(description = "是否返单")
    private Integer isReturnOrder;
    @Schema(description = "下单时间区间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] operationTime;
    @Schema(description = "店铺ID")
    private List<Long> shopId;
    @Schema(description = "操作人")
    private String operator;
} 