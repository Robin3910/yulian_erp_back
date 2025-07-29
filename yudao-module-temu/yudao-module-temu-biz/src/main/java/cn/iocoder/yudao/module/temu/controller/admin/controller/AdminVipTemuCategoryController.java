package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryRespVO;
import cn.iocoder.yudao.module.temu.service.bigCustomer.TemuVipCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 商品品类")
@RestController
@RequestMapping("/temu/product-category-vip")
@Validated
public class AdminVipTemuCategoryController {
    @Resource
    private TemuVipCategoryService temuVipCategoryService;

    @GetMapping("/get")
    @Operation(summary = "获取商品品类信息")
    @Parameter(name = "id", description = "商品品类编号", required = true, example = "1024")
    // @PreAuthorize("@ss.hasPermission('temu:category:query')")
    public CommonResult<TemuCategoryRespVO> getCategory(@RequestParam("id") Long id) {
        return success(temuVipCategoryService.getCategory(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取商品品类分页")
    // @PreAuthorize("@ss.hasPermission('temu:category:query')")
    public CommonResult<PageResult<TemuCategoryRespVO>> getPageCategory(@Valid TemuCategoryPageReqVO pageVO) {
        return success(temuVipCategoryService.getCategoryPage(pageVO));
    }

    @PostMapping("/create")
    @Operation(summary = "创建商品品类")
    // @PreAuthorize("@ss.hasPermission('temu:category:create')")
    public CommonResult<Long> createCategory(@Valid @RequestBody TemuCategoryCreateReqVO createReqVO) {
        return success(temuVipCategoryService.createCategory(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新商品品类")
    @PreAuthorize("@ss.hasPermission('temu:product-category-vip:update')")
    public CommonResult<Boolean> updateProductCategory(@Valid @RequestBody TemuCategoryCreateReqVO updateReqVO) {
        temuVipCategoryService.updateProductCategory(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除商品品类")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('temu:product-category-vip:delete')")
    public CommonResult<Boolean> deleteProductCategory(@RequestParam("id") Long id) {
        temuVipCategoryService.deleteProductCategory(id);
        return success(true);
    }
}
