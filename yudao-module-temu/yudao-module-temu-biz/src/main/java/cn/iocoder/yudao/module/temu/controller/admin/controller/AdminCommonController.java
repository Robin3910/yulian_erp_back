package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.temu.service.order.impl.CommonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;

@Tag(name = "Temu管理 - 公共参数列表")
@RestController
@RequestMapping("/temu/common")
@Validated
public class AdminCommonController {
	@Resource private CommonService commonService;
	//	分类列表
	@RequestMapping("/category/list")
	@PermitAll
	public CommonResult<?> categoryList() {
		return CommonResult.success(commonService.list());
	}
	@RequestMapping("/shop/list")
	@PermitAll
	public CommonResult<?> shopList() {
		return CommonResult.success(commonService.listShop());
	}
}
