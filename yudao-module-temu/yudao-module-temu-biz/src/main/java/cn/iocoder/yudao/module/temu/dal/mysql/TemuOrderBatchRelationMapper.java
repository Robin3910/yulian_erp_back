package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderBatchRelationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TemuOrderBatchRelationMapper extends BaseMapperX<TemuOrderBatchRelationDO> {
    
    /**
     * 根据订单ID删除关联关系
     *
     * @param orderId 订单ID
     * @return 删除的记录数
     */
    int deleteByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据订单ID批量查询关联关系
     * @param orderIds 订单ID列表
     * @return 关联关系列表
     */
    List<TemuOrderBatchRelationDO> selectByOrderIds(@Param("orderIds") List<Long> orderIds);
}
