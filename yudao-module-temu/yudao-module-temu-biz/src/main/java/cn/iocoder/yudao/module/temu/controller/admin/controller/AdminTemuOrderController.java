package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.PermitAll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "超级测试 - Test")
@RestController
@RequestMapping("/temu/order")
@Validated
@PermitAll
public class AdminTemuOrderController {
	
	@GetMapping("/get")
	@Operation(summary = "获取 test 信息")
	public CommonResult<String> get() {
		return success("true");
	}
	
}