package cn.iocoder.yudao.module.temu.service.orderReturn;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn.OrderReturnPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn.OrderReturnPageRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn.OrderReturnUpdateReasonReqVO;

public interface OrderReturnService {
    PageResult<OrderReturnPageRespVO> getPage(OrderReturnPageReqVO reqVO);
    
    /**
     * 修改返工原因
     *
     * @param reqVO 修改信息
     * @return 是否成功
     */
    Boolean updateReturnReason(OrderReturnUpdateReasonReqVO reqVO);
    

} 