package cn.iocoder.yudao.module.temu.controller.admin.vo.orderStatistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
public class OrderStatisticsReqVO {
    @Schema(description = "店铺ID数组")
    private List<Long> shopIds;

    @Schema(description = "开始日期，格式：YYYY-MM-DD")
    private String startDate;

    @Schema(description = "结束日期，格式：YYYY-MM-DD")
    private String endDate;

    @Schema(description = "统计粒度，day/week/month")
    private String granularity;
} 