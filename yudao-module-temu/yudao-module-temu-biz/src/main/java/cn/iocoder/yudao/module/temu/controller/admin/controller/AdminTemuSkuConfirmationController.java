package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.skuconfirmation.TemuSkuConfirmationReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.skuconfirmation.TemuSkuConfirmationRespVO;
import cn.iocoder.yudao.module.temu.service.skuconfirmation.ITemuSkuConfirmationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SKU确认管理")
@RestController
@RequestMapping("/temu/sku-confirmation")
@Validated
public class AdminTemuSkuConfirmationController {

    @Resource
    private ITemuSkuConfirmationService temuSkuConfirmationService;

    @PostMapping("/confirm")
    @Operation(summary = "确认SKU")
    public CommonResult<Boolean> confirmSku(@Valid @RequestBody TemuSkuConfirmationReqVO reqVO) {
        return success(temuSkuConfirmationService.confirmSkuByOrderId(reqVO.getOrderId()));
    }

    @DeleteMapping("/cancel/{id}")
    @Operation(summary = "取消确认SKU")
    public CommonResult<Boolean> cancelConfirmation(@PathVariable("id") Long id) {
        return success(temuSkuConfirmationService.cancelConfirmation(id));
    }

    @GetMapping("/list")
    @Operation(summary = "获取已确认的SKU列表")
    public CommonResult<List<TemuSkuConfirmationRespVO>> getConfirmedSkuList(@RequestParam("shopId") String shopId) {
        return success(temuSkuConfirmationService.getConfirmedSkuList(shopId));
    }

    @GetMapping("/check")
    @Operation(summary = "检查SKU是否已确认")
    public CommonResult<Boolean> isSkuConfirmed(@RequestParam("shopId") String shopId,
            @RequestParam("sku") String sku) {
        return success(temuSkuConfirmationService.isSkuConfirmed(shopId, sku));
    }
}