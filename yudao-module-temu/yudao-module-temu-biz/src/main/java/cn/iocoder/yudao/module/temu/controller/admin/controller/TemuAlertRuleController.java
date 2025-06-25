package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRuleCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRulePageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRuleRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRuleUpdateReqVO;
import cn.iocoder.yudao.module.temu.utils.alertrule.TemuAlertRuleConvert;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuAlertRuleDO;
import cn.iocoder.yudao.module.temu.service.alertRule.TemuAlertRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 告警规则")
@RestController
@RequestMapping("/temu/alert-rule")
@Validated
public class TemuAlertRuleController {

    @Resource
    private TemuAlertRuleService alertRuleService;

    @PostMapping("/create")
    @Operation(summary = "创建告警规则")
    public CommonResult<Long> createAlertRule(@Valid @RequestBody TemuAlertRuleCreateReqVO createReqVO) {
        return success(alertRuleService.createAlertRule(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新告警规则")
    public CommonResult<Boolean> updateAlertRule(@Valid @RequestBody TemuAlertRuleUpdateReqVO updateReqVO) {
        alertRuleService.updateAlertRule(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除告警规则")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<Boolean> deleteAlertRule(@RequestParam("id") Long id) {
        alertRuleService.deleteAlertRule(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得告警规则")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<TemuAlertRuleRespVO> getAlertRule(@RequestParam("id") Long id) {
        TemuAlertRuleDO alertRule = alertRuleService.getAlertRule(id);
        return success(TemuAlertRuleConvert.INSTANCE.convert(alertRule));
    }

    @GetMapping("/page")
    @Operation(summary = "获得告警规则分页")
    public CommonResult<PageResult<TemuAlertRuleRespVO>> getAlertRulePage(@Valid TemuAlertRulePageReqVO pageVO) {
        PageResult<TemuAlertRuleDO> pageResult = alertRuleService.getAlertRulePage(pageVO);
        return success(TemuAlertRuleConvert.INSTANCE.convertPage(pageResult));
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改告警规则状态")
    public CommonResult<Boolean> updateAlertRuleStatus(@RequestParam("id") Long id,
            @RequestParam("status") Integer status) {
        alertRuleService.updateAlertRuleStatus(id, status);
        return success(true);
    }

    @PostMapping("/test")
    @Operation(summary = "测试执行告警规则")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<Integer> testAlertRule(@RequestParam("id") Long id) {
        return success(alertRuleService.testAlertRule(id));
    }
}
