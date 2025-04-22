package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopUpdateReqVO;
import cn.iocoder.yudao.module.temu.service.shop.TemuShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - Temu店铺")
@RestController
@RequestMapping("/temu/shop")
@Validated
@PermitAll
public class AdminTemuShopController {
    
    @Resource
    private TemuShopService temuShopService;
    
    @PostMapping("/create")
    @Operation(summary = "创建店铺")
    public CommonResult<Long> createShop(@Valid @RequestBody TemuShopCreateReqVO createReqVO) {
        return success(temuShopService.createShop(createReqVO));
    }
    
    @PutMapping("/update")
    @Operation(summary = "更新店铺")
    public CommonResult<Boolean> updateShop(@Valid @RequestBody TemuShopUpdateReqVO updateReqVO) {
        temuShopService.updateShop(updateReqVO);
        return success(true);
    }
    
    @DeleteMapping("/delete")
    @Operation(summary = "删除店铺")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteShop(@RequestParam("id") Long id) {
        temuShopService.deleteShop(id);
        return success(true);
    }
    
    @GetMapping("/get")
    @Operation(summary = "获得店铺")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<TemuShopRespVO> getShop(@RequestParam("id") Long id) {
        return success(temuShopService.getShop(id));
    }
    
    @GetMapping("/page")
    @Operation(summary = "获得店铺分页")
    public CommonResult<PageResult<TemuShopRespVO>> getShopPage(@Valid TemuShopPageReqVO pageVO) {
        return success(temuShopService.getShopPage(pageVO));
    }
} 