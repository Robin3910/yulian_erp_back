package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn.OrderReturnPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn.OrderReturnPageRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn.OrderReturnUpdateReasonReqVO;
import cn.iocoder.yudao.module.temu.service.orderReturn.OrderReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/temu/order-return")
@Tag(name = "Temu管理 - 返单管理")
public class OrderReturnController {
    @Resource
    private OrderReturnService orderReturnService;

    @GetMapping("/page")
    @Operation(summary = "分页查询返单数据")
    public CommonResult<PageResult<OrderReturnPageRespVO>> page(OrderReturnPageReqVO reqVO) {
        return success(orderReturnService.getPage(reqVO));
    }

    @PutMapping("/update-reason")
    @Operation(summary = "修改返工原因")
    public CommonResult<Boolean> updateReturnReason(@Valid @RequestBody OrderReturnUpdateReasonReqVO reqVO) {
        return success(orderReturnService.updateReturnReason(reqVO));
    }

 
} 