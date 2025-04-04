package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.TemuCategoryPageReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品品类 Mapper
 */
@Mapper
public interface TemuProductCategoryMapper extends BaseMapperX<TemuProductCategoryDO> {

    default PageResult<TemuProductCategoryDO> selectPage(TemuCategoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TemuProductCategoryDO>()
                .likeIfPresent(TemuProductCategoryDO::getCategoryName, reqVO.getCategoryName())
                .eqIfPresent(TemuProductCategoryDO::getCategoryId, reqVO.getCategoryId())
                .orderByDesc(TemuProductCategoryDO::getId));
    }

    default TemuProductCategoryDO selectByCategoryId(Long categoryId) {
        return selectOne(new LambdaQueryWrapperX<TemuProductCategoryDO>()
                .eq(TemuProductCategoryDO::getCategoryId, categoryId));
    }
} 