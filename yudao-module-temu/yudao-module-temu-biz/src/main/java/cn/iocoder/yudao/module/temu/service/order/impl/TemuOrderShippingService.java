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

    // 保存待发货订单
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrderShipping(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO) {
        // 1. 参数校验
        validateSaveRequest(saveRequestVO);
        try {
            // 2. 构建物流信息对象
            TemuOrderShippingInfoDO shippingInfo = buildShippingInfo(saveRequestVO);
            // 3. 保存到数据库
            int affectedRows = shippingInfoMapper.insert(shippingInfo);
            if (affectedRows == 0) {
                throw new PersistenceException("订单物流信息保存失败");
            }
            // 4. 返回ID
            Long id = shippingInfo.getId();
            log.info("订单物流信息保存成功，ID：{}", id);
            return id;
        } catch (DataAccessException e) {
            log.error("保存订单物流信息时发生数据库异常：{}", e.getMessage());
            throw new PersistenceException("数据库操作失败，原因：" + e.getMessage(), e);
        }
    }

    // 获得待发货订单分页
    @Override
    public PageResult<TemuOrderShippingRespVO> getOrderShippingPage(TemuOrderShippingPageReqVO pageVO) {
        // 1. 先根据订单条件查询符合条件的订单ID
        List<TemuOrderDO> matchedOrders = null;
        if (pageVO.getOrderStatus() != null || StringUtils.hasText(pageVO.getOrderNo())) {
            LambdaQueryWrapperX<TemuOrderDO> orderWrapper = new LambdaQueryWrapperX<>();
            orderWrapper.eqIfPresent(TemuOrderDO::getOrderStatus, pageVO.getOrderStatus())
                    .eqIfPresent(TemuOrderDO::getOrderNo, pageVO.getOrderNo());
            matchedOrders = orderMapper.selectList(orderWrapper);

            if (CollectionUtils.isEmpty(matchedOrders)) {
                return new PageResult<>(new ArrayList<>(), 0L, pageVO.getPageNo(), pageVO.getPageSize());
            }
        }

        // 2. 构建物流信息查询条件
        LambdaQueryWrapperX<TemuOrderShippingInfoDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eqIfPresent(TemuOrderShippingInfoDO::getShopId, pageVO.getShopId())
                .likeIfPresent(TemuOrderShippingInfoDO::getTrackingNumber, pageVO.getTrackingNumber());

        // 处理创建时间条件
        if (pageVO.getCreateTime() != null && pageVO.getCreateTime().length == 2) {
            LocalDateTime startTime = pageVO.getCreateTime()[0].atStartOfDay();
            LocalDateTime endTime = pageVO.getCreateTime()[1].atTime(23, 59, 59, 999999999);
            queryWrapper.between(TemuOrderShippingInfoDO::getCreateTime, startTime, endTime);
        }

        if (matchedOrders != null) {
            Set<String> orderNos = matchedOrders.stream()
                    .map(TemuOrderDO::getOrderNo)
                    .collect(Collectors.toSet());
            queryWrapper.in(TemuOrderShippingInfoDO::getOrderId, orderNos);
        }

        queryWrapper.orderByDesc(TemuOrderShippingInfoDO::getCreateTime)
                .orderByDesc(TemuOrderShippingInfoDO::getId);

        // 3. 执行分页查询
        PageResult<TemuOrderShippingInfoDO> pageResult = shippingInfoMapper.selectPage(pageVO, queryWrapper);
        if (CollectionUtils.isEmpty(pageResult.getList())) {
            return new PageResult<>(new ArrayList<>(), pageResult.getTotal(), pageVO.getPageNo(), pageVO.getPageSize());
        }

        // 4. 获取所有需要的订单编号和店铺ID
        Set<String> allOrderNos = pageResult.getList().stream()
                .map(TemuOrderShippingInfoDO::getOrderId)
                .collect(Collectors.toSet());
        Set<Long> shopIds = pageResult.getList().stream()
                .map(TemuOrderShippingInfoDO::getShopId)
                .collect(Collectors.toSet());

        // 5. 批量查询订单信息
        List<TemuOrderDO> orders = orderMapper.selectList(new LambdaQueryWrapperX<TemuOrderDO>()
                .in(TemuOrderDO::getOrderNo, allOrderNos));
        Map<String, TemuOrderDO> orderMap = orders.stream()
                .collect(Collectors.toMap(TemuOrderDO::getOrderNo, Function.identity(), (v1, v2) -> v1));

        // 6. 批量查询店铺信息
        Map<Long, TemuShopDO> shopMap = new HashMap<>();
        for (Long shopId : shopIds) {
            TemuShopDO shop = shopMapper.selectByShopId(shopId);
            if (shop != null) {
                shopMap.put(shopId, shop);
            }
        }

        // 7. 组装返回结果
        List<TemuOrderShippingRespVO> voList = new ArrayList<>();
        for (TemuOrderShippingInfoDO shippingInfo : pageResult.getList()) {
            TemuOrderShippingRespVO vo = convertToRespVO(shippingInfo, orderMap, shopMap);
            if (vo != null) {
                voList.add(vo);
            }
        }

        return new PageResult<>(voList, pageResult.getTotal(), pageVO.getPageNo(), pageVO.getPageSize());
    }

    // 修改待发货订单状态
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateOrderStatus(TemuOrderShippingPageReqVO reqVO) {
        // 1. 参数校验
        if (reqVO.getOrderId() == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        // 2. 构建更新对象
        TemuOrderDO updateOrder = new TemuOrderDO();
        updateOrder.setId(reqVO.getOrderId());
        updateOrder.setOrderStatus(reqVO.getOrderStatus());
        updateOrder.setUpdateTime(LocalDateTime.now());
        // 3. 执行更新
        try {
            int rows = orderMapper.updateById(updateOrder);
            return rows > 0;
        } catch (Exception e) {
            log.error("[updateOrderStatus][orderId: {}] 更新订单状态失败", reqVO.getOrderId(), e);
            throw new ServiceException();
        }
    }

    @Override
    public int batchSaveOrderShipping(List<TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO> saveRequestVOs) {
        if (CollectionUtils.isEmpty(saveRequestVOs)) {
            return 0;
        }

        // 1. 参数校验
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
        }

        // 2. 批量查询已存在的订单
        Set<String> orderNos = saveRequestVOs.stream()
                .map(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO::getOrderNo)
                .collect(Collectors.toSet());

        List<TemuOrderShippingInfoDO> existingShippings = shippingInfoMapper.selectList(
                new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
                        .in(TemuOrderShippingInfoDO::getOrderId, orderNos));

        Set<String> existingOrderNos = existingShippings.stream()
                .map(TemuOrderShippingInfoDO::getOrderId)
                .collect(Collectors.toSet());

        // 3. 过滤出需要保存的订单
        List<TemuOrderShippingInfoDO> toSaveList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO : saveRequestVOs) {
            if (!existingOrderNos.contains(saveRequestVO.getOrderNo())) {
                TemuOrderShippingInfoDO shippingInfo = new TemuOrderShippingInfoDO();
                shippingInfo.setOrderId(saveRequestVO.getOrderNo()); // 使用orderNo作为orderId
                shippingInfo.setTrackingNumber(saveRequestVO.getTrackingNumber());
                shippingInfo.setExpressImageUrl(saveRequestVO.getExpressImageUrl());
                shippingInfo.setExpressOutsideImageUrl(saveRequestVO.getExpressOutsideImageUrl());
                shippingInfo.setExpressSkuImageUrl(saveRequestVO.getExpressSkuImageUrl());
                shippingInfo.setShopId(saveRequestVO.getShopId());
                shippingInfo.setCreateTime(now);
                shippingInfo.setUpdateTime(now);
                toSaveList.add(shippingInfo);
            } else {
                log.info("订单物流信息已存在，忽略保存，订单编号：{}", saveRequestVO.getOrderNo());
            }
        }

        // 4. 批量保存
        if (!toSaveList.isEmpty()) {
            try {
                int affectedRows = shippingInfoMapper.insertBatch(toSaveList);
                log.info("批量保存订单物流信息成功，数量：{}", affectedRows);
                return affectedRows;
            } catch (Exception e) {
                log.error("批量保存订单物流信息失败：{}", e.getMessage(), e);
                throw new ServiceException("批量保存订单物流信息失败：" + e.getMessage());
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

    // 校验保存请求参数
    private void validateSaveRequest(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO) {
        if (saveRequestVO == null) {
            throw new IllegalArgumentException("订单物流保存请求参数不能为空");
        }
    }

    // 构建物流信息对象
    private TemuOrderShippingInfoDO buildShippingInfo(
            TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO) {
        TemuOrderShippingInfoDO shippingInfo = new TemuOrderShippingInfoDO();
        shippingInfo.setOrderId(saveRequestVO.getOrderNo()); // 使用orderNo作为orderId
        shippingInfo.setTrackingNumber(saveRequestVO.getTrackingNumber());
        shippingInfo.setExpressImageUrl(saveRequestVO.getExpressImageUrl());
        shippingInfo.setExpressOutsideImageUrl(saveRequestVO.getExpressOutsideImageUrl());
        shippingInfo.setExpressSkuImageUrl(saveRequestVO.getExpressSkuImageUrl());
        shippingInfo.setShopId(saveRequestVO.getShopId());
        shippingInfo.setCreateTime(LocalDateTime.now());
        shippingInfo.setUpdateTime(LocalDateTime.now());
        return shippingInfo;
    }

    // 构建物流信息查询条件
    private LambdaQueryWrapperX<TemuOrderShippingInfoDO> buildShippingInfoWrapper(TemuOrderShippingPageReqVO pageVO,
            Set<String> orderIds) {
        LambdaQueryWrapperX<TemuOrderShippingInfoDO> wrapper = new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
                .in(TemuOrderShippingInfoDO::getOrderId, orderIds)
                .eqIfPresent(TemuOrderShippingInfoDO::getShopId, pageVO.getShopId())
                .likeIfPresent(TemuOrderShippingInfoDO::getTrackingNumber, pageVO.getTrackingNumber());

        // 处理创建时间范围查询
        if (pageVO.getCreateTime() != null && pageVO.getCreateTime().length == 2) {
            LocalDateTime startTime = pageVO.getCreateTime()[0].atStartOfDay();
            // 如果是同一天，结束时间要设置为当天的最后一毫秒
            LocalDateTime endTime = pageVO.getCreateTime()[1].atTime(23, 59, 59, 999999999);
            log.info("[getOrderShippingPage][时间范围查询][startTime: {}, endTime: {}]", startTime, endTime);
            wrapper.between(TemuOrderShippingInfoDO::getCreateTime, startTime, endTime);
        }

        // 添加排序条件
        wrapper.orderByDesc(TemuOrderShippingInfoDO::getCreateTime)
                .orderByDesc(TemuOrderShippingInfoDO::getId);
        return wrapper;
    }

    // 转换单个物流信息为响应VO
    private TemuOrderShippingRespVO convertToRespVO(TemuOrderShippingInfoDO shippingInfo,
            Map<String, TemuOrderDO> orderMap, Map<Long, TemuShopDO> shopMap) {
        TemuOrderShippingRespVO vo = BeanUtils.toBean(shippingInfo, TemuOrderShippingRespVO.class);

        // 设置订单相关信息
        String orderNo = shippingInfo.getOrderId();
        vo.setOrderNo(orderNo);
        if (orderNo != null) {
            List<TemuOrderDO> matchedOrders = orderMapper.selectList(
                    new LambdaQueryWrapperX<TemuOrderDO>()
                            .eq(TemuOrderDO::getOrderNo, orderNo));
            if (!CollectionUtils.isEmpty(matchedOrders)) {
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

        return vo;
    }

}
