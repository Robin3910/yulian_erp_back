package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderBatchCategoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TemuOrderBatchCategoryMapper extends BaseMapperX<TemuOrderBatchCategoryDO> {

    /**
     * 根据分类ID列表批量查询批次分类信息
     *
     * @param categoryIds 分类ID列表
     * @return 批次分类信息列表
     */
    List<TemuOrderBatchCategoryDO> selectByCategoryIds(@Param("categoryIds") List<String> categoryIds);
}