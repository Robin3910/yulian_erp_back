package cn.iocoder.yudao.module.temu.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingCountReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingCountRespVO;
import java.util.List;

public interface ITemuOrderShippingService {

    /**
     * 批量更新订单状态，同时更新对应的物流订单发货状态
     *
     * @param orderIds       订单ID列表
     * @param orderStatus    订单状态
     * @param trackingNumber 物流单号
     * @return 是否更新成功
     */
    Boolean batchUpdateOrderStatus(List<Long> orderIds, Integer orderStatus, String trackingNumber);

    /**
     * 获得待发货订单分页
     *
     * @param pageReqVO 分页查询参数
     * @return 待发货订单分页结果
     */
    PageResult<TemuOrderShippingRespVO> getOrderShippingPage(TemuOrderShippingPageReqVO pageVO);

    /**
     * 批量保存发货面单信息
     * 
     * @param saveRequestVOs
     * @return
     */
    int batchSaveOrderShipping(List<TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO> saveRequestVOs);

    /**
     * 根据用户ID分页查询待发货列表
     *
     * @param pageVO 分页查询参数
     * @param userId 用户ID
     * @return 待发货订单分页结果
     */
    PageResult<TemuOrderShippingRespVO> getOrderShippingPageByUser(TemuOrderShippingPageReqVO pageVO, Long userId);

    /**
     * 测试方法
     * 批量保存发货面单信息（附带编号）
     *
     * @param saveRequestVOs
     * @return
     */
    int batchSaveOrderShippingTest(List<TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO> saveRequestVOs);

    /**
     * 查询加急未发货订单总数
     *
     * @param reqVO 查询参数
     * @return 加急未发货订单总数
     */
    TemuOrderShippingCountRespVO getUrgentOrderCount(TemuOrderShippingCountReqVO reqVO);
}
