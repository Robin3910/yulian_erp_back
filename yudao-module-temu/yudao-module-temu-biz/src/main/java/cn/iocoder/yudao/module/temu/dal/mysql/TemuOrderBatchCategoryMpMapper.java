package cn.iocoder.yudao.module.temu.dal.mysql;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderBatchCategoryMpDO;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import cn.iocoder.yudao.module.temu.controller.admin.vo.batchCategoryMp.TemuOrderBatchCategoryPageReqVO;

@Mapper
public interface TemuOrderBatchCategoryMpMapper extends BaseMapper<TemuOrderBatchCategoryMpDO> {
    // 可扩展自定义SQL
    List<TemuOrderBatchCategoryMpDO> selectPage(@Param("page") Page<?> page, @Param("reqVO") TemuOrderBatchCategoryPageReqVO reqVO);
}

