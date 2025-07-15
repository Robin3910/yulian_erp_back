package cn.iocoder.yudao.module.temu.service.orderBatch.impl;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderBatchCategoryDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderBatchDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderBatchRelationDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderBatchCategoryMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderBatchMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderBatchRelationMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.enums.TemuOrderStatusEnum;
import cn.iocoder.yudao.module.temu.service.orderBatch.ITemuOrderBatchCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.ORDER_BATCH_EXISTS;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class TemuOrderBatchCategoryService implements ITemuOrderBatchCategoryService {

    @Resource
    private TemuOrderBatchCategoryMapper temuOrderBatchCategoryMapper;

    @Resource
    private TemuOrderBatchMapper temuOrderBatchMapper;

    @Resource
    private TemuOrderBatchRelationMapper temuOrderBatchRelationMapper;

    @Resource
    private TemuOrderMapper temuOrderMapper;

    @Resource
    private ConfigApi configApi;

    public void processBatchAndRelations(Map<String, List<Long>> batchCategoryOrderMap) {
    // 从配置中获取是否开启可动态配置批次生成时间段
        String dynamicBatchSchedule = configApi.getConfigValueByKey("temu_dynamic_batch_schedule");
        log.info("是否开启可动态配置批次生成时间段的配置值: {}", dynamicBatchSchedule);
        boolean flag = false; // 默认值
        if (StrUtil.isNotEmpty(dynamicBatchSchedule)) {
            try {
                flag = Boolean.parseBoolean(dynamicBatchSchedule);
            } catch (Exception e) {
                log.warn("是否开启可动态配置批次生成时间段的配置格式错误，使用默认值");
            }
        }
        if(flag){
            // 动态配置批次生成时间段
            buildDynamicBatchSchedule(batchCategoryOrderMap);
        }else{
            // 固定：上午时段 (0:00 - 12:00) 下午时段 (12:00 - 24:00) 批次生成
            toProcessBatchAndRelations(batchCategoryOrderMap);
        }

    }

    /**
     * 根据categoryOrderMap查询对应的batchCategoryId，并重新组织数据结构
     *
     * @param categoryOrderMap 分类ID和订单ID列表的映射
     * @return batchCategoryId和订单ID列表的映射
     */
    public Map<String, List<Long>> getBatchCategoryOrderMap(Map<String, List<Long>> categoryOrderMap) {
        if (categoryOrderMap == null || categoryOrderMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // 获取所有categoryId
        List<String> categoryIds = new ArrayList<>(categoryOrderMap.keySet());

        // 查询对应的batchCategoryId
        List<TemuOrderBatchCategoryDO> batchCategories = temuOrderBatchCategoryMapper.selectByCategoryIds(categoryIds);

        // 构建batchCategoryId到categoryId列表的映射
        Map<String, List<String>> batchCategoryToCategoriesMap = new HashMap<>();
        for (TemuOrderBatchCategoryDO batchCategory : batchCategories) {
            batchCategoryToCategoriesMap.computeIfAbsent(batchCategory.getBatchCategoryId(), k -> new ArrayList<>())
                    .add(batchCategory.getCategoryId());
        }

        // 处理不存在的categoryId，为它们创建新的记录
        List<TemuOrderBatchCategoryDO> newBatchCategories = new ArrayList<>();
        for (String categoryId : categoryIds) {
            boolean exists = batchCategories.stream()
                    .anyMatch(bc -> bc.getCategoryId().equals(categoryId));
            if (!exists) {
                // 创建新的批次分类记录
                TemuOrderBatchCategoryDO newBatchCategory = new TemuOrderBatchCategoryDO();
                newBatchCategory.setCategoryId(categoryId);
                // 生成新的batchCategoryId
                newBatchCategory.setBatchCategoryId(UUID.randomUUID().toString());
                newBatchCategories.add(newBatchCategory);
                // 将新创建的记录添加到映射中
                batchCategoryToCategoriesMap
                        .computeIfAbsent(newBatchCategory.getBatchCategoryId(), k -> new ArrayList<>())
                        .add(categoryId);
            }
        }

        // 批量插入新的批次分类记录
        if (!newBatchCategories.isEmpty()) {
            temuOrderBatchCategoryMapper.insertBatch(newBatchCategories);
        }

        // 重新组织数据结构，合并相同batchCategoryId下的订单
        Map<String, List<Long>> batchCategoryOrderMap = new HashMap<>();
        batchCategoryToCategoriesMap.forEach((batchCategoryId, categoryIdList) -> {
            List<Long> allOrderIds = new ArrayList<>();
            for (String categoryId : categoryIdList) {
                List<Long> orderIds = categoryOrderMap.get(categoryId);
                if (orderIds != null) {
                    allOrderIds.addAll(orderIds);
                }
            }
            if (!allOrderIds.isEmpty()) {
                batchCategoryOrderMap.put(batchCategoryId, allOrderIds);
            }
        });

        return batchCategoryOrderMap;
    }

    /**
     * 处理批次和批次关系
     *
     * @param batchCategoryOrderMap batchCategoryId和订单ID列表的映射
     */
    @Transactional
    public void toProcessBatchAndRelations(Map<String, List<Long>> batchCategoryOrderMap) {
        if (batchCategoryOrderMap == null || batchCategoryOrderMap.isEmpty()) {
            return;
        }

        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime periodStart, periodEnd;
        String periodStr;

        if (now.getHour() < 12) {
            // 上午时段 (0:00 - 12:00)
            periodStart = today.atStartOfDay();
            periodEnd = today.atTime(12, 0, 0);
            periodStr = "上午";
        } else {
            // 下午时段 (12:00 - 24:00)
            periodStart = today.atTime(12, 0, 0);
            periodEnd = today.atTime(23, 59, 59);
            periodStr = "下午";
        }

        // 遍历每个batchCategoryId
        batchCategoryOrderMap.forEach((batchCategoryId, orderIds) -> {
            // 1. 查询当天最新的批次
            TemuOrderBatchDO latestBatch = temuOrderBatchMapper.selectLatestBatchByCategoryId(batchCategoryId,
                    periodStart, periodEnd);

            Long batchId;
            if (latestBatch != null) {
                // 如果存在批次，使用现有的batchId
                batchId = latestBatch.getId();
            } else {
                // 查询当前时段已有批次数量
                List<TemuOrderBatchDO> periodBatchList = temuOrderBatchMapper.selectByCreateTimeRange(periodStart, periodEnd);
                
                // 将批次列表按上午下午分组统计
                int morningCount = 0;
                int afternoonCount = 0;
                
                for (TemuOrderBatchDO batch : periodBatchList) {
                    LocalTime batchTime = batch.getCreateTime().toLocalTime();
                    if (batchTime.isBefore(LocalTime.of(12, 0))) {
                        morningCount++;
                    } else {
                        afternoonCount++;
                    }
                }
                
                // 根据当前时段确定批次数量
                int batchCount;
                if (now.getHour() < 12) {
                    // 上午时段
                    batchCount = morningCount + 1;
                } else {
                    // 下午时段
                    batchCount = afternoonCount + 1;
                }

                // 创建新批次
                TemuOrderBatchDO newBatch = new TemuOrderBatchDO();
                newBatch.setBatchCategoryId(batchCategoryId);
                String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String batchNo = String.format("%s%s%02d", dateStr, periodStr, batchCount);
                newBatch.setBatchNo(batchNo);
                newBatch.setStatus(0);
                temuOrderBatchMapper.insert(newBatch);
                batchId = newBatch.getId();

            }

            // 2. 创建批次关系记录
            List<TemuOrderBatchRelationDO> relations = orderIds.stream()
                    .map(orderId -> {
                        TemuOrderBatchRelationDO relation = new TemuOrderBatchRelationDO();
                        relation.setBatchId(batchId);
                        relation.setOrderId(orderId);
                        return relation;
                    })
                    .collect(Collectors.toList());

            // 检查订单是否已经被批次化
            // 检查订单状态是否存在历史批次中
            for (Long orderId : orderIds) {
                Long count = temuOrderBatchRelationMapper.selectCount("order_id", orderId);
                if (count > 0) {
                    // 查询订单信息获取定制SKU
                    TemuOrderDO order = temuOrderMapper.selectById(orderId);
                    String customSku = order != null ? order.getCustomSku() : "未知";
                    throw exception(ORDER_BATCH_EXISTS, String.format("订单ID: %d, 定制SKU: %s 已存在批次中", orderId, customSku));
                }
            }

            // 3. 批量插入关系记录
            boolean insertSuccess = temuOrderBatchRelationMapper.insertBatch(relations);

            // 4. 如果成功插入关系记录，更新订单状态
            if (insertSuccess) {
                // 批量更新订单状态为待生产
                List<TemuOrderDO> orders = orderIds.stream()
                        .map(orderId -> {
                            TemuOrderDO order = new TemuOrderDO();
                            order.setId(orderId);
                            order.setOrderStatus(TemuOrderStatusEnum.IN_PRODUCTION);
                            return order;
                        })
                        .collect(Collectors.toList());
                temuOrderMapper.updateBatch(orders);
            }
        });
    }

    /**
     * 自定义配置自动生成批次时间段
     * 说明：
     *
     * @param batchCategoryOrderMap
     */
    @Transactional
    public void buildDynamicBatchSchedule(Map<String, List<Long>> batchCategoryOrderMap) {
        if (batchCategoryOrderMap == null || batchCategoryOrderMap.isEmpty()) {
            return;
        }

        // 从配置获取时间段定义（配置格式："开始小时-结束小时"，使用小数表示半小时）
        String morningConfig = configApi.getConfigValueByKey("temu.batch.morning.period"); // 默认 "0-11"
        String afternoonConfig = configApi.getConfigValueByKey("temu.batch.afternoon.period"); // 默认 "11-17.5"

        // 解析动态配置（带默认值）
        TimePeriod morningPeriod = parsePeriodConfig(morningConfig, 0.0, 11.0);
        TimePeriod afternoonPeriod = parsePeriodConfig(afternoonConfig, 11.0, 17.5);

        // 获取当前时间并确定目标时间段
        LocalDateTime now = LocalDateTime.now();
        TimePeriod targetPeriod = calculateTargetPeriod(now, morningPeriod, afternoonPeriod);

        // 2. 确定批次日期和时段
        LocalDate batchDate = now.toLocalDate();
        // 夜间时段需要切换到第二天的批次
        if (isNightShift(now, afternoonPeriod)) {
            batchDate = batchDate.plusDays(1);
        }

        // 构建时间段边界
        LocalDateTime periodStart = LocalDateTime.of(batchDate, targetPeriod.getStartTime());
        LocalDateTime periodEnd = LocalDateTime.of(batchDate, targetPeriod.getEndTime());

        // 3. 处理批次
        batchCategoryOrderMap.forEach((batchCategoryId, orderIds) -> {
            // 查询指定时间段内的最新批次
            TemuOrderBatchDO latestBatch = temuOrderBatchMapper.selectLatestBatchByCategoryId(
                    batchCategoryId, periodStart, periodEnd
            );

            Long batchId;
            if (latestBatch != null) {
                batchId = latestBatch.getId();
            } else {
                // 查询当前时段已有批次数量
                List<TemuOrderBatchDO> periodBatchList = temuOrderBatchMapper.selectByCreateTimeRange(periodStart, periodEnd);
                
                // 将批次列表按上午下午分组统计
                int morningCount = 0;
                int afternoonCount = 0;
                
                for (TemuOrderBatchDO batch : periodBatchList) {
                    LocalTime batchTime = batch.getCreateTime().toLocalTime();
                    if (batchTime.isBefore(LocalTime.of(12, 0))) {
                        morningCount++;
                    } else {
                        afternoonCount++;
                    }
                }
                
                // 根据当前时段确定批次数量
                int batchCount;
                if (now.getHour() < 12) {
                    // 上午时段
                    batchCount = morningCount + 1;
                } else {
                    // 下午时段
                    batchCount = afternoonCount + 1;
                }

                // 创建新批次（使用当天日期+递增编号）
                TemuOrderBatchDO newBatch = new TemuOrderBatchDO();
                newBatch.setBatchCategoryId(batchCategoryId);
                // 设置批次号为yyyyMMdd上午或yyyyMMdd下午格式
                String dateStr = DateTime.now().toString("yyyyMMdd");
                String periodStr = (now.getHour() < 12) ? "上午" : "下午";
                String batchNo = String.format("%s%s%02d", dateStr, periodStr, batchCount);
                newBatch.setBatchNo(batchNo);
                newBatch.setStatus(0);

                //如果是17:30到凌晨0点下的单 那么生成批次的日期+1 如今天是6月28日 那么批次的创建时间会被设置成6月29日0点0分01秒
                if (isNightShift(now, afternoonPeriod)) {
                    // 将periodStart增加1秒（解决数据库时间精度进位问题）
                    LocalDateTime createTime = periodStart.plusSeconds(1);
                    newBatch.setCreateTime(createTime); // 关键修改
                }
                temuOrderBatchMapper.insert(newBatch);
                batchId = newBatch.getId();
            }

            // 2. 创建批次关系记录
            List<TemuOrderBatchRelationDO> relations = orderIds.stream()
                    .map(orderId -> {
                        TemuOrderBatchRelationDO relation = new TemuOrderBatchRelationDO();
                        relation.setBatchId(batchId);
                        relation.setOrderId(orderId);
                        return relation;
                    })
                    .collect(Collectors.toList());

            // 检查订单是否已经被批次化
            // 检查订单状态是否存在历史批次中
            for (Long orderId : orderIds) {
                Long count = temuOrderBatchRelationMapper.selectCount("order_id", orderId);
                if (count > 0) {
                    // 查询订单信息获取定制SKU
                    TemuOrderDO order = temuOrderMapper.selectById(orderId);
                    String customSku = order != null ? order.getCustomSku() : "未知";
                    throw exception(ORDER_BATCH_EXISTS, String.format("订单ID: %d, 定制SKU: %s 已存在批次中", orderId, customSku));
                }
            }

            // 3. 批量插入关系记录
            boolean insertSuccess = temuOrderBatchRelationMapper.insertBatch(relations);

            // 4. 如果成功插入关系记录，更新订单状态
            if (insertSuccess) {
                // 批量更新订单状态为待生产
                List<TemuOrderDO> orders = orderIds.stream()
                        .map(orderId -> {
                            TemuOrderDO order = new TemuOrderDO();
                            order.setId(orderId);
                            order.setOrderStatus(TemuOrderStatusEnum.IN_PRODUCTION);
                            return order;
                        })
                        .collect(Collectors.toList());
                temuOrderMapper.updateBatch(orders);
            }
        });

        // 获取今日起止时间
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // 拉取今日所有批次数据到集合
        List<TemuOrderBatchDO> todayBatchList = temuOrderBatchMapper.selectByCreateTimeRange(startOfDay, endOfDay);

        // 现在 todayBatchList 就是今日所有批次数据的集合，可以后续分组、编号等操作
        // 例如输出数量
        System.out.println("今日批次数量：" + todayBatchList.size());
    }

    // 时间段配置解析
    private TimePeriod parsePeriodConfig(String config, double defaultStart, double defaultEnd) {
        double start = defaultStart;
        double end = defaultEnd;

        if (config != null && !config.isEmpty()) {
            String[] parts = config.split("-");
            if (parts.length == 2) {
                try {
                    start = Double.parseDouble(parts[0].trim());
                    end = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException e) {
                    log.warn("批次时间段配置格式错误，使用默认值", e);
                }
            }
        }

        return new TimePeriod(start, end);
    }

    // 时间片段对象
    private static class TimePeriod {
        private final LocalTime startTime;
        private final LocalTime endTime;

        public TimePeriod(double startHour, double endHour) {
            this.startTime = toLocalTime(startHour);
            this.endTime = toLocalTime(endHour);
        }

        private LocalTime toLocalTime(double hourValue) {
            int hour = (int) hourValue;
            int minute = (int) ((hourValue - hour) * 60);
            return LocalTime.of(hour, minute);
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }
    }

    // 判断是否夜间时段（需次日批次）
    private boolean isNightShift(LocalDateTime now, TimePeriod afternoonPeriod) {
        return now.toLocalTime().isAfter(afternoonPeriod.getEndTime());
    }

    // 确定当前应使用的时间段
    private TimePeriod calculateTargetPeriod(LocalDateTime now,
                                             TimePeriod morningPeriod,
                                             TimePeriod afternoonPeriod) {
        LocalTime currentTime = now.toLocalTime();

        if (!currentTime.isBefore(morningPeriod.getStartTime()) &&
                currentTime.isBefore(morningPeriod.getEndTime())) {
            // 上午时段 (0-11点)
            return morningPeriod;
        } else if (!currentTime.isBefore(afternoonPeriod.getStartTime()) &&
                currentTime.isBefore(afternoonPeriod.getEndTime())) {
            // 下午时段 (11-17.5点)
            return afternoonPeriod;
        } else {
            // 夜间时段 (17.5点-0点) 使用次日上午
            return morningPeriod;
        }
    }
}