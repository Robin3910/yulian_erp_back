package cn.iocoder.yudao.module.temu.dal.mysql;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderBatchCategoryMpDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TemuOrderBatchCategoryMpMapper extends BaseMapper<TemuOrderBatchCategoryMpDO> {
    // 可扩展自定义SQL
}

