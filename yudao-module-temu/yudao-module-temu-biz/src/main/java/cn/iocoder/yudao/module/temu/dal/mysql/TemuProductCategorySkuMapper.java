package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategorySkuPageReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategorySkuDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品品类SKU关系 Mapper
 */
@Mapper
public interface TemuProductCategorySkuMapper extends BaseMapperX<TemuProductCategorySkuDO> {

    default PageResult<TemuProductCategorySkuDO> selectPage(TemuCategorySkuPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TemuProductCategorySkuDO>()
                .eqIfPresent(TemuProductCategorySkuDO::getCategoryId, reqVO.getCategoryId())
                .likeIfPresent(TemuProductCategorySkuDO::getSku, reqVO.getSku())
                .eqIfPresent(TemuProductCategorySkuDO::getShopId, reqVO.getShopId())
                .orderByDesc(TemuProductCategorySkuDO::getId));
    }
    
    default TemuProductCategorySkuDO selectByCategoryIdAndSku(Long categoryId, String sku) {
        return selectOne(new LambdaQueryWrapperX<TemuProductCategorySkuDO>()
                .eq(TemuProductCategorySkuDO::getCategoryId, categoryId)
                .eq(TemuProductCategorySkuDO::getSku, sku));
    }

    default TemuProductCategorySkuDO selectByShopIdAndSku(Long shopId, String sku) {
        return selectOne(new LambdaQueryWrapperX<TemuProductCategorySkuDO>()
                .eq(TemuProductCategorySkuDO::getShopId, shopId)
                .eq(TemuProductCategorySkuDO::getSku, sku));
    }
} 