package cn.iocoder.yudao.module.temu.service.user.impl;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.temu.controller.admin.vo.user.UserBindShopReqVO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuUserShopMapper;
import cn.iocoder.yudao.module.temu.service.user.UserService;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.iocoder.yudao.module.temu.dal.dataobject.usershop.TemuUserShopDO;

import javax.annotation.Resource;
import java.util.HashMap;
@Log
@Service
public class UserServiceImpl implements UserService {
	@Resource
	private TemuUserShopMapper temuUserShopMapper;
	
	@Override
	@Transactional
	public Boolean bindShop(UserBindShopReqVO reqVO) {
		HashMap<String, Object> map = new HashMap<>();
		map.put("userId", reqVO.getUserId());
		map.put("shopIp", reqVO.getShopId());
		temuUserShopMapper.deleteByCloumnMap(map);
		TemuUserShopDO updateObj = BeanUtils.toBean(reqVO, TemuUserShopDO.class);
		updateObj.setDeleted(false);
		return temuUserShopMapper.insert(updateObj) > 1;
	}
}
