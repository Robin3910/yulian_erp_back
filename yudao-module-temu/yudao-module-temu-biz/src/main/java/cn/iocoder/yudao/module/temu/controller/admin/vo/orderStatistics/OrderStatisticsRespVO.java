package cn.iocoder.yudao.module.temu.controller.admin.vo.orderStatistics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
public class OrderStatisticsRespVO {
    @Schema(description = "时间点数组")
    private List<String> timePoints;

    @Schema(description = "订单数量数组")
    private List<Integer> values;

    @Schema(description = "汇总信息")
    private Summary summary;

    @Schema(description = "统计粒度，day/week/month")
    private String granularity;

    @Data
    public static class Summary {
        @Schema(description = "总订单数")
        private Integer totalOrders;
        @Schema(description = "日均订单")
        private Double averageDaily;
        @Schema(description = "单日最高订单")
        private Integer maxOrders;
        @Schema(description = "单日最低订单")
        private Integer minOrders;
    }
} 