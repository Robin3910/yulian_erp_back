package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogSaveReqVO;
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

}