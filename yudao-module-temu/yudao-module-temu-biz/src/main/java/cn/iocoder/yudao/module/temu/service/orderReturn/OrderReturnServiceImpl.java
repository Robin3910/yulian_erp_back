package cn.iocoder.yudao.module.temu.service.orderReturn;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn.OrderReturnPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn.OrderReturnPageRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn.OrderReturnUpdateReasonReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderReturnDO;
import cn.iocoder.yudao.module.temu.dal.mysql.OrderReturnMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderReturnMapper;
import cn.iocoder.yudao.module.temu.enums.ReturnReasonEnum;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import javax.annotation.Resource;
import java.util.List;


@Service
@Validated
public class OrderReturnServiceImpl implements OrderReturnService {
    @Resource
    private OrderReturnMapper orderReturnMapper;
    
    @Resource
    private TemuOrderReturnMapper temuOrderReturnMapper;

    @Override
    public PageResult<OrderReturnPageRespVO> getPage(OrderReturnPageReqVO reqVO) {
        reqVO.setOffset((reqVO.getPageNo() - 1) * reqVO.getPageSize());
        List<OrderReturnPageRespVO> list = orderReturnMapper.selectPage(reqVO);
        Long total = orderReturnMapper.selectCount(reqVO);
        return new PageResult<>(list, total);
    }

    @Override
    public Boolean updateReturnReason(OrderReturnUpdateReasonReqVO reqVO) {
        // 1. 校验返单记录是否存在
        TemuOrderReturnDO orderReturn = temuOrderReturnMapper.selectById(reqVO.getId());
        if (orderReturn == null) {
            throw new RuntimeException("返单记录不存在");
        }
        
        // 2. 校验返工原因是否有效
        ReturnReasonEnum reasonEnum = ReturnReasonEnum.getByValue(reqVO.getRepeatReason());
        if (reasonEnum == null) {
            throw new RuntimeException("无效的返工原因");
        }
        
        // 3. 更新返工原因
        TemuOrderReturnDO updateObj = new TemuOrderReturnDO();
        updateObj.setId(reqVO.getId());
        updateObj.setRepeatReason(reqVO.getRepeatReason());
        
        return temuOrderReturnMapper.updateById(updateObj) > 0;
    }


} 