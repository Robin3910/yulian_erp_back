package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.TemuCategorySkuCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.TemuCategorySkuPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.TemuCategorySkuRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.TemuCategorySkuUpdateReqVO;
import cn.iocoder.yudao.module.temu.service.categorySku.TemuCategorySkuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import java.util.Collection;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 商品品类SKU关系")
@RestController
@RequestMapping("/temu/category-sku")
@Validated
@PermitAll
public class TemuCategorySkuController {

    @Resource
    private TemuCategorySkuService categorySkuService;

    @PostMapping("/create")
    @Operation(summary = "创建商品品类SKU关系")
    // @PreAuthorize("@ss.hasPermission('temu:category-sku:create')")
    public CommonResult<Long> createCategorySku(@Valid @RequestBody TemuCategorySkuCreateReqVO createReqVO) {
        return success(categorySkuService.createCategorySku(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新商品品类SKU关系")
    // @PreAuthorize("@ss.hasPermission('temu:category-sku:update')")
    public CommonResult<Boolean> updateCategorySku(@Valid @RequestBody TemuCategorySkuUpdateReqVO updateReqVO) {
        categorySkuService.updateCategorySku(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除商品品类SKU关系")
    @Parameter(name = "id", description = "编号", required = true)
    // @PreAuthorize("@ss.hasPermission('temu:category-sku:delete')")
    public CommonResult<Boolean> deleteCategorySku(@RequestParam("id") Long id) {
        categorySkuService.deleteCategorySku(id);
        return success(true);
    }

    @DeleteMapping("/delete-batch")
    @Operation(summary = "批量删除商品品类SKU关系")
    @Parameter(name = "ids", description = "编号集合", required = true)
    // @PreAuthorize("@ss.hasPermission('temu:category-sku:delete')")
    public CommonResult<Boolean> deleteCategorySkus(@RequestParam("ids") Collection<Long> ids) {
        categorySkuService.deleteCategorySkus(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得商品品类SKU关系")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    // @PreAuthorize("@ss.hasPermission('temu:category-sku:query')")
    public CommonResult<TemuCategorySkuRespVO> getCategorySku(@RequestParam("id") Long id) {
        return success(categorySkuService.getCategorySku(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得商品品类SKU关系分页")
    // @PreAuthorize("@ss.hasPermission('temu:category-sku:query')")
    public CommonResult<PageResult<TemuCategorySkuRespVO>> getCategorySkuPage(@Valid TemuCategorySkuPageReqVO pageVO) {
        return success(categorySkuService.getCategorySkuPage(pageVO));
    }

    @GetMapping("/get-by-shop-sku")
    @Operation(summary = "根据店铺ID和SKU获得商品品类关系")
    @Parameter(name = "shopId", description = "店铺ID", required = true, example = "634418216202223")
    @Parameter(name = "sku", description = "SKU", required = false, example = "5181898930")
    // @PreAuthorize("@ss.hasPermission('temu:category-sku:query')")
    public CommonResult<TemuCategorySkuRespVO> getCategorySkuByShopIdAndSku(
            @RequestParam("shopId") Long shopId,
            @RequestParam("sku") String sku) {
        return success(categorySkuService.getCategorySkuByShopIdAndSku(shopId, sku));
    }
} 