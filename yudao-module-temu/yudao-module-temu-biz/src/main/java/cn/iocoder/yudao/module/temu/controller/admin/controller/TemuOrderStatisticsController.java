package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.module.temu.service.orderStatistics.IOrderStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderStatistics.OrderStatisticsReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderStatistics.OrderStatisticsRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderStatistics.CategoryRespVO;
import cn.iocoder.yudao.module.temu.dal.mysql.OrderStatisticsMapper;

import javax.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;

@Tag(name = "Temu管理 - 订单统计")
@RestController
@RequestMapping("/api/order/statistics")
@Slf4j
public class TemuOrderStatisticsController {
    @Autowired
    private IOrderStatisticsService orderStatisticsService;

    @Autowired
    private OrderStatisticsMapper orderStatisticsMapper;

    @PostMapping("")
    @Operation(summary = "获取订单统计数据")
    public CommonResult<OrderStatisticsRespVO> getOrderStatistics(@Valid @RequestBody OrderStatisticsReqVO reqVO) {
        return CommonResult.success(orderStatisticsService.getOrderStatistics(reqVO));
    }

    @PostMapping("/return")
    @Operation(summary = "获取返单统计数据")
    public CommonResult<OrderStatisticsRespVO> getReturnOrderStatistics(
            @Valid @RequestBody OrderStatisticsReqVO reqVO) {
        return CommonResult.success(orderStatisticsService.getReturnOrderStatistics(reqVO));
    }

    @GetMapping("/categories")
    @Operation(summary = "获取所有可选的类目列表")
    public CommonResult<List<CategoryRespVO>> getCategories() {
        List<Map<String, Object>> categoryMaps = orderStatisticsMapper.selectAllCategories();
        List<CategoryRespVO> categories = new java.util.ArrayList<>();
        for (Map<String, Object> map : categoryMaps) {
            CategoryRespVO category = new CategoryRespVO();
            category.setCategoryId((String) map.get("categoryId"));
            category.setCategoryName((String) map.get("categoryName"));
            categories.add(category);
        }
        return CommonResult.success(categories);
    }
}
