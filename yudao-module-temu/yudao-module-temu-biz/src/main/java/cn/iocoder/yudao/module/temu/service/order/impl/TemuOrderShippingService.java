package cn.iocoder.yudao.module.temu.service.order.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderListRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.*;
import cn.iocoder.yudao.module.temu.dal.mysql.*;
import cn.iocoder.yudao.module.temu.service.order.ITemuOrderShippingService;
import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import java.util.Objects;

/**
 * Temu订单物流 Service 实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TemuOrderShippingService implements ITemuOrderShippingService {

    private final TemuOrderShippingMapper shippingInfoMapper;
    private final TemuOrderMapper orderMapper;
    private final TemuShopMapper shopMapper;
    private final TemuProductCategoryMapper categoryMapper;
    private final TemuUserShopMapper userShopMapper;

    // 分页查询用户店铺待发货列表
    @Override
    public PageResult<TemuOrderShippingRespVO> getOrderShippingPageByUser(TemuOrderShippingPageReqVO pageVO,
            Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("[getOrderShippingPageByUser] 开始执行用户店铺待发货列表查询, userId: {}", userId);
        // 1. 查询用户绑定的店铺列表
        long step1StartTime = System.currentTimeMillis();
        List<TemuUserShopDO> userShops = userShopMapper.selectList(
                new LambdaQueryWrapperX<TemuUserShopDO>()
                        .eq(TemuUserShopDO::getUserId, userId));
        log.info("[getOrderShippingPageByUser] 步骤1：查询用户店铺列表耗时：{}ms, 店铺数量：{}",
                System.currentTimeMillis() - step1StartTime,
                userShops == null ? 0 : userShops.size());
        if (CollectionUtils.isEmpty(userShops)) {
            log.info("[getOrderShippingPageByUser] 用户未绑定店铺，直接返回空结果，总耗时：{}ms",
                    System.currentTimeMillis() - startTime);
            return new PageResult<>(new ArrayList<>(), 0L, pageVO.getPageNo(), pageVO.getPageSize());
        }
        // 2. 获取用户的所有店铺ID
        Set<Long> userShopIds = userShops.stream()
                .map(TemuUserShopDO::getShopId)
                .collect(Collectors.toSet());
        // 3. 如果指定了shopId，验证权限
        if (pageVO.getShopId() != null && !userShopIds.contains(pageVO.getShopId())) {
            log.info("[getOrderShippingPageByUser] 指定的shopId不属于该用户，直接返回空结果，总耗时：{}ms, shopId: {}",
                    System.currentTimeMillis() - startTime,
                    pageVO.getShopId());
            return new PageResult<>(new ArrayList<>(), 0L, pageVO.getPageNo(), pageVO.getPageSize());
        }
        // 4. 查询匹配的订单
        List<TemuOrderDO> matchedOrders = getMatchedOrders(pageVO.getOrderStatus(), pageVO.getOrderNo());
        if (matchedOrders != null && matchedOrders.isEmpty()) {
            log.info("[getOrderShippingPageByUser] 未找到匹配订单，总耗时：{}ms", System.currentTimeMillis() - startTime);
            return new PageResult<>(new ArrayList<>(), 0L, pageVO.getPageNo(), pageVO.getPageSize());
        }
        // 5. 构建查询条件并执行分页查询
        LambdaQueryWrapperX<TemuOrderShippingInfoDO> queryWrapper = buildShippingQueryWrapper(pageVO, matchedOrders,
                pageVO.getShopId() == null ? userShopIds : null);
        PageResult<TemuOrderShippingInfoDO> pageResult = shippingInfoMapper.selectPage(pageVO, queryWrapper);

        if (CollectionUtils.isEmpty(pageResult.getList())) {
            log.info("[getOrderShippingPageByUser] 分页查询无结果，总耗时：{}ms", System.currentTimeMillis() - startTime);
            return new PageResult<>(new ArrayList<>(), pageResult.getTotal(), pageVO.getPageNo(), pageVO.getPageSize());
        }
        // 6. 批量查询相关数据
        Set<String> allOrderNos = pageResult.getList().stream()
                .map(TemuOrderShippingInfoDO::getOrderNo)
                .collect(Collectors.toSet());
        // 查询订单信息
        List<TemuOrderDO> orders = orderMapper.selectList(
                new LambdaQueryWrapperX<TemuOrderDO>()
                        .in(TemuOrderDO::getOrderNo, allOrderNos));
        Map<String, TemuOrderDO> orderMap = orders.stream()
                .collect(Collectors.toMap(TemuOrderDO::getOrderNo, Function.identity(), (v1, v2) -> v1));
        // 查询店铺信息
        List<TemuShopDO> shops = shopMapper.selectList(
                new LambdaQueryWrapperX<TemuShopDO>()
                        .in(TemuShopDO::getShopId, userShopIds));
        Map<Long, TemuShopDO> shopMap = shops.stream()
                .collect(Collectors.toMap(TemuShopDO::getShopId, Function.identity(), (v1, v2) -> v1));
        // 查询分类信息
        Map<String, TemuProductCategoryDO> categoryMap = getCategoryMap(orders);
        // 7. 组装返回结果
        List<TemuOrderShippingRespVO> voList = buildOrderShippingRespList(pageResult.getList(), orderMap, shopMap,
                categoryMap);
        log.info("[getOrderShippingPageByUser] 查询完成 - 总记录数：{}, 当前页记录数：{}, 总耗时：{}ms",
                pageResult.getTotal(),
                voList.size(),
                System.currentTimeMillis() - startTime);

        return new PageResult<>(voList, pageResult.getTotal(), pageVO.getPageNo(), pageVO.getPageSize());
    }

    // 分页查询待发货列表
    @Override
    public PageResult<TemuOrderShippingRespVO> getOrderShippingPage(TemuOrderShippingPageReqVO pageVO) {
        long startTime = System.currentTimeMillis();
        log.info("[getOrderShippingPage] 开始执行分页查询");
        // 1. 查询匹配的订单
        List<TemuOrderDO> matchedOrders = getMatchedOrders(pageVO.getOrderStatus(), pageVO.getOrderNo());
        if (matchedOrders != null && matchedOrders.isEmpty()) {
            log.info("[getOrderShippingPage] 未找到匹配订单，总耗时：{}ms", System.currentTimeMillis() - startTime);
            return new PageResult<>(new ArrayList<>(), 0L, pageVO.getPageNo(), pageVO.getPageSize());
        }
        // 2. 构建查询条件并执行分页查询
        LambdaQueryWrapperX<TemuOrderShippingInfoDO> queryWrapper = buildShippingQueryWrapper(pageVO, matchedOrders,
                null);
        PageResult<TemuOrderShippingInfoDO> pageResult = shippingInfoMapper.selectPage(pageVO, queryWrapper);

        if (CollectionUtils.isEmpty(pageResult.getList())) {
            log.info("[getOrderShippingPage] 分页查询无结果，总耗时：{}ms", System.currentTimeMillis() - startTime);
            return new PageResult<>(new ArrayList<>(), pageResult.getTotal(), pageVO.getPageNo(), pageVO.getPageSize());
        }
        // 3. 批量查询相关数据
        Set<String> allOrderNos = pageResult.getList().stream()
                .map(TemuOrderShippingInfoDO::getOrderNo)
                .collect(Collectors.toSet());
        Set<Long> shopIds = pageResult.getList().stream()
                .map(TemuOrderShippingInfoDO::getShopId)
                .collect(Collectors.toSet());
        // 查询订单信息
        List<TemuOrderDO> orders = orderMapper.selectList(
                new LambdaQueryWrapperX<TemuOrderDO>()
                        .in(TemuOrderDO::getOrderNo, allOrderNos));
        Map<String, TemuOrderDO> orderMap = orders.stream()
                .collect(Collectors.toMap(TemuOrderDO::getOrderNo, Function.identity(), (v1, v2) -> v1));
        // 查询店铺信息
        List<TemuShopDO> shops = shopMapper.selectList(
                new LambdaQueryWrapperX<TemuShopDO>()
                        .in(TemuShopDO::getShopId, shopIds));
        Map<Long, TemuShopDO> shopMap = shops.stream()
                .collect(Collectors.toMap(TemuShopDO::getShopId, Function.identity(), (v1, v2) -> v1));
        // 查询分类信息
        Map<String, TemuProductCategoryDO> categoryMap = getCategoryMap(orders);
        // 4. 组装返回结果
        List<TemuOrderShippingRespVO> voList = buildOrderShippingRespList(pageResult.getList(), orderMap, shopMap,
                categoryMap);
        log.info("[getOrderShippingPage] 查询完成 - 总记录数：{}, 当前页记录数：{}, 总耗时：{}ms",
                pageResult.getTotal(),
                voList.size(),
                System.currentTimeMillis() - startTime);
        return new PageResult<>(voList, pageResult.getTotal(), pageVO.getPageNo(), pageVO.getPageSize());
    }

    // 批量保存待发货订单
    /**
     * 物流单号可以重复
     * 已发货状态的记录都会被保留
     * 只有当物流单号、shopId和orderNo都匹配时，才会从 saveRequestVOs 中移除对应记录（避免重复保存）
     * 未发货状态的记录会被删除并重新保存
     * 新保存的记录默认为未发货状态
     * 
     * @param saveRequestVOs
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSaveOrderShipping(List<TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO> saveRequestVOs) {
        if (CollectionUtils.isEmpty(saveRequestVOs)) {
            return 0;
        }
        // 1. 参数校验（强制要求 trackingNumber 非空）
        for (TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO : saveRequestVOs) {
            if (saveRequestVO == null) {
                throw new IllegalArgumentException("待发货订单信息不能为空");
            }
            if (saveRequestVO.getOrderNo() == null) {
                throw new IllegalArgumentException("订单编号不能为空");
            }
            if (saveRequestVO.getShopId() == null) {
                throw new IllegalArgumentException("店铺ID不能为空");
            }
            if (saveRequestVO.getTrackingNumber() == null) {
                throw new IllegalArgumentException("物流单号不能为空");
            }
        }

        // 2. 提取所有 trackingNumber
        Set<String> trackingNumbers = saveRequestVOs.stream()
                .map(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO::getTrackingNumber)
                .collect(Collectors.toSet());

        // 3. 查询已存在的记录
        List<TemuOrderShippingInfoDO> existingShippings = shippingInfoMapper.selectList(
                new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
                        .in(TemuOrderShippingInfoDO::getTrackingNumber, trackingNumbers));

        // 4. 处理已存在的记录
        if (!existingShippings.isEmpty()) {
            // 收集需要删除的记录ID
            List<Long> toDeleteIds = new ArrayList<>();
            // 创建一个Set来存储已发货且匹配的记录的唯一标识（物流单号+shopId+orderNo）
            Set<String> matchedShippedRecords = new HashSet<>();

            for (TemuOrderShippingInfoDO existing : existingShippings) {
                if (existing.getShippingStatus() != null && existing.getShippingStatus() == 1) {
                    // 已发货状态的记录都保留（不删除）
                    // 检查是否有完全匹配的新记录
                    for (TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO newRecord : saveRequestVOs) {
                        if (existing.getTrackingNumber().equals(newRecord.getTrackingNumber())
                                && existing.getShopId().equals(newRecord.getShopId())
                                && existing.getOrderNo().equals(newRecord.getOrderNo())) {
                            // 如果找到完全匹配的记录，将其标记为已匹配
                            matchedShippedRecords.add(generateMatchKey(newRecord));
                        }
                    }
                } else {
                    // 未发货状态的记录需要删除
                    toDeleteIds.add(existing.getId());
                }
            }

            // 删除未发货状态的记录
            if (!toDeleteIds.isEmpty()) {
                shippingInfoMapper.deleteBatchIds(toDeleteIds);
            }

            // 过滤掉已发货且完全匹配的记录
            if (!matchedShippedRecords.isEmpty()) {
                saveRequestVOs = saveRequestVOs.stream()
                        .filter(vo -> !matchedShippedRecords.contains(generateMatchKey(vo)))
                        .collect(Collectors.toList());
            }
        }

        // 5. 如果还有需要保存的记录
        if (!saveRequestVOs.isEmpty()) {
            // 转换所有请求为 DO
            LocalDateTime now = LocalDateTime.now();
            List<TemuOrderShippingInfoDO> toSaveList = saveRequestVOs.stream()
                    .map(vo -> {
                        TemuOrderShippingInfoDO info = new TemuOrderShippingInfoDO();
                        info.setOrderNo(vo.getOrderNo());
                        info.setTrackingNumber(vo.getTrackingNumber());
                        info.setExpressImageUrl(vo.getExpressImageUrl());
                        info.setExpressOutsideImageUrl(vo.getExpressOutsideImageUrl());
                        info.setExpressSkuImageUrl(vo.getExpressSkuImageUrl());
                        info.setShopId(vo.getShopId());
                        info.setShippingStatus(0); // 新保存的记录默认为未发货状态
                        info.setCreateTime(now);
                        info.setUpdateTime(now);
                        return info;
                    })
                    .collect(Collectors.toList());

            // 6. 批量保存
            try {
                int affectedRows = shippingInfoMapper.insertBatch(toSaveList);
                log.info("批量保存成功，数量：{}", affectedRows);
                return affectedRows;
            } catch (Exception e) {
                log.error("批量保存失败", e);
                throw new ServiceException("批量保存失败：" + e.getMessage());
            }
        }
        return 0;
    }

    /**
     * 生成记录的唯一匹配键（物流单号+shopId+orderNo）
     */
    private String generateMatchKey(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO vo) {
        return vo.getTrackingNumber() + "_" + vo.getShopId() + "_" + vo.getOrderNo();
    }

    /**
     * 批量更新订单状态，同时更新对应的物流订单发货状态
     * 
     * @param orderIds       订单ID列表
     * @param orderStatus    订单状态
     * @param trackingNumber 物流单号
     * @return 是否更新成功
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchUpdateOrderStatus(List<Long> orderIds, Integer orderStatus, String trackingNumber) {
        if (CollUtil.isEmpty(orderIds)) {
            return false;
        }

        log.info("[batchUpdateOrderStatus] 开始执行批量更新, orderIds={}, orderStatus={}, trackingNumber={}",
                orderIds, orderStatus, trackingNumber);

        // 1. 更新订单状态
        List<TemuOrderDO> updateList = orderIds.stream().map(orderId -> {
            TemuOrderDO order = new TemuOrderDO();
            order.setId(orderId);
            order.setOrderStatus(orderStatus);
            return order;
        }).collect(Collectors.toList());

        // 2. 执行订单状态更新
        boolean orderUpdateCount = orderMapper.updateBatch(updateList);
        log.info("[batchUpdateOrderStatus] 订单状态更新完成, 更新数量: {}", orderUpdateCount);

        // 3. 如果提供了物流单号，更新物流订单发货状态
        if (StringUtils.hasText(trackingNumber)) {
            // 查询订单编号
            List<TemuOrderDO> orders = orderMapper.selectBatchIds(orderIds);
            log.info("[batchUpdateOrderStatus] 查询到订单数量: {}", orders.size());

            if (!orders.isEmpty()) {
                // 收集订单编号
                Set<String> orderNos = orders.stream()
                        .map(TemuOrderDO::getOrderNo)
                        .collect(Collectors.toSet());
                log.info("[batchUpdateOrderStatus] 待更新的订单编号: {}", orderNos);

                // 更新物流订单发货状态
                TemuOrderShippingInfoDO updateShipping = new TemuOrderShippingInfoDO();
                updateShipping.setShippingStatus(1); // 设置为已发货
                updateShipping.setUpdateTime(LocalDateTime.now());

                // 先查询符合条件的记录
                List<TemuOrderShippingInfoDO> matchingRecords = shippingInfoMapper.selectList(
                        new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
                                .eq(TemuOrderShippingInfoDO::getTrackingNumber, trackingNumber)
                                .in(TemuOrderShippingInfoDO::getOrderNo, orderNos));

                log.info("[batchUpdateOrderStatus] 找到匹配的物流记录数量: {}", matchingRecords.size());

                if (!matchingRecords.isEmpty()) {
                    // 执行更新
                    int shippingUpdateCount = shippingInfoMapper.update(updateShipping,
                            new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
                                    .eq(TemuOrderShippingInfoDO::getTrackingNumber, trackingNumber)
                                    .in(TemuOrderShippingInfoDO::getOrderNo, orderNos));

                    log.info("[batchUpdateOrderStatus] 物流订单状态更新完成, 更新数量: {}", shippingUpdateCount);
                } else {
                    log.warn("[batchUpdateOrderStatus] 未找到匹配的物流记录, trackingNumber={}, orderNos={}",
                            trackingNumber, orderNos);
                }
            }
        }

        return true;
    }


    /**
     * 根据订单状态和订单号查询匹配的订单
     *
     * @param orderStatus 订单状态
     * @param orderNo     订单号
     * @return 匹配的订单列表
     */
    private List<TemuOrderDO> getMatchedOrders(Integer orderStatus, String orderNo) {
        if (orderStatus == null && !StringUtils.hasText(orderNo)) {
            return null;
        }
        LambdaQueryWrapperX<TemuOrderDO> orderWrapper = new LambdaQueryWrapperX<>();
        orderWrapper.eqIfPresent(TemuOrderDO::getOrderStatus, orderStatus)
                .eqIfPresent(TemuOrderDO::getOrderNo, orderNo);
        return orderMapper.selectList(orderWrapper);
    }

    /**
     * 构建物流信息查询条件
     */
    private LambdaQueryWrapperX<TemuOrderShippingInfoDO> buildShippingQueryWrapper(TemuOrderShippingPageReqVO pageVO,
            List<TemuOrderDO> matchedOrders, Set<Long> limitShopIds) {
        LambdaQueryWrapperX<TemuOrderShippingInfoDO> queryWrapper = new LambdaQueryWrapperX<>();

        // 处理店铺ID条件
        if (pageVO.getShopId() != null) {
            queryWrapper.eq(TemuOrderShippingInfoDO::getShopId, pageVO.getShopId());
        } else if (limitShopIds != null && !limitShopIds.isEmpty()) {
            queryWrapper.in(TemuOrderShippingInfoDO::getShopId, limitShopIds);
        }

        // 处理物流单号
        queryWrapper.likeIfPresent(TemuOrderShippingInfoDO::getTrackingNumber, pageVO.getTrackingNumber());

        // 处理创建时间条件
        if (pageVO.getCreateTime() != null && pageVO.getCreateTime().length == 2) {
            LocalDateTime beginTime = pageVO.getCreateTime()[0].atStartOfDay();
            LocalDateTime endTime = pageVO.getCreateTime()[1].atTime(23, 59, 59, 999999999);
            queryWrapper.between(TemuOrderShippingInfoDO::getCreateTime, beginTime, endTime);
        }

        // 处理订单号条件
        if (matchedOrders != null) {
            Set<String> orderNos = matchedOrders.stream()
                    .map(TemuOrderDO::getOrderNo)
                    .collect(Collectors.toSet());
            queryWrapper.in(TemuOrderShippingInfoDO::getOrderNo, orderNos);
        }

        // 排序
        queryWrapper.orderByDesc(TemuOrderShippingInfoDO::getCreateTime)
                .orderByDesc(TemuOrderShippingInfoDO::getId);

        return queryWrapper;
    }

    /**
     * 批量查询分类信息
     */
    private Map<String, TemuProductCategoryDO> getCategoryMap(List<TemuOrderDO> orders) {
        Map<String, TemuProductCategoryDO> categoryMap = new HashMap<>();
        Set<String> categoryIds = orders.stream()
                .map(TemuOrderDO::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!categoryIds.isEmpty()) {
            List<Long> validCategoryIds = new ArrayList<>();
            Map<Long, String> catIdMapping = new HashMap<>();
            for (String categoryId : categoryIds) {
                try {
                    Long catId = Long.parseLong(categoryId);
                    validCategoryIds.add(catId);
                    catIdMapping.put(catId, categoryId);
                } catch (NumberFormatException e) {
                    log.warn("[getCategoryMap] 转换分类ID失败: {}", categoryId);
                }
            }

            if (!validCategoryIds.isEmpty()) {
                List<TemuProductCategoryDO> categories = categoryMapper.selectList(
                        new LambdaQueryWrapperX<TemuProductCategoryDO>()
                                .in(TemuProductCategoryDO::getCategoryId, validCategoryIds));
                if (categories != null) {
                    for (TemuProductCategoryDO category : categories) {
                        String originalCategoryId = catIdMapping.get(category.getCategoryId());
                        if (originalCategoryId != null) {
                            categoryMap.put(originalCategoryId, category);
                        }
                    }
                }
            }
        }
        return categoryMap;
    }

    /**
     * 组装订单物流返回结果
     */
    private List<TemuOrderShippingRespVO> buildOrderShippingRespList(List<TemuOrderShippingInfoDO> shippingList,
            Map<String, TemuOrderDO> orderMap,
            Map<Long, TemuShopDO> shopMap,
            Map<String, TemuProductCategoryDO> categoryMap) {
        List<TemuOrderShippingRespVO> voList = new ArrayList<>();
        for (TemuOrderShippingInfoDO shippingInfo : shippingList) {
            TemuOrderShippingRespVO vo = BeanUtils.toBean(shippingInfo, TemuOrderShippingRespVO.class);

            // 设置订单相关信息
            String orderNo = shippingInfo.getOrderNo();
            vo.setOrderNo(orderNo);

            // 设置店铺信息
            Long shopId = shippingInfo.getShopId();
            vo.setShopId(shopId);
            TemuShopDO shop = shopMap.get(shopId);
            if (shop != null) {
                vo.setShopName(shop.getShopName());
            }

            // 设置订单列表
            List<TemuOrderListRespVO> orderListVOs = new ArrayList<>();
            TemuOrderDO order = orderMap.get(orderNo);
            if (order != null) {
                TemuOrderListRespVO orderVO = BeanUtils.toBean(order, TemuOrderListRespVO.class);
                if (order.getCategoryId() != null) {
                    TemuProductCategoryDO category = categoryMap.get(order.getCategoryId());
                    if (category != null && category.getOldType() != null && shop != null
                            && shop.getOldTypeUrl() != null) {
                        Object url = shop.getOldTypeUrl().get(category.getOldType());
                        if (url != null) {
                            orderVO.setOldTypeUrl(url.toString());
                        }
                    }
                }
                orderListVOs.add(orderVO);
            }
            vo.setOrderList(orderListVOs);

            voList.add(vo);
        }
        return voList;
    }

}
