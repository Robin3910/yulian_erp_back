package cn.iocoder.yudao.module.system.dal.mysql.user;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;

import cn.iocoder.yudao.module.system.dal.dataobject.temu.AdminTemuShopDO;
import cn.iocoder.yudao.module.system.dal.dataobject.temu.AdminTemuUserShopDO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AdminTemuShopMapper extends BaseMapperX<AdminTemuShopDO> {
	default List<AdminTemuShopDO> getShopList(Long userId) {
		MPJLambdaWrapper<AdminTemuShopDO> temuShopDOMPJLambdaWrapper = new MPJLambdaWrapper<>();
		temuShopDOMPJLambdaWrapper.selectAll(AdminTemuShopDO.class)
				.innerJoin(AdminTemuUserShopDO.class, AdminTemuUserShopDO::getShopId, AdminTemuShopDO::getShopId)
				.eq(AdminTemuUserShopDO::getUserId, userId);
		return selectJoinList(AdminTemuShopDO.class,temuShopDOMPJLambdaWrapper);
	}
	
} 