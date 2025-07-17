package cn.iocoder.yudao.module.temu.service.orderStatistics;

import cn.iocoder.yudao.module.temu.controller.admin.vo.orderStatistics.OrderStatisticsReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderStatistics.OrderStatisticsRespVO;
import cn.iocoder.yudao.module.temu.dal.mysql.OrderStatisticsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class orderStatisticsServiceImpl implements IOrderStatisticsService{
    @Autowired
    private OrderStatisticsMapper orderStatisticsMapper;

    /**
     * 订单统计主方法，根据统计粒度（日/周/月）返回统计结果
     * @param reqVO 请求参数，包括店铺ID、起止日期、统计粒度
     * @return 统计结果，包含时间点、订单数量、汇总信息等
     */
    @Override
    public OrderStatisticsRespVO getOrderStatistics(OrderStatisticsReqVO reqVO) {
        List<Map<String, Object>> dbResult;
        String granularity = reqVO.getGranularity();
        List<Long> shopIds = reqVO.getShopIds();
        String startDate = reqVO.getStartDate();
        String endDate = reqVO.getEndDate();
        // 校验店铺ID参数
        if (shopIds == null || shopIds.isEmpty()) return emptyResp(granularity);
        // 根据粒度调用不同SQL
        if ("day".equals(granularity)) {
            dbResult = orderStatisticsMapper.selectOrderCountByDay(shopIds, startDate, endDate);
        } else if ("week".equals(granularity)) {
            dbResult = orderStatisticsMapper.selectOrderCountByWeek(shopIds, startDate, endDate);
        } else if ("month".equals(granularity)) {
            dbResult = orderStatisticsMapper.selectOrderCountByMonth(shopIds, startDate, endDate);
        } else {
            return emptyResp(granularity);
        }
        // 组装数据库返回的时间点和订单数
        Map<String, Integer> dataMap = new LinkedHashMap<>();
        for (Map<String, Object> row : dbResult) {
            String timePoint = String.valueOf(row.get("time_point"));
            Integer count = ((Number)row.get("order_count")).intValue();
            dataMap.put(timePoint, count);
        }
        // 补全所有时间点（如某天无订单，补0）
        List<String> timePoints = buildTimePoints(startDate, endDate, granularity);
        List<Integer> values = new ArrayList<>();
        for (String tp : timePoints) {
            values.add(dataMap.getOrDefault(tp, 0));
        }
        // 计算汇总信息
        int total = values.stream().mapToInt(Integer::intValue).sum(); // 总订单数
        int max = values.stream().mapToInt(Integer::intValue).max().orElse(0); // 单日最大
        int min = values.stream().mapToInt(Integer::intValue).min().orElse(0); // 单日最小
        double avg = values.isEmpty() ? 0 : (double)total / values.size(); // 日均订单
        double roundedAvg = BigDecimal.valueOf(avg)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        // 组装返回对象
        OrderStatisticsRespVO resp = new OrderStatisticsRespVO();
        resp.setTimePoints(timePoints);
        resp.setValues(values);
        OrderStatisticsRespVO.Summary summary = new OrderStatisticsRespVO.Summary();
        summary.setTotalOrders(total);
        summary.setAverageDaily(roundedAvg);
        summary.setMaxOrders(max);
        summary.setMinOrders(min);
        resp.setSummary(summary);
        resp.setGranularity(granularity);
        return resp;
    }

    /**
     * 构建所有时间点（如所有天、周、月），用于补全无订单的时间点
     */
    private List<String> buildTimePoints(String start, String end, String granularity) {
        List<String> result = new ArrayList<>();
        LocalDate s = LocalDate.parse(start);
        LocalDate e = LocalDate.parse(end);
        if ("day".equals(granularity)) {
            for (LocalDate d = s; !d.isAfter(e); d = d.plusDays(1)) {
                result.add(d.toString());
            }
        } else if ("week".equals(granularity)) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-ww");
            for (LocalDate d = s; !d.isAfter(e); d = d.plusWeeks(1)) {
                result.add(d.format(fmt));
            }
        } else if ("month".equals(granularity)) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
            for (LocalDate d = s.withDayOfMonth(1); !d.isAfter(e); d = d.plusMonths(1)) {
                result.add(d.format(fmt));
            }
        }
        return result;
    }

    /**
     * 返回空的统计结果（用于参数校验失败等场景）
     */
    private OrderStatisticsRespVO emptyResp(String granularity) {
        OrderStatisticsRespVO resp = new OrderStatisticsRespVO();
        resp.setTimePoints(Collections.emptyList());
        resp.setValues(Collections.emptyList());
        OrderStatisticsRespVO.Summary summary = new OrderStatisticsRespVO.Summary();
        summary.setTotalOrders(0);
        summary.setAverageDaily(0.0);
        summary.setMaxOrders(0);
        summary.setMinOrders(0);
        resp.setSummary(summary);
        resp.setGranularity(granularity);
        return resp;
    }
}
