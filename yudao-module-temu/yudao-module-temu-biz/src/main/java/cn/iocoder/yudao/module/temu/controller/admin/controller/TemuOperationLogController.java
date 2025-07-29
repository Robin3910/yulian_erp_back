package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogSaveReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.UserRechargeRecordPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.UserRechargeRecordRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.OrderPlacementRecordPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.OrderPlacementRecordRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.OrderPlacementAmountStatisticsRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOperationLogDO;
import cn.iocoder.yudao.module.temu.service.operationlog.TemuOperationLogService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;



@Tag(name = "管理后台 - 操作日志")
@RestController
@RequestMapping("/temu/operation-log")
@Validated
public class TemuOperationLogController {

    @Resource
    private TemuOperationLogService operationLogService;

    @PostMapping("/create")
    @Operation(summary = "创建操作日志")
    @PreAuthorize("@ss.hasPermission('temu:operation-log:create')")
    public CommonResult<Long> createOperationLog(@Valid @RequestBody TemuOperationLogSaveReqVO createReqVO) {
        return success(operationLogService.createOperationLog(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新操作日志")
    @PreAuthorize("@ss.hasPermission('temu:operation-log:update')")
    public CommonResult<Boolean> updateOperationLog(@Valid @RequestBody TemuOperationLogSaveReqVO updateReqVO) {
        operationLogService.updateOperationLog(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除操作日志")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('temu:operation-log:delete')")
    public CommonResult<Boolean> deleteOperationLog(@RequestParam("id") Long id) {
        operationLogService.deleteOperationLog(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得操作日志")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('temu:operation-log:query')")
    public CommonResult<TemuOperationLogRespVO> getOperationLog(@RequestParam("id") Long id) {
        TemuOperationLogDO operationLog = operationLogService.getOperationLog(id);
        return success(BeanUtils.toBean(operationLog, TemuOperationLogRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得操作日志分页")
    @PreAuthorize("@ss.hasPermission('temu:operation-log:query')")
    public CommonResult<PageResult<TemuOperationLogRespVO>> getOperationLogPage(@Valid TemuOperationLogPageReqVO pageReqVO) {
        PageResult<TemuOperationLogDO> pageResult = operationLogService.getOperationLogPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TemuOperationLogRespVO.class));
    }

    @GetMapping("/bill/user-recharge")
    @Operation(summary = "获得用户充值记录分页")
    @PreAuthorize("@ss.hasPermission('temu:operation-log:query')")
    public CommonResult<PageResult<UserRechargeRecordRespVO>> getUserRechargeRecordPage(@Valid UserRechargeRecordPageReqVO pageReqVO) {
        PageResult<UserRechargeRecordRespVO> pageResult = operationLogService.getUserRechargeRecordPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/bill/user-recharge/export-excel")
    @Operation(summary = "导出用户充值记录 Excel")
    @PreAuthorize("@ss.hasPermission('temu:operation-log:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportUserRechargeRecordExcel(@Valid UserRechargeRecordPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<UserRechargeRecordRespVO> list = operationLogService.getUserRechargeRecordList(pageReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "用户充值记录.xls", "数据", UserRechargeRecordRespVO.class, list);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出操作日志 Excel")
    @PreAuthorize("@ss.hasPermission('temu:operation-log:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOperationLogExcel(@Valid TemuOperationLogPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TemuOperationLogDO> list = operationLogService.getOperationLogPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "操作日志.xls", "数据", TemuOperationLogRespVO.class,
                        BeanUtils.toBean(list, TemuOperationLogRespVO.class));
    }

    @GetMapping("/bill/order-placement")
    @Operation(summary = "获得批量下单记录分页")
    @PreAuthorize("@ss.hasPermission('temu:operation-log:query')")
    public CommonResult<PageResult<OrderPlacementRecordRespVO>> getOrderPlacementRecordPage(@Valid OrderPlacementRecordPageReqVO pageReqVO) {
        PageResult<OrderPlacementRecordRespVO> pageResult = operationLogService.getOrderPlacementRecordPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/bill/order-placement/amount-statistics")
    @Operation(summary = "获得批量下单记录总金额统计")
    @PreAuthorize("@ss.hasPermission('temu:operation-log:query')")
    public CommonResult<OrderPlacementAmountStatisticsRespVO> getOrderPlacementAmountStatistics(@Valid OrderPlacementRecordPageReqVO pageReqVO) {
        OrderPlacementAmountStatisticsRespVO statistics = operationLogService.getOrderPlacementAmountStatistics(pageReqVO);
        return success(statistics);
    }

    @GetMapping("/bill/order-placement/export-excel")
    @Operation(summary = "导出批量下单记录 Excel")
    @PreAuthorize("@ss.hasPermission('temu:operation-log:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOrderPlacementRecordExcel(@Valid OrderPlacementRecordPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<OrderPlacementRecordRespVO> list = operationLogService.getOrderPlacementRecordList(pageReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "批量下单记录.xls", "数据", OrderPlacementRecordRespVO.class, list);
    }

    // 大客户的账单页面。
    @GetMapping("/bill/order-VipPlacement")
    @Operation(summary = "获得Vip批量下单记录分页")
    @PreAuthorize("@ss.hasPermission('temu:operation-log:query')")
    public CommonResult<PageResult<OrderPlacementRecordRespVO>> getOrderVipPlacementRecordPage(
            @Valid OrderPlacementRecordPageReqVO pageReqVO) {
        PageResult<OrderPlacementRecordRespVO> pageResult = operationLogService
                .getOrderVipPlacementRecordPage(pageReqVO);
        return success(pageResult);
    }

    @GetMapping("/bill/order-VipPlacement/amount-statistics")
    @Operation(summary = "获得Vip批量下单记录总金额统计")
    @PreAuthorize("@ss.hasPermission('temu:operation-log:query')")
    public CommonResult<OrderPlacementAmountStatisticsRespVO> getOrderVipPlacementAmountStatistics(
            @Valid OrderPlacementRecordPageReqVO pageReqVO) {
        OrderPlacementAmountStatisticsRespVO statistics = operationLogService
                .getOrderVipPlacementAmountStatistics(pageReqVO);
        return success(statistics);
    }

    @GetMapping("/bill/order-VipPlacement/export-excel")
    @Operation(summary = "导出批量下单记录 Excel")
    @PreAuthorize("@ss.hasPermission('temu:operation-log:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOrderVipPlacementRecordExcel(@Valid OrderPlacementRecordPageReqVO pageReqVO,
                                                   HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<OrderPlacementRecordRespVO> list = operationLogService.getOrderVipPlacementRecordList(pageReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "批量下单记录.xls", "数据", OrderPlacementRecordRespVO.class, list);
    }

}