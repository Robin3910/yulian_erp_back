package cn.iocoder.yudao.module.temu.dal.mysql;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderStatisticsMapper {
    // 按日统计
    List<Map<String, Object>> selectOrderCountByDay(@Param("shopIds") List<Long> shopIds,
            @Param("categoryIds") List<String> categoryIds,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    // 按周统计
    List<Map<String, Object>> selectOrderCountByWeek(@Param("shopIds") List<Long> shopIds,
            @Param("categoryIds") List<String> categoryIds,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    // 按月统计
    List<Map<String, Object>> selectOrderCountByMonth(@Param("shopIds") List<Long> shopIds,
            @Param("categoryIds") List<String> categoryIds,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    // 按日统计返单
    List<Map<String, Object>> selectReturnOrderCountByDay(@Param("shopIds") List<Long> shopIds,
            @Param("categoryIds") List<String> categoryIds,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    // 按周统计返单
    List<Map<String, Object>> selectReturnOrderCountByWeek(@Param("shopIds") List<Long> shopIds,
            @Param("categoryIds") List<String> categoryIds,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    // 按月统计返单
    List<Map<String, Object>> selectReturnOrderCountByMonth(@Param("shopIds") List<Long> shopIds,
            @Param("categoryIds") List<String> categoryIds,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    // 获取所有可选的类目列表
    List<Map<String, Object>> selectAllCategories();
}