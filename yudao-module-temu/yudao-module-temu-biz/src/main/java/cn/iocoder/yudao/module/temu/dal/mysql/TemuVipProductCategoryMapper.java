package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryPageReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuVipProductCategoryDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TemuVipProductCategoryMapper extends BaseMapperX<TemuVipProductCategoryDO> {
    default PageResult<TemuVipProductCategoryDO> selectPage() {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(1);
        pageParam.setPageSize(-1);
        LambdaQueryWrapperX<TemuVipProductCategoryDO> temuVipProductCateGoryDOLambdaQueryWrapperX = new LambdaQueryWrapperX<>();
        return selectPage(pageParam, temuVipProductCateGoryDOLambdaQueryWrapperX);
    }

    default PageResult<TemuVipProductCategoryDO> selectPageByParams(TemuCategoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TemuVipProductCategoryDO>()
                .likeIfPresent(TemuVipProductCategoryDO::getCategoryName, reqVO.getCategoryName())
                .eqIfPresent(TemuVipProductCategoryDO::getCategoryId, reqVO.getCategoryId())
                .betweenIfPresent(TemuVipProductCategoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TemuVipProductCategoryDO::getId));
    }

    default TemuVipProductCategoryDO selectByCategoryId(Long categoryId) {
        return selectOne(new LambdaQueryWrapperX<TemuVipProductCategoryDO>()
                .eq(TemuVipProductCategoryDO::getCategoryId, categoryId));
    }
}
