package cn.iocoder.yudao.module.temu.controller.admin.controller;


import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingRespVO;
import cn.iocoder.yudao.module.temu.service.order.ITemuOrderShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Temu管理 - 待发货列表管理")
@RestController
@RequestMapping("/temu/order-shipping")
@Validated
@RequiredArgsConstructor
public class AdminTemuOrderShippingController {

    private final ITemuOrderShippingService shippingService;

    @PostMapping("/save")
    @Operation(summary = "保存待发货订单")
    public CommonResult<Long> saveOrderShipping(@Valid @RequestBody TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO) {
        return success(shippingService.saveOrderShipping(saveRequestVO));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询待发货订单")
    public CommonResult<PageResult<TemuOrderShippingRespVO>> getOrderShippingPage(TemuOrderShippingPageReqVO pageVO) {

        return success(shippingService.getOrderShippingPage(pageVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改订单状态")
    public CommonResult<Boolean> updateOrderStatus(@Valid @RequestBody TemuOrderShippingPageReqVO reqVO) {
        return success(shippingService.updateOrderStatus(reqVO));
    }

}
