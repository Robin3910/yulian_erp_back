package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.temu.service.order.impl.CommonService;
import com.sun.org.apache.xpath.internal.operations.Bool;
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
	@Resource
	private CommonService commonService;
	@Resource
	private PermissionService permissionService;
	
	//	分类列表
	@RequestMapping("/category/list")
	public CommonResult<?> categoryList() {
		return CommonResult.success(commonService.list());
	}
	
	@RequestMapping("/shop/list")
	public CommonResult<?> shopList() {
		Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
		boolean isAdmin = permissionService.hasAnyRoles(loginUserId, "super_admin", "crm_admin");
		if (isAdmin) {
			return CommonResult.success(commonService.listShop());
		} else {
			return CommonResult.success(commonService.listShop(loginUserId));
		}
	}
	
	@RequestMapping("/test/temu-open-api")
	@PermitAll
	public CommonResult<?> testTemuOpenApi() {
		return CommonResult.success(commonService.testTemuOpenApi());
	}
}
