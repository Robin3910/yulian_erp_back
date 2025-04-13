package cn.iocoder.yudao.module.temu.service.user;

import cn.iocoder.yudao.module.temu.controller.admin.vo.user.UserBindShopReqVO;

import java.util.List;

public interface UserService {
	Boolean bindShop(UserBindShopReqVO reqVO);
}