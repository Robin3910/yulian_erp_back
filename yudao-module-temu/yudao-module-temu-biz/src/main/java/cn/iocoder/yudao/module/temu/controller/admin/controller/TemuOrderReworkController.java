package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.rework.TemuOrderReworkCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.rework.TemuOrderReworkRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.rework.TemuOrderReworkFinishReqVO;
import cn.iocoder.yudao.module.temu.service.rework.TemuOrderReworkService;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderReworkDO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import java.util.stream.Collectors;
import cn.iocoder.yudao.module.temu.controller.admin.vo.rework.TemuOrderReworkPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.rework.TemuOrderReworkFinishRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.rework.TemuOrderReworkFromShippingReqVO;

@Tag(name = "管理后台 - 订单返工")
@RestController
@RequestMapping("/temu/rework")
@Validated
public class TemuOrderReworkController {

    @Resource
    private TemuOrderReworkService temuOrderReworkService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建订单返工记录")
    @PreAuthorize("@ss.hasPermission('temu:rework:create')")
    public CommonResult<Long> createRework(@Valid @RequestBody TemuOrderReworkCreateReqVO createReqVO) {
        return success(temuOrderReworkService.createRework(createReqVO));
    }

    @PostMapping("/create-from-shipping")
    @Operation(summary = "从发货管理页面创建订单返工记录")
    @PreAuthorize("@ss.hasPermission('temu:rework:create')")
    public CommonResult<Long> createReworkFromShipping(@Valid @RequestBody TemuOrderReworkFromShippingReqVO createReqVO) {
        // 获取当前登录用户信息
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        String reworkInitiatorName = null;
        
        if (currentUserId != null) {
            AdminUserRespDTO currentUser = adminUserApi.getUser(currentUserId);
            if (currentUser != null) {
                reworkInitiatorName = currentUser.getNickname();
            }
        }
        
        // 转换为标准的创建请求VO
        TemuOrderReworkCreateReqVO standardReqVO = new TemuOrderReworkCreateReqVO();
        standardReqVO.setOrderNo(createReqVO.getOrderNo());
        standardReqVO.setCustomSku(createReqVO.getCustomSku());
        standardReqVO.setReworkReason(createReqVO.getReworkReason());
        
        // 创建返工记录
        Long reworkId = temuOrderReworkService.createRework(standardReqVO);
        
        // 如果有返工发起人信息，更新返工记录
        if (reworkInitiatorName != null) {
            temuOrderReworkService.updateReworkInitiator(reworkId, reworkInitiatorName);
        }
        
        // 获取返工记录，确保作图人信息已正确设置
        TemuOrderReworkDO rework = temuOrderReworkService.getReworkById(reworkId);
        if (rework != null && rework.getLastDrawUserName() != null) {
            // 确保返工作图人信息与上一次作图人信息保持一致
            temuOrderReworkService.updateReworkDrawUser(reworkId, rework.getLastDrawUserName(), rework.getLastDrawUserId());
        }
        
        return success(reworkId);
    }

    @GetMapping("/get")
    @Operation(summary = "根据定制SKU获取返工记录")
    @Parameter(name = "customSku", description = "定制SKU", required = true)
    @PreAuthorize("@ss.hasPermission('temu:rework:query')")
    public CommonResult<TemuOrderReworkRespVO> getReworkByCustomSku(@RequestParam("customSku") String customSku) {
        return success(convertToRespVO(temuOrderReworkService.getReworkByCustomSku(customSku)));
    }

    @GetMapping("/page")
    @Operation(summary = "返工订单分页列表")
    @PreAuthorize("@ss.hasPermission('temu:rework:query')")
    public CommonResult<PageResult<TemuOrderReworkRespVO>> getReworkPage(@ModelAttribute TemuOrderReworkPageReqVO reqVO) {
        PageResult<TemuOrderReworkDO> page = temuOrderReworkService.getReworkPage(reqVO);
        // 转换为RespVO
        PageResult<TemuOrderReworkRespVO> voPage = new PageResult<>(
            page.getList().stream().map(this::convertToRespVO).collect(Collectors.toList()),
            page.getTotal()
        );
        return success(voPage);
    }

    @PostMapping("/finish")
    @Operation(summary = "完成返工")
    @PreAuthorize("@ss.hasPermission('temu:rework:finish')")
    public CommonResult<TemuOrderReworkFinishRespVO> finishRework(@RequestBody @Valid TemuOrderReworkFinishReqVO reqVO) {
        TemuOrderReworkDO rework = temuOrderReworkService.finishReworkAndReturn(reqVO.getCustomSku());
        TemuOrderReworkFinishRespVO respVO = new TemuOrderReworkFinishRespVO();
        respVO.setCustomSku(rework.getCustomSku());
        respVO.setIsFinished(rework.getIsFinished());
        return success(respVO);
    }

    private TemuOrderReworkRespVO convertToRespVO(cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderReworkDO rework) {
        if (rework == null) {
            return null;
        }
        TemuOrderReworkRespVO respVO = new TemuOrderReworkRespVO();
        respVO.setId(rework.getId());
        respVO.setOrderNo(rework.getOrderNo());
        respVO.setProductTitle(rework.getProductTitle());
        respVO.setProductImgUrl(rework.getProductImgUrl());
        respVO.setProductProperties(rework.getProductProperties());
        respVO.setSku(rework.getSku());
        respVO.setSkc(rework.getSkc());
        respVO.setCustomSku(rework.getCustomSku());
        respVO.setReworkReason(rework.getReworkReason());
        respVO.setReworkInitiatorName(rework.getReworkInitiatorName());
        respVO.setReworkDrawUserName(rework.getReworkDrawUserName());
        respVO.setReworkDrawUserId(rework.getReworkDrawUserId());
        respVO.setLastDrawUserName(rework.getLastDrawUserName());
        respVO.setLastDrawUserId(rework.getLastDrawUserId());
        respVO.setIsFinished(rework.getIsFinished());
        respVO.setReworkCount(rework.getReworkCount());
        respVO.setCustomImageUrls(rework.getCustomImageUrls());
        respVO.setCustomTextList(rework.getCustomTextList());
        respVO.setShopId(rework.getShopId());
        respVO.setCreateTime(rework.getCreateTime());
        return respVO;
    }

} 