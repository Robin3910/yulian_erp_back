package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuOpenapiShopPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuOpenapiShopSaveReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOpenapiShopDO;
import cn.iocoder.yudao.module.temu.service.order.impl.CommonService;
import cn.iocoder.yudao.module.temu.utils.openapi.TemuOpenApiUtil;
import java.util.TreeMap;
import com.sun.org.apache.xpath.internal.operations.Bool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.stock.TemuStockPreparationPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.stock.TemuStockPreparationVO;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.TreeMap;
import cn.iocoder.yudao.module.temu.service.stock.TemuStockPreparationService;

@Tag(name = "Temu管理 - 公共参数列表")
@RestController
@RequestMapping("/temu/common")
@Validated
public class AdminCommonController {
	@Resource
	private CommonService commonService;
	@Resource
	private PermissionService permissionService;
	@Resource
	private TemuStockPreparationService stockPreparationService;
	
	//	分类列表
	@RequestMapping("/category/list")
	public CommonResult<?> categoryList() {
		return CommonResult.success(commonService.list());
	}
	
	@RequestMapping("/shop/list")
	public CommonResult<?> shopList() {
		Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
		boolean isAdmin = permissionService.hasAnyRoles(loginUserId, "super_admin", "crm_admin", "art_staff", "production_staff", "运营人员");
//		return CommonResult.success(commonService.listShop());
		if (isAdmin) {
			return CommonResult.success(commonService.listShop());
		} else {
			return CommonResult.success(commonService.listShop(loginUserId));
		}
	}
	
	//根据角色标识获取角色下所有用户
	@RequestMapping("/role/get-user-by-role-code")
	public CommonResult<?> getUserByRoleCode(@RequestParam("roleCode") @NotNull String roleCode) {
		return CommonResult.success(commonService.getUserByRoleCode(roleCode));
	}
	
	@RequestMapping("/test/temu-open-api")
	@PermitAll
	public CommonResult<?> testTemuOpenApi() {
		return null;
		// return CommonResult.success(commonService.testTemuOpenApi());
	}

	@RequestMapping("/temu-open-api/shopapi")
	@Operation(summary = "保存店铺授权信息")
	public CommonResult<String> saveShopApi(@Valid @RequestBody TemuOpenapiShopSaveReqVO reqVO) {
		// 构造DO对象
		TemuOpenapiShopDO shopDO = new TemuOpenapiShopDO();
		shopDO.setToken(reqVO.getToken());
		shopDO.setShopName(reqVO.getShopName());
		shopDO.setShopId(reqVO.getShopId());
		shopDO.setOwner(reqVO.getOwner());
		shopDO.setPlatform(reqVO.getPlatform());
		shopDO.setAppKey(reqVO.getAppKey());
		shopDO.setAppSecret(reqVO.getAppSecret());
		// 保存到数据库
		commonService.saveTemuOpenapiShop(shopDO);
		return CommonResult.success("保存成功");
	}

	@RequestMapping("/temu-open-api/shopapi/page")
	@Operation(summary = "分页查询店铺授权信息")
	public CommonResult<?> getShopApiPage(@Valid TemuOpenapiShopPageReqVO reqVO) {
		return CommonResult.success(commonService.getTemuOpenapiShopPage(reqVO));
	}

	@PostMapping("/temu-open-api/stock-preparation/page")
	@Operation(summary = "查询备货单列表")
	public CommonResult<PageResult<TemuStockPreparationVO>> getStockPreparationPage(@Valid @RequestBody TemuStockPreparationPageReqVO reqVO) {
		try {
			return CommonResult.success(stockPreparationService.getStockPreparationPage(reqVO));
		} catch (Exception e) {
			return CommonResult.error(500, "备货单查询失败: " + e.getMessage());
		}
	}

}
