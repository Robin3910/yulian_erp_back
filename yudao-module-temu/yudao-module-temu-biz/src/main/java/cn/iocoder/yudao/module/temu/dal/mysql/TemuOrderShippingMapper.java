package cn.iocoder.yudao.module.temu.dal.mysql;


import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingPageReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderShippingInfoDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

/**
 * 待发货列表 Mapper
 */
@Mapper
public interface TemuOrderShippingMapper extends BaseMapperX<TemuOrderShippingInfoDO> {

    /**
     * 根据订单ID列表查询待发货订单
     */
    // default List<TemuOrderShippingInfoDO> selectListByOrderIds(@Param("orderIds") Collection<Long> orderIds) {
    //     return selectList(new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
    //             .in(TemuOrderShippingInfoDO::getOrderNo, orderIds));
    // }

    /**
     * 根据shopId和trackingNumber查询待发货订单
     */
    default PageResult<TemuOrderShippingInfoDO> selectPage(TemuOrderShippingPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
                .eqIfPresent(TemuOrderShippingInfoDO::getShopId, pageReqVO.getShopId())
                .likeIfPresent(TemuOrderShippingInfoDO::getTrackingNumber, pageReqVO.getTrackingNumber())
                .orderByDesc(TemuOrderShippingInfoDO::getCreateTime)
                .orderByDesc(TemuOrderShippingInfoDO::getId));
    }

    /**
     * 批量插入待发货订单
     * @param list 待发货订单列表
     * @return 影响行数
     */
    // default int insertBatch(List<TemuOrderShippingInfoDO> list) {
    //     if (CollectionUtils.isEmpty(list)) {
    //         return 0;
    //     }
    //     return insertBatch(list);
    // }

    /**
     * 批量插入订单物流信息
     * @param list 待插入的数据列表
     * @return 插入成功的行数
     */
    @Insert({
            "<script>",
            "INSERT INTO temu_order_shipping_info ",
            "(order_no, tracking_number, express_image_url, express_outside_image_url, express_sku_image_url, shop_id, shipping_status, create_time, update_time) ",
            "VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.orderNo}, #{item.trackingNumber}, #{item.expressImageUrl}, ",
            "#{item.expressOutsideImageUrl}, #{item.expressSkuImageUrl}, #{item.shopId}, #{item.shippingStatus}, ",
            "#{item.createTime}, #{item.updateTime})",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("list") List<TemuOrderShippingInfoDO> list);

    /**
     * 根据订单号列表查询物流信息
     * @param orderNos 订单号列表
     * @return 物流信息列表
     */
    default List<TemuOrderShippingInfoDO> selectListByOrderNos(@Param("orderNos") Collection<String> orderNos) {
        return selectList(new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
                .in(TemuOrderShippingInfoDO::getOrderNo, orderNos));
    }

}
