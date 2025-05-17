package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.*;
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
	public CommonResult<?> page(@Valid TemuOrderBatchPageVO temuOrderBatchPageVO) {
		return CommonResult.success(temuOrderBatchService.list(temuOrderBatchPageVO));
	}
	
	@GetMapping("/task-page")
	@Operation(summary = "分页查询批次任务")
	public CommonResult<?> task(@Valid TemuOrderBatchPageVO temuOrderBatchPageVO) {
		//获取当前登陆的用户
		Long userId = SecurityFrameworkUtils.getLoginUserId();
		temuOrderBatchPageVO.setUserId(userId);
		return CommonResult.success(temuOrderBatchService.taskList(temuOrderBatchPageVO));
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
	
	@PutMapping("/update-file-by-task")
	@Operation(summary = "更新批次")
	public CommonResult<?> updateFileByTask(@Valid @RequestBody TemuOrderBatchUpdateFileByTaskVO temuOrderBatchUpdateFileVO) {
		return CommonResult.success(temuOrderBatchService.updateBatchFileByTask(temuOrderBatchUpdateFileVO));
	}
	
	/**
	 * 更新订单批次状态的接口方法
	 * <p>
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
	
	@PutMapping("/update-status-by-task")
	@Operation(summary = "确认完成批次任务")
	public CommonResult<?> updateStatusByTask(@Valid @RequestBody TemuOrderBatchUpdateStatusByTaskVO temuOrderBatchUpdateStatusVO) {
		return CommonResult.success(temuOrderBatchService.updateStatusByTask(temuOrderBatchUpdateStatusVO));
	}
	
	/**
	 * 提交批次订单备注的接口方法
	 * <p>
	 * 该方法通过POST请求接收客户端发送的订单批次备注信息，
	 * 并调用服务层方法进行备注提交，返回提交结果
	 *
	 * @param requestVO 订单批次备注信息对象
	 * @return 提交结果，封装在CommonResult对象中
	 */
	@PutMapping("/update-order-remark")
	@Operation(summary = "提交批次订单备注")
	public CommonResult<?> saveOrderRemark(@Valid @RequestBody TemuOrderBatchSaveOrderRemarkReqVO requestVO) {
		return CommonResult.success(temuOrderBatchService.saveOrderRemark(requestVO));
	}
	
	//批量分配批次订单任务
	@PostMapping("/dispatch-task")
	@Operation(summary = "批量分配批次订单任务")
	public CommonResult<?> dispatchTask(@Valid @RequestBody TemuOrderBatchDispatchTaskVO requestVO) {
		return CommonResult.success(temuOrderBatchService.dispatchTask(requestVO));
	}
//
	//完成单个批次任务中的单个订单任务
	@PostMapping("/complete-batch-order-task")
	@Operation(summary = "完成单个批次任务中的单个订单任务")
	public CommonResult<?> completeBatchOrderTask(@Valid @RequestBody TemuOrderBatchCompleteOrderTaskVO requestVO) {
		return CommonResult.success(temuOrderBatchService.completeBatchOrderTask(requestVO));
	}
}
