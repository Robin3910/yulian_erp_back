package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderReworkDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 订单返工 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface TemuOrderReworkMapper extends BaseMapperX<TemuOrderReworkDO> {



    @Update("UPDATE temu_order_rework SET is_finished=1 WHERE custom_sku=#{customSku}")
    int finishReworkByCustomSku(@Param("customSku") String customSku);

    // 新增：强制将 rework_initiator_name 置为 null（只清空发起人，保留作图人信息）
    @Update("UPDATE temu_order_rework SET rework_initiator_name = NULL WHERE custom_sku = #{customSku}")
    int clearReworkNamesByCustomSku(@Param("customSku") String customSku);

    // 新增：更新上一次作图人信息
    @Update("UPDATE temu_order_rework SET last_draw_user_name=#{lastDrawUserName}, last_draw_user_id=#{lastDrawUserId} WHERE custom_sku=#{customSku}")
    int updateLastDrawUserInfo(@Param("customSku") String customSku, @Param("lastDrawUserName") String lastDrawUserName, @Param("lastDrawUserId") Long lastDrawUserId);

    // 新增：更新返工发起人信息
    @Update("UPDATE temu_order_rework SET rework_initiator_name=#{reworkInitiatorName} WHERE id=#{reworkId}")
    int updateReworkInitiator(@Param("reworkId") Long reworkId, @Param("reworkInitiatorName") String reworkInitiatorName);

    // 新增：更新返工作图人信息
    @Update("UPDATE temu_order_rework SET rework_draw_user_name=#{reworkDrawUserName}, rework_draw_user_id=#{reworkDrawUserId} WHERE id=#{reworkId}")
    int updateReworkDrawUser(@Param("reworkId") Long reworkId, @Param("reworkDrawUserName") String reworkDrawUserName, @Param("reworkDrawUserId") Long reworkDrawUserId);
} 