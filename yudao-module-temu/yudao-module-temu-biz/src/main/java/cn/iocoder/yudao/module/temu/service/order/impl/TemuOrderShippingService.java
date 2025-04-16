package cn.iocoder.yudao.module.temu.service.order.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderShippingInfoDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderShippingMapper;
import cn.iocoder.yudao.module.temu.service.order.ITemuOrderShippingService;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TemuOrderShippingService implements ITemuOrderShippingService {

    private final TemuOrderShippingMapper shippingInfoMapper;
    private final TemuOrderMapper orderMapper;

    /**
     * 保存待发货订单信息
     * @param saveRequestVO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrderShipping(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO) {
        // 1. 参数校验
        if (saveRequestVO == null) {
            throw new IllegalArgumentException("订单物流保存请求参数不能为空");
        }
        try {
            // 2. 对象属性拷贝
            TemuOrderShippingInfoDO shippingInfo = new TemuOrderShippingInfoDO();
            BeanUtils.copyProperties(saveRequestVO, shippingInfo);
            // 3. 设置审计字段
            LocalDateTime now = LocalDateTime.now();
            shippingInfo.setCreateTime(now);
            shippingInfo.setUpdateTime(now);
            // 4. 数据库插入操作
            int affectedRows = shippingInfoMapper.insert(shippingInfo);
            if (affectedRows == 0) {
                throw new PersistenceException("订单物流信息保存失败");
            }
            // 5. 验证生成的主键ID
            Long id = shippingInfo.getId();
            if (id == null || id <= 0L) {
                throw new PersistenceException("生成的主键ID无效");
            }
            log.info("订单物流信息保存成功，ID：{}", id);
            return id;
        } catch (DataAccessException e) {
            log.error("保存订单物流信息时发生数据库异常：{}", e.getMessage());
            throw new PersistenceException("数据库操作失败，原因：" + e.getMessage(), e);
        }
    }

    /**
     * 分页查询待发货列表
     * @param pageVO 分页查询
     * @return
     */
    @Override
    public PageResult<TemuOrderShippingRespVO> getOrderShippingPage(TemuOrderShippingPageReqVO pageVO) {
        // 1. 分页查询物流信息（核心分页逻辑在Mapper中处理）
        PageResult<TemuOrderShippingInfoDO> result = shippingInfoMapper.selectPage(pageVO);
        // 2. 处理空结果集短路返回
        if (CollectionUtils.isEmpty(result.getList())) {
            return PageResult.empty();
        }
        // 3. 提取关联订单ID集合
        Set<String> orderIds = result.getList().parallelStream()
                .map(TemuOrderShippingInfoDO::getOrderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));  // 保持顺序依赖
        // 4. 批量获取订单主体信息
        Map<String, TemuOrderDO> orderMap = orderMapper.selectByIds(orderIds)
                .stream()
                .collect(Collectors.toMap(
                        TemuOrderDO::getOrderNo,
                        Function.identity(),
                        (existing, replacement) -> existing));  // 处理重复ID冲突
        // 5. 构建最终响应对象
        List<TemuOrderShippingRespVO> voList = new ArrayList<>(result.getList().size());
        result.getList().forEach(shippingInfo -> {
            // 5.1 基础信息拷贝
            TemuOrderShippingRespVO vo = BeanUtils.toBean(shippingInfo, TemuOrderShippingRespVO.class);
            // 5.2 补充订单主体信息
            Optional.ofNullable(orderMap.get(shippingInfo.getOrderId()))
                    .ifPresent(order -> {
                        vo.setOrderNo(order.getOrderNo());
                        vo.setProductImgUrl(order.getProductImgUrl());
                        vo.setSku(order.getSku());
                        vo.setSkc(order.getSkc());
                        vo.setCustomSku(order.getCustomSku());
                        vo.setQuantity(order.getQuantity());
                        vo.setCustomImageUrls(order.getCustomImageUrls());
                        vo.setProductTitle(order.getProductTitle());
                        vo.setProductProperties(order.getProductProperties());
                        vo.setOrderStatus(order.getOrderStatus());
                        vo.setCustomTextList(order.getCustomTextList());
                        vo.setEffectiveImgUrl(order.getEffectiveImgUrl());
                    });

            voList.add(vo);
        });
        // 6. 返回标准化分页结果
        return new PageResult<>(
                voList,
                result.getTotal(),     // 保持原始总记录数
                pageVO.getPageNo(),    // 使用请求页码
                pageVO.getPageSize()   //分页大小
        );
    }
}


