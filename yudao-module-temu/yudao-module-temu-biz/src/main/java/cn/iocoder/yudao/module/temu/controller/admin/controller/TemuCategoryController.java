package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryRespVO;
import cn.iocoder.yudao.module.temu.service.category.TemuCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.annotation.security.PermitAll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 商品品类")
@RestController
@RequestMapping("/temu/category")
@Validated
@PermitAll
public class TemuCategoryController {

	@Resource
	private TemuCategoryService temuCategoryService;

	@GetMapping("/get")
	@Operation(summary = "获取商品品类信息")
	@Parameter(name = "id", description = "商品品类编号", required = true, example = "1024")
	// @PreAuthorize("@ss.hasPermission('temu:category:query')")
	public CommonResult<TemuCategoryRespVO> getCategory(@RequestParam("id") Long id) {
		return success(temuCategoryService.getCategory(id));
	}
	
	@GetMapping("/page")
	@Operation(summary = "获取商品品类分页")
	// @PreAuthorize("@ss.hasPermission('temu:category:query')")
	public CommonResult<PageResult<TemuCategoryRespVO>> getPageCategory(@Valid TemuCategoryPageReqVO pageVO) {
		return success(temuCategoryService.getCategoryPage(pageVO));
	}

	@PostMapping("/create")
	@Operation(summary = "创建商品品类")
	// @PreAuthorize("@ss.hasPermission('temu:category:create')")
	public CommonResult<Long> createCategory(@Valid @RequestBody TemuCategoryCreateReqVO createReqVO) {
		return success(temuCategoryService.createCategory(createReqVO));
	}
}