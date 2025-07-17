package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderSimpleVO;
import cn.iocoder.yudao.module.temu.service.deliveryOrder.TemuDeliveryOrderConvertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@Tag(name = "TemuApi - 发货管理 ")
@RestController
@RequestMapping("/temuApi/delivery")
@Validated
public class TemuApiDeliveryOrderController {

    @Resource
    private TemuDeliveryOrderConvertService convertService;

    @RequestMapping("/page")
    @Operation(summary = "查询Temu平台物流信息")
    public CommonResult<PageResult<TemuDeliveryOrderSimpleVO>> queryTemuLogistics(@RequestBody TemuDeliveryOrderQueryReqVO reqVO) {
        try {
            PageResult<TemuDeliveryOrderSimpleVO> pageResult = convertService.queryTemuLogisticsPage(reqVO);
            return CommonResult.success(pageResult);
        } catch (Exception e) {
            return CommonResult.error(500, "物流查询失败: " + e.getMessage());
        }
    }
}
