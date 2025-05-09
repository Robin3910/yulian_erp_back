package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.user.UserBindShopReqVO;
import cn.iocoder.yudao.module.temu.service.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "Temu管理 - 用户管理")
@RestController
@RequestMapping("/temu/user")
@Validated
public class AdminTemuUserController {
	@Resource
	private UserService userService;
	
	@RequestMapping("/bind-shop")
	public CommonResult<?> bindShop(@RequestBody List<UserBindShopReqVO> reqVO) {
		return CommonResult.success(userService.bindShop(reqVO));
	}
	
}
