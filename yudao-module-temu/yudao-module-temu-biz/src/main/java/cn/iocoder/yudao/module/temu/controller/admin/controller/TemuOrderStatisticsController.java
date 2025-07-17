package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.module.temu.service.orderStatistics.IOrderStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderStatistics.OrderStatisticsReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderStatistics.OrderStatisticsRespVO;

import javax.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Temu管理 - 订单统计")
@RestController
@RequestMapping("/api/order/statistics")
@Slf4j
public class TemuOrderStatisticsController {
    @Autowired
    private IOrderStatisticsService orderStatisticsService;

    @PostMapping("")
    @Operation(summary = "获取订单统计数据")
    public CommonResult<OrderStatisticsRespVO> getOrderStatistics(@Valid @RequestBody OrderStatisticsReqVO reqVO) {
        return CommonResult.success(orderStatisticsService.getOrderStatistics(reqVO));
    }
}
