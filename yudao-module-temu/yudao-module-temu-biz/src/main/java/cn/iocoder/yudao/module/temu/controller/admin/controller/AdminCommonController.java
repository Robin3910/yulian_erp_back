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

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.TreeMap;

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

	@RequestMapping("/temu-open-api/logistics/query")
	@Operation(summary = "查询Temu平台物流信息")
	public CommonResult<?> queryTemuLogistics() {
		try {
			// 直接new TemuOpenApiUtil并set参数
			TemuOpenApiUtil openApiUtil = new TemuOpenApiUtil();
			openApiUtil.setAppKey("0ce5328c2c804c13db86534799af62b6");
			openApiUtil.setAppSecret("38453a5dfcead2fa62ab8a871906832c14129eb6");
			openApiUtil.setAccessToken("iek0f3k2dovntbdqkl8uyo1rcjljqirabawbcbls88tpesu67bhxp2vw");
			openApiUtil.setBaseUrl("https://openapi.kuajingmaihuo.com/openapi/router");

			// 调用新的getShipOrderList方法
			TreeMap<String, Object> params = new TreeMap<>();
			String apiResult = openApiUtil.getShipOrderList(params);
			
			return CommonResult.success(apiResult);
		} catch (Exception e) {
			return CommonResult.error(500, "物流查询失败: " + e.getMessage());
		}
	}

}
