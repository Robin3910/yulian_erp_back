package cn.iocoder.yudao.module.temu.service.user.impl;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.temu.controller.admin.vo.user.UserBindShopReqVO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuUserShopMapper;
import cn.iocoder.yudao.module.temu.service.user.UserService;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuUserShopDO;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

@Log
@Service
public class UserServiceImpl implements UserService {
	@Resource
	private TemuUserShopMapper temuUserShopMapper;
	
	@Override
	@Transactional
	public Boolean bindShop(List<UserBindShopReqVO> reqVO) {
		//删除当前关联的所有用户的记录
		HashMap<String, Object> map = new HashMap<>();
		map.put("userId", reqVO.get(0).getUserId());
		temuUserShopMapper.deleteByCloumnMap(map);
		//批量插入
		for (UserBindShopReqVO userBindShopReqVO : reqVO) {
			TemuUserShopDO temuUserShopDO = BeanUtils.toBean(userBindShopReqVO, TemuUserShopDO.class);
			temuUserShopDO.setDeleted(false);
			temuUserShopMapper.insert(temuUserShopDO);
		}
		return true;
	}
}
