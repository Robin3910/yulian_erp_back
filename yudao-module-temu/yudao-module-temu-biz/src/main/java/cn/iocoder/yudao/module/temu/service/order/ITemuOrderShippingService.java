package cn.iocoder.yudao.module.temu.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingRespVO;
import java.util.List;

public interface ITemuOrderShippingService {

    Long saveOrderShipping(TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO);

    /**
     * 获得待发货订单分页
     * @param pageVO 分页查询
     * @return 待发货订单分页
     */
    PageResult<TemuOrderShippingRespVO> getOrderShippingPage(TemuOrderShippingPageReqVO pageVO);

    Boolean updateOrderStatus(TemuOrderShippingPageReqVO reqVO);

    /**
     * 批量保存待发货订单
     * @param saveRequestVOs 待发货订单列表
     * @return 成功保存的数量
     */
    int batchSaveOrderShipping(List<TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO> saveRequestVOs);
}
