package cn.iocoder.yudao.module.temu.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingRespVO;

public interface ITemuOrderShippingService {

    Long saveOrderShipping(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO);

    /**
     * 获得待发货订单分页
     *
     * @param pageVO 分页查询
     * @return 待发货订单分页
     */
    PageResult<TemuOrderShippingRespVO> getOrderShippingPage(TemuOrderShippingPageReqVO pageVO);
}
