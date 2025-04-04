package cn.iocoder.yudao.module.temu.controller.admin.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.dal.dataobject.TemuProductCateGoryDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TemuProductCateGoryMapper extends BaseMapperX<TemuProductCateGoryDO> {
	default PageResult<TemuProductCateGoryDO> selectPage() {
		PageParam pageParam = new PageParam();
		pageParam.setPageNo(1);
		pageParam.setPageSize(-1);
		LambdaQueryWrapperX<TemuProductCateGoryDO> temuProductCateGoryDOLambdaQueryWrapperX = new LambdaQueryWrapperX<>();
		return selectPage(pageParam, temuProductCateGoryDOLambdaQueryWrapperX);
	}
}
