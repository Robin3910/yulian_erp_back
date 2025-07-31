package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderApiDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TemuOrderApiMapper extends BaseMapper<TemuOrderApiDO> {
    
    @Select("SELECT * FROM temu_order_api WHERE custom_sku = #{customSku} LIMIT 1")
    TemuOrderApiDO selectByCustomSku(String customSku);

    @Select("SELECT * FROM temu_order_api WHERE custom_sku = #{customSku} AND order_no = #{orderNo} LIMIT 1")
    TemuOrderApiDO selectByCustomSkuAndOrderNo(String customSku, String orderNo);
} 