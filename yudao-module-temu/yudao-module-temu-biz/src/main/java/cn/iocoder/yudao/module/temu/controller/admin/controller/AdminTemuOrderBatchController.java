package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchCreateVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchPageVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchUpdateFileVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchUpdateStatusVO;
import cn.iocoder.yudao.module.temu.service.orderBatch.ITemuOrderBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Temu管理 - 批次管理")
@RestController
@RequestMapping("/temu/order-batch")
@Validated

public class AdminTemuOrderBatchController {
	@Resource
	private ITemuOrderBatchService temuOrderBatchService;
	
	/**
	 * 创建批次
	 *
	 * @param temuOrderBatchCreateVO 创建批次的请求参数
	 * @return CommonResult<?> 操作结果，成功时包含创建的批次信息
	 */
	@PostMapping("/create")
	@Operation(summary = "创建批次")
	
	
	public CommonResult<?> create(@Valid @RequestBody TemuOrderBatchCreateVO temuOrderBatchCreateVO) {
		return CommonResult.success(temuOrderBatchService.createBatch(temuOrderBatchCreateVO));
	}
	
	/**
	 * 分页查询批次
	 * 该方法用于根据请求参数分页查询批次信息。
	 *
	 * @param temuOrderBatchPageVO 分页查询的请求参数对象，包含分页信息和查询条件
	 * @return CommonResult<?> 返回操作结果，成功时包含查询到的批次信息
	 */
	@GetMapping("/page")
	@Operation(summary = "分页查询批次")
	@PermitAll
	public CommonResult<?> page(@Valid TemuOrderBatchPageVO temuOrderBatchPageVO) {
		return CommonResult.success(temuOrderBatchService.list(temuOrderBatchPageVO));
	}
	
	/**
	 * 更新批次文件信息接口
	 * 该接口用于更新特定批次的相关文件信息
	 *
	 * @param temuOrderBatchUpdateFileVO 更新批次文件信息的请求对象，包含需要更新的文件信息
	 * @return 返回更新操作的结果，封装在CommonResult对象中
	 */
	@PutMapping("/update-file")
	@Operation(summary = "更新批次")
	public CommonResult<?> updateFile(@Valid @RequestBody TemuOrderBatchUpdateFileVO temuOrderBatchUpdateFileVO) {
	    return CommonResult.success(temuOrderBatchService.updateBatchFile(temuOrderBatchUpdateFileVO));
	}
	
	/**
	 * 更新订单批次状态的接口方法
	 *
	 * 该方法通过POST请求接收客户端发送的订单批次状态更新信息，
	 * 并调用服务层方法进行状态更新，返回更新结果
	 *
	 * @param temuOrderBatchUpdateStatusVO 包含订单批次状态更新信息的请求体对象
	 * @return 返回一个CommonResult对象，包含更新操作的结果信息
	 */
	@PutMapping("/update-status")
	@Operation(summary = "更新订单批次状态")
	public CommonResult<?> updateStatus(@Valid @RequestBody TemuOrderBatchUpdateStatusVO temuOrderBatchUpdateStatusVO) {
		return CommonResult.success(temuOrderBatchService.updateStatus(temuOrderBatchUpdateStatusVO));
	}
	
}
