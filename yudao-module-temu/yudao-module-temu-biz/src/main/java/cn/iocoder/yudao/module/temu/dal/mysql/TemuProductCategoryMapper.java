package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryPageReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 商品品类 Mapper
 */
@Mapper
public interface TemuProductCategoryMapper extends BaseMapperX<TemuProductCategoryDO> {

    default PageResult<TemuProductCategoryDO> selectPage() {
		PageParam pageParam = new PageParam();
		pageParam.setPageNo(1);
		pageParam.setPageSize(-1);
		LambdaQueryWrapperX<TemuProductCategoryDO> temuProductCateGoryDOLambdaQueryWrapperX = new LambdaQueryWrapperX<>();
		return selectPage(pageParam, temuProductCateGoryDOLambdaQueryWrapperX);
	}

    default PageResult<TemuProductCategoryDO> selectPageByParams(TemuCategoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TemuProductCategoryDO>()
                .likeIfPresent(TemuProductCategoryDO::getCategoryName, reqVO.getCategoryName())
                .eqIfPresent(TemuProductCategoryDO::getCategoryId, reqVO.getCategoryId())
		        .betweenIfPresent(TemuProductCategoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TemuProductCategoryDO::getId));
    }

    default TemuProductCategoryDO selectByCategoryId(Long categoryId) {
        return selectOne(new LambdaQueryWrapperX<TemuProductCategoryDO>()
                .eq(TemuProductCategoryDO::getCategoryId, categoryId));
    }
} 