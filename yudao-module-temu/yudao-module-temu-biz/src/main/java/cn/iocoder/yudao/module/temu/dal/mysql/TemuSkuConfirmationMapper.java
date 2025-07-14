package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuSkuConfirmationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SKU确认 Mapper
 */
@Mapper
public interface TemuSkuConfirmationMapper extends BaseMapperX<TemuSkuConfirmationDO> {

    default TemuSkuConfirmationDO selectByShopIdAndSku(String shopId, String sku) {
        return selectOne(new LambdaQueryWrapperX<TemuSkuConfirmationDO>()
                .eq(TemuSkuConfirmationDO::getShopId, shopId)
                .eq(TemuSkuConfirmationDO::getSku, sku));
    }

    default List<TemuSkuConfirmationDO> selectListByShopId(String shopId) {
        return selectList(new LambdaQueryWrapperX<TemuSkuConfirmationDO>()
                .eq(TemuSkuConfirmationDO::getShopId, shopId)
                .eq(TemuSkuConfirmationDO::getStatus, 1));
    }
}