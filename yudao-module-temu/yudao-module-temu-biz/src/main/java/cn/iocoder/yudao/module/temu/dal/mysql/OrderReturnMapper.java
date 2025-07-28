package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn.OrderReturnPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn.OrderReturnPageRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OrderReturnMapper {
    List<OrderReturnPageRespVO> selectPage(@Param("reqVO") OrderReturnPageReqVO reqVO);
    Long selectCount(@Param("reqVO") OrderReturnPageReqVO reqVO);
} 