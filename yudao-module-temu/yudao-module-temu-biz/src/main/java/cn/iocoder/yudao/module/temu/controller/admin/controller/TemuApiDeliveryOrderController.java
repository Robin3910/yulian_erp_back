package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderSimpleVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuBoxMarkQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuBoxMarkRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.goods.TemuCustomGoodsLabelQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.goods.TemuCustomGoodsLabelRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.print.TemuPrintDataKeyRespVO;
import cn.iocoder.yudao.module.temu.service.deliveryOrder.TemuDeliveryOrderConvertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "TemuApi - 发货管理 ")
@RestController
@RequestMapping("/temuApi/delivery")
@Validated
public class TemuApiDeliveryOrderController {

    @Resource
    private TemuDeliveryOrderConvertService convertService;

    @PostMapping("/page")
    @Operation(summary = "查询Temu平台物流信息")
    public CommonResult<PageResult<TemuDeliveryOrderSimpleVO>> queryTemuLogistics(@RequestBody TemuDeliveryOrderQueryReqVO reqVO) {
        try {
            PageResult<TemuDeliveryOrderSimpleVO> pageResult = convertService.queryTemuLogisticsPage(reqVO);
            return CommonResult.success(pageResult);
        } catch (Exception e) {
            return CommonResult.error(500, "物流查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/boxmark")
    @Operation(summary = "查询物流面单信息")
    public CommonResult<List<TemuBoxMarkRespVO>> queryBoxMark(@RequestBody TemuBoxMarkQueryReqVO reqVO) {
        try {
            List<TemuBoxMarkRespVO> result = convertService.queryBoxMark(reqVO);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.error(500, "查询物流面单信息失败: " + e.getMessage());
        }
    }

    @PostMapping("/custom-label")
    @Operation(summary = "查询定制sku条码信息")
    public CommonResult<TemuCustomGoodsLabelRespVO> queryCustomGoodsLabel(@RequestBody TemuCustomGoodsLabelQueryReqVO reqVO) {
        try {
            TemuCustomGoodsLabelRespVO result = convertService.queryCustomGoodsLabel(reqVO);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.error(500, "定制sku条码查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/boxmark/print")
    @Operation(summary = "获取物流面单打印数据")
    public CommonResult<TemuPrintDataKeyRespVO> getBoxMarkPrintData(@RequestBody TemuBoxMarkQueryReqVO reqVO) {
        try {
            TemuPrintDataKeyRespVO result = convertService.getBoxMarkPrintDataKey(reqVO);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.error(500, "获取物流面单打印数据: " + e.getMessage());
        }
    }

    @PostMapping("/custom-label/print")
    @Operation(summary = "获取定制sku条码打印数据")
    public CommonResult<TemuPrintDataKeyRespVO> getCustomGoodsLabelPrintData(@RequestBody TemuCustomGoodsLabelQueryReqVO reqVO) {
        try {
            TemuPrintDataKeyRespVO result = convertService.getCustomGoodsLabelPrintDataKey(reqVO);
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.error(500, "获取定制sku条码打印数据失败: " + e.getMessage());
        }
    }

}
