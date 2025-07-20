package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingCountReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingCountRespVO;
import cn.iocoder.yudao.module.temu.service.order.ITemuOrderShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderBatchMapper.log;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderUrgentAlertReqVO;

@Tag(name = "Temu管理 - 待发货列表管理")
@RestController
@RequestMapping("/temu/order-shipping")
@Validated
@RequiredArgsConstructor
public class AdminTemuOrderShippingController {

    private final ITemuOrderShippingService shippingService;

    private final ConfigApi configApi;

    @GetMapping("/user-page")
    @Operation(summary = "分页查询用户店铺待发货订单")
    public CommonResult<PageResult<TemuOrderShippingRespVO>> getOrderShippingPageByUser(
            TemuOrderShippingPageReqVO pageVO) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return success(shippingService.getOrderShippingPageByUser(pageVO, userId));
    }

    @PostMapping("/send-urgent-alert")
    @Operation(summary = "发送紧急物流告警")
    public CommonResult<Boolean> sendUrgentAlert(@Valid @RequestBody List<TemuOrderUrgentAlertReqVO> reqVOs) {
        // 遍历处理每个告警请求
        for (TemuOrderUrgentAlertReqVO reqVO : reqVOs) {
            if (!shippingService.sendUrgentAlert(reqVO)) {
                return success(false);
            }
        }
        return success(true);
    }

    @PostMapping("/batch-save")
    @Operation(summary = "批量保存待发货订单")
    public CommonResult<Integer> batchSaveOrderShipping(
            @Valid @RequestBody List<TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO> saveRequestVOs) {
        if (saveRequestVOs == null || saveRequestVOs.isEmpty()) {
            return success(0);
        }
        // 从配置中获取是否开启序号保存
        String isDailySequence = configApi.getConfigValueByKey("temu.is_daily_sequence");
        log.info("批量保存待发货订单是否使用测试方法的配置值: {}", isDailySequence);
        boolean flag = false; // 默认值
        if (StrUtil.isNotEmpty(isDailySequence)) {
            try {
                flag = Boolean.parseBoolean(isDailySequence);
            } catch (Exception e) {
                log.warn("批量保存待发货订单是否使用测试方法的配置格式错误，使用默认值");
            }
        }
        if(flag){
            return  success(shippingService.batchSaveOrderShippingTest(saveRequestVOs));
        }else{
            return success(shippingService.batchSaveOrderShipping(saveRequestVOs));
        }
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询待发货订单")
    public CommonResult<PageResult<TemuOrderShippingRespVO>> getOrderShippingPage(TemuOrderShippingPageReqVO pageVO) {

        return success(shippingService.getOrderShippingPage(pageVO));
    }

    @PutMapping("/batch-update-status")
    @Operation(summary = "批量修改订单状态")
    public CommonResult<Boolean> batchUpdateOrderStatus(
            @Valid @RequestBody TemuOrderShippingPageReqVO.BatchUpdateStatusReqVO reqVO) {
        return success(shippingService.batchUpdateOrderStatus(reqVO.getOrderIds(), reqVO.getOrderStatus(),
                reqVO.getTrackingNumber()));
    }

    @GetMapping("/count")
    @Operation(summary = "查询加急未发货订单总数")
    public CommonResult<TemuOrderShippingCountRespVO> getUrgentOrderCount(
            @Valid TemuOrderShippingCountReqVO reqVO) {
        return success(shippingService.getUrgentOrderCount(reqVO));
    }

}
