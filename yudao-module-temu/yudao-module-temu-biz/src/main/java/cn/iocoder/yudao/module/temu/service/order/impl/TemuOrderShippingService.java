package cn.iocoder.yudao.module.temu.service.order.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderListRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderShippingInfoDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderShippingMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuProductCategoryMapper;
import cn.iocoder.yudao.module.temu.service.order.ITemuOrderShippingService;
import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.dao.DataAccessException;
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

    // 获得待发货订单分页
    @Override
    public PageResult<TemuOrderShippingRespVO> getOrderShippingPage(TemuOrderShippingPageReqVO pageVO) {
        long startTime = System.currentTimeMillis();
        log.info("[getOrderShippingPage] 开始执行分页查询");
        
        // 1. 先根据订单条件查询符合条件的订单ID
        List<TemuOrderDO> matchedOrders = null;
        if (pageVO.getOrderStatus() != null || StringUtils.hasText(pageVO.getOrderNo())) {
            LambdaQueryWrapperX<TemuOrderDO> orderWrapper = new LambdaQueryWrapperX<>();
            orderWrapper.eqIfPresent(TemuOrderDO::getOrderStatus, pageVO.getOrderStatus())
                    .eqIfPresent(TemuOrderDO::getOrderNo, pageVO.getOrderNo());
            matchedOrders = orderMapper.selectList(orderWrapper);
            
            log.info("[getOrderShippingPage] 步骤1：订单条件过滤耗时：{}ms, 匹配订单数：{}", 
                    System.currentTimeMillis() - startTime, 
                    matchedOrders == null ? 0 : matchedOrders.size());

            if (CollectionUtils.isEmpty(matchedOrders)) {
                log.info("[getOrderShippingPage] 未找到匹配订单，总耗时：{}ms", System.currentTimeMillis() - startTime);
                return new PageResult<>(new ArrayList<>(), 0L, pageVO.getPageNo(), pageVO.getPageSize());
            }
        }
        
        long step1Time = System.currentTimeMillis();

        // 2. 构建物流信息查询条件
        LambdaQueryWrapperX<TemuOrderShippingInfoDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eqIfPresent(TemuOrderShippingInfoDO::getShopId, pageVO.getShopId())
                .likeIfPresent(TemuOrderShippingInfoDO::getTrackingNumber, pageVO.getTrackingNumber());

        // 处理创建时间条件
        if (pageVO.getCreateTime() != null && pageVO.getCreateTime().length == 2) {
            LocalDateTime beginTime = pageVO.getCreateTime()[0].atStartOfDay();
            LocalDateTime endTime = pageVO.getCreateTime()[1].atTime(23, 59, 59, 999999999);
            queryWrapper.between(TemuOrderShippingInfoDO::getCreateTime, beginTime, endTime);
        }

        if (matchedOrders != null) {
            Set<String> orderNos = matchedOrders.stream()
                    .map(TemuOrderDO::getOrderNo)
                    .collect(Collectors.toSet());
            queryWrapper.in(TemuOrderShippingInfoDO::getOrderNo, orderNos);
        }

        queryWrapper.orderByDesc(TemuOrderShippingInfoDO::getCreateTime)
                .orderByDesc(TemuOrderShippingInfoDO::getId);
                
        log.info("[getOrderShippingPage] 步骤2：构建查询条件耗时：{}ms", System.currentTimeMillis() - step1Time);
        long step2Time = System.currentTimeMillis();

        // 3. 执行分页查询
        PageResult<TemuOrderShippingInfoDO> pageResult = shippingInfoMapper.selectPage(pageVO, queryWrapper);
        log.info("[getOrderShippingPage] 步骤3：执行分页查询耗时：{}ms, 结果数量：{}", 
                System.currentTimeMillis() - step2Time, 
                pageResult.getList().size());
                
        if (CollectionUtils.isEmpty(pageResult.getList())) {
            log.info("[getOrderShippingPage] 分页查询无结果，总耗时：{}ms", System.currentTimeMillis() - startTime);
            return new PageResult<>(new ArrayList<>(), pageResult.getTotal(), pageVO.getPageNo(), pageVO.getPageSize());
        }
        
        long step3Time = System.currentTimeMillis();

        // 4. 获取所有需要的订单编号和店铺ID
        Set<String> allOrderNos = pageResult.getList().stream()
                .map(TemuOrderShippingInfoDO::getOrderNo)
                .collect(Collectors.toSet());
        // 这里已经拿到店铺ID了
        Set<Long> shopIds = pageResult.getList().stream()
                .map(TemuOrderShippingInfoDO::getShopId)
                .collect(Collectors.toSet());
                
        log.info("[getOrderShippingPage] 步骤4：提取订单编号和店铺ID耗时：{}ms, 订单数：{}, 店铺数：{}", 
                System.currentTimeMillis() - step3Time, 
                allOrderNos.size(), 
                shopIds.size());
        long step4Time = System.currentTimeMillis();

        // 5. 批量查询订单信息
        // 这里已经拿到订单信息了
        List<TemuOrderDO> orders = orderMapper.selectList(new LambdaQueryWrapperX<TemuOrderDO>()
                .in(TemuOrderDO::getOrderNo, allOrderNos));
        Map<String, TemuOrderDO> orderMap = orders.stream()
                .collect(Collectors.toMap(TemuOrderDO::getOrderNo, Function.identity(), (v1, v2) -> v1));
                
        log.info("[getOrderShippingPage] 步骤5：批量查询订单信息耗时：{}ms, 查询到订单数：{}", 
                System.currentTimeMillis() - step4Time, 
                orders.size());
        long step5Time = System.currentTimeMillis();

        // 6. 批量查询店铺信息
        Map<Long, TemuShopDO> shopMap = new HashMap<>();
        if (!shopIds.isEmpty()) {
            // 批量查询店铺信息，替代循环单个查询
            List<TemuShopDO> shops = shopMapper.selectList(
                    new LambdaQueryWrapperX<TemuShopDO>().in(TemuShopDO::getShopId, shopIds));
            if (shops != null) {
                shopMap = shops.stream()
                        .collect(Collectors.toMap(TemuShopDO::getShopId, shop -> shop, (v1, v2) -> v1));
            }
        }
        
        log.info("[getOrderShippingPage] 步骤6：批量查询店铺信息耗时：{}ms, 查询到店铺数：{}", 
                System.currentTimeMillis() - step5Time, 
                shopMap.size());
        long step6Time = System.currentTimeMillis();

        // 7. 组装返回结果
        List<TemuOrderShippingRespVO> voList = new ArrayList<>();
        
        // 预查询所有分类信息并缓存
        Set<String> categoryIds = orders.stream()
                .map(TemuOrderDO::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
                
        Map<String, TemuProductCategoryDO> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            // 收集有效的分类ID（能够转换为Long类型的）
            List<Long> validCategoryIds = new ArrayList<>();
            Map<Long, String> catIdMapping = new HashMap<>(); // 用于保存Long和String类型ID的映射
            
            for (String categoryId : categoryIds) {
                try {
                    Long catId = Long.parseLong(categoryId);
                    validCategoryIds.add(catId);
                    catIdMapping.put(catId, categoryId);
                } catch (NumberFormatException e) {
                    log.warn("[getOrderShippingPage] 转换分类ID失败: {}", categoryId);
                }
            }
            
            // 一次性批量查询所有分类
            if (!validCategoryIds.isEmpty()) {
                long queryStart = System.currentTimeMillis();
                
                // 执行批量查询
                List<TemuProductCategoryDO> categories = categoryMapper.selectList(
                        new LambdaQueryWrapperX<TemuProductCategoryDO>()
                                .in(TemuProductCategoryDO::getCategoryId, validCategoryIds));
                
                // 构建分类映射，使用String类型的categoryId作为键
                if (categories != null) {
                    for (TemuProductCategoryDO category : categories) {
                        String originalCategoryId = catIdMapping.get(category.getCategoryId());
                        if (originalCategoryId != null) {
                            categoryMap.put(originalCategoryId, category);
                        }
                    }
                }
                
                log.debug("[getOrderShippingPage] 批量查询分类信息耗时：{}ms, 查询数量：{}, 结果数量：{}", 
                        System.currentTimeMillis() - queryStart, 
                        validCategoryIds.size(),
                        categories != null ? categories.size() : 0);
            }
        }
        
        // 批量处理所有订单物流信息
        for (TemuOrderShippingInfoDO shippingInfo : pageResult.getList()) {
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
            if (orderNo != null) {
                List<TemuOrderDO> ordersList = orders.stream()
                        .filter(order -> orderNo.equals(order.getOrderNo()))
                        .collect(Collectors.toList());
                
                if (!CollectionUtils.isEmpty(ordersList)) {
                    List<TemuOrderListRespVO> orderListVOs = new ArrayList<>();
                    for (TemuOrderDO order : ordersList) {
                        TemuOrderListRespVO orderVO = BeanUtils.toBean(order, TemuOrderListRespVO.class);
                        if (order.getCategoryId() != null) {
                            TemuProductCategoryDO category = categoryMap.get(order.getCategoryId());
                            if (category != null && category.getOldType() != null && shop != null && shop.getOldTypeUrl() != null) {
                                Object url = shop.getOldTypeUrl().get(category.getOldType());
                                if (url != null) {
                                    orderVO.setOldTypeUrl(url.toString());
                                }
                            }
                        }
                        orderListVOs.add(orderVO);
                    }
                    vo.setOrderList(orderListVOs);
                }
            }
            
            voList.add(vo);
        }
        
        log.info("[getOrderShippingPage] 步骤7：组装返回结果耗时：{}ms, 结果数量：{}", 
                System.currentTimeMillis() - step6Time, 
                voList.size());
        log.info("[getOrderShippingPage] 分页查询完成，总耗时：{}ms", System.currentTimeMillis() - startTime);

        return new PageResult<>(voList, pageResult.getTotal(), pageVO.getPageNo(), pageVO.getPageSize());
    }

    //批量保存待发货订单
    @Override
    @Transactional(rollbackFor = Exception.class) // 确保事务一致性
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
                throw new IllegalArgumentException("物流单号不能为空"); // 新增校验
            }
        }

        // 2. 提取所有 trackingNumber
        Set<String> trackingNumbers = saveRequestVOs.stream()
                .map(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO::getTrackingNumber)
                .collect(Collectors.toSet());

        // 3. 查询已存在的 trackingNumber 记录
        List<TemuOrderShippingInfoDO> existingShippings = shippingInfoMapper.selectList(
                new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
                        .in(TemuOrderShippingInfoDO::getTrackingNumber, trackingNumbers));

        // 4. 如果存在，先删除（批量删除）
        if (!existingShippings.isEmpty()) {
            // 提取存在的 trackingNumber（可能部分存在）
            Set<String> existingTrackingNumbers = existingShippings.stream()
                    .map(TemuOrderShippingInfoDO::getTrackingNumber)
                    .collect(Collectors.toSet());

            // 根据 trackingNumber 删除记录
            int deletedRows = shippingInfoMapper.delete(
                    new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
                            .in(TemuOrderShippingInfoDO::getTrackingNumber, existingTrackingNumbers));
            log.info("删除已存在的物流单号记录，数量：{}", deletedRows);
        }

        // 5. 转换所有请求为 DO（无需过滤，直接全部保存）
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
                    info.setCreateTime(now);
                    info.setUpdateTime(now);
                    return info;
                })
                .collect(Collectors.toList());

        // 6. 批量保存
        if (!toSaveList.isEmpty()) {
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

    // 校验保存请求参数
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchUpdateOrderStatus(List<Long> orderIds, Integer orderStatus) {
        if (CollUtil.isEmpty(orderIds)) {
            return false;
        }

        // 构建批量更新条件
        List<TemuOrderDO> updateList = orderIds.stream().map(orderId -> {
            TemuOrderDO order = new TemuOrderDO();
            order.setId(orderId);
            order.setOrderStatus(orderStatus);
            return order;
        }).collect(Collectors.toList());

        // 执行批量更新
        orderMapper.updateBatch(updateList);
        return true;
    }

    // 转换单个物流信息为响应VO
    private TemuOrderShippingRespVO convertToRespVO(TemuOrderShippingInfoDO shippingInfo,
            Map<String, TemuOrderDO> orderMap, Map<Long, TemuShopDO> shopMap) {
        long startTime = System.currentTimeMillis();
        
        TemuOrderShippingRespVO vo = BeanUtils.toBean(shippingInfo, TemuOrderShippingRespVO.class);

        // 设置订单相关信息
        String orderNo = shippingInfo.getOrderNo();
        vo.setOrderNo(orderNo);
        if (orderNo != null) {
            long queryStart = System.currentTimeMillis();
            List<TemuOrderDO> matchedOrders = orderMapper.selectList(
                    new LambdaQueryWrapperX<TemuOrderDO>()
                            .eq(TemuOrderDO::getOrderNo, orderNo));
            long queryEnd = System.currentTimeMillis();
            log.debug("[convertToRespVO] 查询订单信息耗时：{}ms, 订单号：{}, 匹配数量：{}", 
                    queryEnd - queryStart, orderNo, matchedOrders.size());
                    
            if (!CollectionUtils.isEmpty(matchedOrders)) {
                long convertStart = System.currentTimeMillis();
                List<TemuOrderListRespVO> orderListVOs = new ArrayList<>();
                for (TemuOrderDO order : matchedOrders) {
                    TemuOrderListRespVO orderVO = BeanUtils.toBean(order, TemuOrderListRespVO.class);
                    if (order.getCategoryId() != null) {
                        TemuProductCategoryDO category = categoryMapper.selectById(order.getCategoryId());
                        if (category != null && category.getOldType() != null) {
                            TemuShopDO shop = shopMapper.selectByShopId(shippingInfo.getShopId());
                            if (shop != null && shop.getOldTypeUrl() != null) {
                                Object url = shop.getOldTypeUrl().get(category.getOldType());
                                if (url != null) {
                                    orderVO.setOldTypeUrl(url.toString());
                                }
                            }
                        }
                    }
                    orderListVOs.add(orderVO);
                }
                vo.setOrderList(orderListVOs);
                log.debug("[convertToRespVO] 转换订单列表耗时：{}ms, 订单号：{}, 订单数量：{}", 
                        System.currentTimeMillis() - convertStart, orderNo, orderListVOs.size());
            }
        }

        // 设置店铺信息
        vo.setShopId(shippingInfo.getShopId());
        if (shippingInfo.getShopId() != null) {
            TemuShopDO shop = shopMap.get(shippingInfo.getShopId());
            if (shop != null) {
                vo.setShopName(shop.getShopName());
            }
        }

        log.debug("[convertToRespVO] 转换物流信息完成，总耗时：{}ms, 订单号：{}", 
                System.currentTimeMillis() - startTime, orderNo);
        return vo;
    }

}
