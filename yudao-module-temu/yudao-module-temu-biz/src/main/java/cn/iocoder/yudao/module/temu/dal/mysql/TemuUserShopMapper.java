package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.Map;


/**
 * 用户与店铺关系绑定 Mapper
 *
 * @author 禹链科技
 */
@Mapper
public interface TemuUserShopMapper extends BaseMapperX<cn.iocoder.yudao.module.temu.dal.dataobject.TemuUserShopDO> {
	int deleteByCloumnMap(HashMap<String, Object> map);
}