package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.module.temu.controller.admin.vo.workertask.TemuWorkerTaskPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.workertask.TemuWorkerTaskRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.workertask.TemuWorkerTaskSaveReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuWorkerTaskDO;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.constraints.*;
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

import cn.iocoder.yudao.module.temu.service.workertask.TemuWorkerTaskService;

@Tag(name = "管理后台 - 工作人员任务记录")
@RestController
@RequestMapping("/temu/worker-task")
@Validated
public class TemuWorkerTaskController {

    @Resource
    private TemuWorkerTaskService workerTaskService;

    @PostMapping("/create")
    @Operation(summary = "创建工作人员任务记录")
    @PreAuthorize("@ss.hasPermission('temu:worker-task:create')")
    public CommonResult<Long> createWorkerTask(@Valid @RequestBody TemuWorkerTaskSaveReqVO createReqVO) {
        return success(workerTaskService.createWorkerTask(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工作人员任务记录")
    @PreAuthorize("@ss.hasPermission('temu:worker-task:update')")
    public CommonResult<Boolean> updateWorkerTask(@Valid @RequestBody TemuWorkerTaskSaveReqVO updateReqVO) {
        workerTaskService.updateWorkerTask(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工作人员任务记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('temu:worker-task:delete')")
    public CommonResult<Boolean> deleteWorkerTask(@RequestParam("id") Long id) {
        workerTaskService.deleteWorkerTask(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工作人员任务记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('temu:worker-task:query')")
    public CommonResult<TemuWorkerTaskRespVO> getWorkerTask(@RequestParam("id") Long id) {
        TemuWorkerTaskDO workerTask = workerTaskService.getWorkerTask(id);
        return success(BeanUtils.toBean(workerTask, TemuWorkerTaskRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得工作人员任务记录分页")
    @PreAuthorize("@ss.hasPermission('temu:worker-task:query')")
    public CommonResult<PageResult<TemuWorkerTaskRespVO>> getWorkerTaskPage(@Valid TemuWorkerTaskPageReqVO pageReqVO) {
        return success(workerTaskService.getWorkerTaskPage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出工作人员任务记录 Excel")
    @PreAuthorize("@ss.hasPermission('temu:worker-task:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWorkerTaskExcel(@Valid TemuWorkerTaskPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TemuWorkerTaskRespVO> list = workerTaskService.getWorkerTaskPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "工作人员任务记录.xls", "数据", TemuWorkerTaskRespVO.class, list);
    }

}