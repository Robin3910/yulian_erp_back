package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopOldTypeSkcDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TemuShopOldTypeSkcMapper extends BaseMapperX<TemuShopOldTypeSkcDO> {

    /**
     * 根据shopId和skc查询记录，忽略逻辑删除标记
     */
    @Select("SELECT * FROM temu_shop_old_type_skc WHERE shop_id = #{shopId} AND skc = #{skc}")
    TemuShopOldTypeSkcDO selectByShopIdAndSkcWithoutDeleted(@Param("shopId") Long shopId, @Param("skc") String skc);

    /**
     * 更新记录，忽略逻辑删除标记
     */
    @Update("UPDATE temu_shop_old_type_skc SET shop_id = #{shopId}, skc = #{skc}, " +
            "old_type_url = #{oldTypeUrl}, old_type = #{oldType}, " +
            "old_type_image_url = #{oldTypeImageUrl}, deleted = #{deleted}, " +
            "update_time = #{updateTime}, updater = #{updater} " +
            "WHERE id = #{id}")
    int updateByIdWithoutDeleted(TemuShopOldTypeSkcDO entity);
}
