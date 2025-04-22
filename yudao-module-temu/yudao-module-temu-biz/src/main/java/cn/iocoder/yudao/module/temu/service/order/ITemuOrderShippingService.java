package cn.iocoder.yudao.module.temu.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingRespVO;
import java.util.List;

public interface ITemuOrderShippingService {

    Long saveOrderShipping(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO);

    Boolean updateOrderStatus(TemuOrderShippingPageReqVO reqVO);

    /**
     * 批量更新订单状态
     *
     * @param orderIds    订单ID列表
     * @param orderStatus 订单状态
     * @return 是否更新成功
     */
    Boolean batchUpdateOrderStatus(List<Long> orderIds, Integer orderStatus);

    /**
     * 获得待发货订单分页
     *
     * @param pageReqVO 分页查询参数
     * @return 待发货订单分页结果
     */
    PageResult<TemuOrderShippingRespVO> getOrderShippingPage(TemuOrderShippingPageReqVO pageVO);
    
    /**
     * 批量保存发货面单信息
     * @param saveRequestVOs
     * @return
     */
    int batchSaveOrderShipping(List<TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO> saveRequestVOs);
}
