package cn.iocoder.yudao.module.temu.service.orderBatch.impl;

import cn.hutool.core.date.DateTime;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.ORDER_BATCH_EXISTS;

@Service
public class TemuOrderBatchCategoryService implements ITemuOrderBatchCategoryService {

    @Resource
    private TemuOrderBatchCategoryMapper temuOrderBatchCategoryMapper;

    @Resource
    private TemuOrderBatchMapper temuOrderBatchMapper;

    @Resource
    private TemuOrderBatchRelationMapper temuOrderBatchRelationMapper;

    @Resource
    private TemuOrderMapper temuOrderMapper;

    /**
     * 根据categoryOrderMap查询对应的batchCategoryId，并重新组织数据结构
     *
     * @param categoryOrderMap 分类ID和订单ID列表的映射
     * @return batchCategoryId和订单ID列表的映射
     */
    @Override
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
    public void processBatchAndRelations(Map<String, List<Long>> batchCategoryOrderMap) {
        if (batchCategoryOrderMap == null || batchCategoryOrderMap.isEmpty()) {
            return;
        }

        // 获取今天的开始时间和结束时间
        LocalDateTime todayStart = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.now().with(LocalTime.MAX);

        // 遍历每个batchCategoryId
        batchCategoryOrderMap.forEach((batchCategoryId, orderIds) -> {
            // 1. 查询当天最新的批次
            TemuOrderBatchDO latestBatch = temuOrderBatchMapper.selectLatestBatchByCategoryId(batchCategoryId,
                    todayStart, todayEnd);

            Long batchId;
            if (latestBatch != null) {
                // 如果存在批次，使用现有的batchId
                batchId = latestBatch.getId();
            } else {
                // 如果不存在批次，创建新的批次
                TemuOrderBatchDO newBatch = new TemuOrderBatchDO();
                newBatch.setBatchCategoryId(batchCategoryId);
                // 设置批次号（年月日时分秒）
                newBatch.setBatchNo(DateTime.now().toString("yyyyMMddHHmmss"));
                // 设置状态为待生产
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
                    throw exception(ORDER_BATCH_EXISTS);
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
}
