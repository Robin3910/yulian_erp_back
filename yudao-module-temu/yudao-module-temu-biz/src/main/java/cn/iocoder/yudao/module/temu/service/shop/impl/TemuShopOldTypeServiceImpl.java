package cn.iocoder.yudao.module.temu.service.shop.impl;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.*;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopOldTypeSkcDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopOldTypeSkcMapper;
import cn.iocoder.yudao.module.temu.service.pdf.AsyncPdfProcessService;
import cn.iocoder.yudao.module.temu.service.shop.TemuShopOldTypeService;
import cn.iocoder.yudao.module.temu.utils.pdf.PdfToImageUtil;
import cn.iocoder.yudao.module.temu.service.oss.TemuOssService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TemuShopOldTypeServiceImpl implements TemuShopOldTypeService {

    private final TemuShopOldTypeSkcMapper temuShopOldTypeSkcMapper;
    private final TemuOrderMapper temuOrderMapper;
    private final TemuOssService temuOssService;
    private final AsyncPdfProcessService asyncPdfProcessService;

    // 一次转换 重复使用 PDF转换图片URL结果
    // 缓存PDF转图片结果 避免同一店铺同一合规单类型下的合规单图片url重复转换 浪费资源
    private final Map<String, String> oldTypeUrlCache = new ConcurrentHashMap<>();

    // 批量保存合规单SKC
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchSaveOldTypeSkc(List<TemuShopBatchSaveSkcReqVO> saveSkcReqVOList) {
        if (CollUtil.isEmpty(saveSkcReqVOList)) {
            return 0;
        }
        // 遍历处理每个请求对象
        List<TemuShopOldTypeSkcDO> insertList = new ArrayList<>();
        List<TemuShopOldTypeSkcDO> updateList = new ArrayList<>();

        for (TemuShopBatchSaveSkcReqVO reqVO : saveSkcReqVOList) {
            // 检查记录是否已存在（包括已删除的记录）
            TemuShopOldTypeSkcDO existingRecord = temuShopOldTypeSkcMapper.selectByShopIdAndSkcWithoutDeleted(
                    reqVO.getShopId(), reqVO.getSkc());
            // 处理当前记录
            TemuShopOldTypeSkcDO oldTypeSkcDO = processOldTypeRecord(
                    reqVO.getShopId(),
                    reqVO.getSkc(),
                    reqVO.getOldType(),
                    reqVO.getOldTypeUrl());
            // 如果记录存在（无论是否删除），都执行更新操作
            if (existingRecord != null) {
                oldTypeSkcDO.setId(existingRecord.getId());
                // 恢复删除标记（如果之前是删除状态）
                oldTypeSkcDO.setDeleted(false);
                oldTypeSkcDO.setCreateTime(existingRecord.getCreateTime());
                updateList.add(oldTypeSkcDO);
            } else {
                insertList.add(oldTypeSkcDO);
            }

            // 处理近两天内的订单PDF合并
            processRecentOrders(reqVO.getShopId(), reqVO.getSkc(), reqVO.getOldTypeUrl(),
                    oldTypeSkcDO.getOldTypeImageUrl());
        }
        // 执行批量插入操作
        if (!insertList.isEmpty()) {
            temuShopOldTypeSkcMapper.insertBatch(insertList);
        }
        // 执行批量更新操作
        for (TemuShopOldTypeSkcDO updateObj : updateList) {
            temuShopOldTypeSkcMapper.updateByIdWithoutDeleted(updateObj);
        }
        return insertList.size() + updateList.size();
    }

    /**
     * 处理近两天内的订单PDF合并
     * 
     * @param shopId     店铺ID
     * @param skc        SKC编号
     * @param oldTypeUrl 合规单URL
     */
    private void processRecentOrders(Long shopId, String skc, String oldTypeUrl, String oldTypeImageUrl) {
        try {
            // 计算两天前的时间
            LocalDateTime twoDaysAgo = LocalDate.now().minusDays(2).atStartOfDay();

            // 查询符合条件的订单
            LambdaQueryWrapper<TemuOrderDO> queryWrapper = new LambdaQueryWrapper<TemuOrderDO>()
                    .eq(TemuOrderDO::getShopId, shopId)
                    .eq(TemuOrderDO::getSkc, skc)
                    .ge(TemuOrderDO::getBookingTime, twoDaysAgo);

            List<TemuOrderDO> orders = temuOrderMapper.selectList(queryWrapper);

            // 处理每个订单
            for (TemuOrderDO order : orders) {

                order.setComplianceUrl(oldTypeUrl);
                order.setComplianceImageUrl(oldTypeImageUrl);
                log.info("更新订单{}的合规单{}", order.getSkc(), oldTypeUrl);
                temuOrderMapper.updateById(order);

                if (StrUtil.isNotEmpty(order.getGoodsSn()) && StrUtil.isNotEmpty(order.getCustomSku())) {
                    // 异步处理PDF合并
                    CompletableFuture<String> future = asyncPdfProcessService.processPdfAsync(
                            oldTypeUrl,
                            order.getGoodsSn(),
                            temuOssService);

                    // 处理合并结果
                    future.thenAccept(mergedUrl -> {
                        if (StrUtil.isNotEmpty(mergedUrl)) {
                            // 更新订单的合并PDF URL
                            TemuOrderDO currentOrder = temuOrderMapper.selectById(order.getId());
                            if (currentOrder != null) {
                                currentOrder.setComplianceGoodsMergedUrl(mergedUrl);
                                temuOrderMapper.updateById(currentOrder);
                                log.info("订单PDF合并成功 - orderId: {}, mergedUrl: {}", order.getId(), mergedUrl);
                            }
                        }
                    }).exceptionally(e -> {
                        log.error("订单PDF合并失败 - orderId: {}, error: {}", order.getId(), e.getMessage());
                        return null;
                    });
                }
            }
        } catch (Exception e) {
            log.error("处理近两天订单PDF合并失败 - shopId: {}, skc: {}", shopId, skc, e);
        }
    }

    // 批量更新合规单信息
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateOldTypeInfo(List<TemuShopOldTypeUpdateReqVO> updateReqList) {
        if (CollUtil.isEmpty(updateReqList)) {
            return;
        }

        // 1. 收集所有的shopId和skc
        List<String> skcList = new ArrayList<>();
        Long shopId = null;
        Map<String, TemuShopOldTypeUpdateReqVO> updateReqMap = new HashMap<>();

        for (TemuShopOldTypeUpdateReqVO updateReq : updateReqList) {
            if (StrUtil.isEmpty(updateReq.getSkc())) {
                continue;
            }
            // 确保所有记录都是同一个shopId
            if (shopId == null) {
                shopId = updateReq.getShopId();
            } else if (!shopId.equals(updateReq.getShopId())) {
                log.error("批量更新包含不同的shopId，请确保所有记录属于同一个店铺");
                return;
            }
            skcList.add(updateReq.getSkc());
            updateReqMap.put(updateReq.getSkc(), updateReq);
        }

        if (CollUtil.isEmpty(skcList)) {
            return;
        }

        // 2. 批量查询现有记录
        LambdaQueryWrapper<TemuShopOldTypeSkcDO> queryWrapper = new LambdaQueryWrapper<TemuShopOldTypeSkcDO>()
                .eq(TemuShopOldTypeSkcDO::getShopId, shopId)
                .in(TemuShopOldTypeSkcDO::getSkc, skcList);
        List<TemuShopOldTypeSkcDO> existingRecords = temuShopOldTypeSkcMapper.selectList(queryWrapper);

        // 3. 将现有记录转换为Map，方便查找
        Map<String, TemuShopOldTypeSkcDO> existingRecordMap = existingRecords.stream()
                .collect(Collectors.toMap(TemuShopOldTypeSkcDO::getSkc, record -> record));

        // 4. 分类处理：需要新增的和需要更新的
        List<TemuShopOldTypeSkcDO> insertList = new ArrayList<>();
        List<TemuShopOldTypeSkcDO> updateList = new ArrayList<>();

        for (String skc : skcList) {
            TemuShopOldTypeUpdateReqVO updateReq = updateReqMap.get(skc);
            TemuShopOldTypeSkcDO existingRecord = existingRecordMap.get(skc);

            try {
                // 如果记录存在，检查是否需要更新
                if (existingRecord != null &&
                        StrUtil.equals(existingRecord.getOldType(), updateReq.getOldType()) &&
                        StrUtil.equals(existingRecord.getOldTypeUrl(), updateReq.getOldTypeUrl())) {
                    // 内容相同，跳过更新
                    continue;
                }

                // 处理当前记录（包含PDF转换逻辑）
                TemuShopOldTypeSkcDO oldTypeSkcDO = processOldTypeRecord(
                        updateReq.getShopId(),
                        updateReq.getSkc(),
                        updateReq.getOldType(),
                        updateReq.getOldTypeUrl());

                if (existingRecord != null) {
                    oldTypeSkcDO.setId(existingRecord.getId());
                    oldTypeSkcDO.setDeleted(false);
                    oldTypeSkcDO.setCreateTime(existingRecord.getCreateTime());
                    updateList.add(oldTypeSkcDO);
                } else {
                    insertList.add(oldTypeSkcDO);
                }
            } catch (Exception e) {
                log.error("更新失败，店铺ID：{}，SKC：{}", updateReq.getShopId(), updateReq.getSkc(), e);
            }
        }

        // 5. 执行批量操作
        if (!insertList.isEmpty()) {
            temuShopOldTypeSkcMapper.insertBatch(insertList);
        }
        for (TemuShopOldTypeSkcDO updateObj : updateList) {
            temuShopOldTypeSkcMapper.updateByIdWithoutDeleted(updateObj);
        }
    }

    // 获取合规单信息
    @Override
    public List<TemuShopOldTypeRespVO> getOldTypeInfo(TemuShopOldTypeReqVO reqVO) {
        // 构建查询条件
        LambdaQueryWrapperX<TemuShopOldTypeSkcDO> queryWrapper = new LambdaQueryWrapperX<TemuShopOldTypeSkcDO>()
                .eqIfPresent(TemuShopOldTypeSkcDO::getShopId, reqVO.getShopId())
                .eqIfPresent(TemuShopOldTypeSkcDO::getSkc, reqVO.getSkc())
                .eqIfPresent(TemuShopOldTypeSkcDO::getOldType, reqVO.getOldType());
        // 查询并转换结果
        List<TemuShopOldTypeSkcDO> result = temuShopOldTypeSkcMapper.selectList(queryWrapper);
        List<TemuShopOldTypeRespVO> respVOList = new ArrayList<>();
        for (TemuShopOldTypeSkcDO skcDO : result) {
            TemuShopOldTypeRespVO respVO = new TemuShopOldTypeRespVO();
            respVO.setShopId(skcDO.getShopId());
            respVO.setSkc(skcDO.getSkc());
            respVO.setOldTypeUrl(skcDO.getOldTypeUrl());
            respVO.setOldType(skcDO.getOldType());
            respVO.setOldTypeImageUrl(skcDO.getOldTypeImageUrl());
            respVOList.add(respVO);
        }
        return respVOList;
    }

    // 批量删除合规单信息
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteOldTypeInfo(TemuShopOldTypeDeleteReqVO deleteReqVO) {
        if (CollUtil.isEmpty(deleteReqVO.getSkcList())) {
            return;
        }
        // 构建删除条件并执行删除
        LambdaQueryWrapperX<TemuShopOldTypeSkcDO> deleteWrapper = new LambdaQueryWrapperX<TemuShopOldTypeSkcDO>()
                .eq(TemuShopOldTypeSkcDO::getShopId, deleteReqVO.getShopId())
                .in(TemuShopOldTypeSkcDO::getSkc, deleteReqVO.getSkcList())
                .eqIfPresent(TemuShopOldTypeSkcDO::getOldType, deleteReqVO.getOldType());
        temuShopOldTypeSkcMapper.delete(deleteWrapper);
    }

    // 生成缓存键（用于存储/获取PDF转图片结果） 如：shop1_1_http://example.com/report.pdf
    private String generateCacheKey(Long shopId, String oldType, String oldTypeUrl) {
        return String.format("%d_%s_%s", shopId, oldType, oldTypeUrl);
    }

    // 处理单个合规单记录
    private TemuShopOldTypeSkcDO processOldTypeRecord(Long shopId, String skc, String oldType, String oldTypeUrl) {
        TemuShopOldTypeSkcDO oldTypeSkcDO = new TemuShopOldTypeSkcDO();
        oldTypeSkcDO.setShopId(shopId);
        oldTypeSkcDO.setSkc(skc);
        oldTypeSkcDO.setOldType(oldType);
        oldTypeSkcDO.setOldTypeUrl(oldTypeUrl);
        // 如果oldTypeUrl为空，直接设置oldTypeImageUrl为null,这两个值要保持同步
        if (oldTypeUrl == null || oldTypeUrl.equals("")) {
            oldTypeSkcDO.setOldTypeImageUrl(null);
            return oldTypeSkcDO;
        }
        // 缓存逻辑：如果已有转换结果直接使用，否则进行转换并缓存
        String cacheKey = generateCacheKey(shopId, oldType, oldTypeUrl);
        String imageUrl = oldTypeUrlCache.get(cacheKey);

        if (imageUrl == null) {
            // 调用工具类转换PDF为图片，并上传到OSS
            imageUrl = PdfToImageUtil.getImageUrl(oldTypeUrl, oldType, temuOssService);
            if (imageUrl != null) {
                // 缓存转换结果
                oldTypeUrlCache.put(cacheKey, imageUrl);
            }
        }
        oldTypeSkcDO.setOldTypeImageUrl(imageUrl);
        return oldTypeSkcDO;
    }
}
