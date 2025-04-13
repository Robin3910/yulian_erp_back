package cn.iocoder.yudao.module.system.dal.mysql.user;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.temu.AdminTemuUserShopDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;


/**
 * 用户与店铺关系绑定 Mapper
 *
 * @author 禹链科技
 */
@Mapper
public interface AdminTemuUserShopMapper extends BaseMapperX<AdminTemuUserShopDO> {
}