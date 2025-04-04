package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.temu.controller.admin.controller.vo.client.TemuOrderRequestVO;
import cn.iocoder.yudao.module.temu.controller.admin.service.order.ITemuOrderService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Temu管理 - 订单管理")
@RestController
@RequestMapping("/temu/order")
@Validated

public class AdminTemuOrderController {
	@Resource private ITemuOrderService temuOrderService;
	@GetMapping("/page")
	@Operation(summary = "获取订单管理信息")
	@PermitAll
	public CommonResult<?> list(@RequestBody TemuOrderRequestVO temuOrderRequestVO)  {
	 return  CommonResult.success(temuOrderService.list(temuOrderRequestVO));
	}
	
}