package cn.iocoder.yudao.module.temu.dal.mysql;


import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingPageReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderShippingInfoDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Mapper
public interface TemuOrderShippingMapper extends BaseMapperX<TemuOrderShippingInfoDO> {

    /**
     * 分页查询待发货订单
     *
     * @return
     */
    default PageResult<TemuOrderShippingInfoDO> selectPage(TemuOrderShippingPageReqVO pageReqVO) {
        LambdaQueryWrapperX<TemuOrderShippingInfoDO> wrapper = new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
                .eqIfPresent(TemuOrderShippingInfoDO::getShopId, pageReqVO.getShopId())
                .likeIfPresent(TemuOrderShippingInfoDO::getTrackingNumber, pageReqVO.getTrackingNumber())
                .eqIfPresent(TemuOrderShippingInfoDO::getOrderId, pageReqVO.getOrderNo())
                .orderByDesc(TemuOrderShippingInfoDO::getCreateTime)
                .orderByDesc(TemuOrderShippingInfoDO::getId);

        if (pageReqVO.getCreateTime() != null && pageReqVO.getCreateTime().length == 2) {
            wrapper.between(TemuOrderShippingInfoDO::getCreateTime,
                    LocalDateTime.of(pageReqVO.getCreateTime()[0], LocalTime.MIN),
                    LocalDateTime.of(pageReqVO.getCreateTime()[1], LocalTime.MAX));
        }

        return selectPage(pageReqVO, wrapper);
    }
}