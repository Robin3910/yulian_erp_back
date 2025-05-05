package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopBatchSaveSkcReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopOldTypeReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopOldTypeSkcDO;
import cn.iocoder.yudao.module.temu.service.shop.TemuShopOldTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - Temu店铺合规单skc")
@RestController
@RequestMapping("/temu/shop-oldType")
@Validated
@PermitAll
public class AdminTemuShopOldTypeController {

    @Resource
    private TemuShopOldTypeService temuShopOldTypeService;

    @PostMapping("/save")
    @Operation(summary = "批量保存合规单SKC")
    public CommonResult<Integer> batchSaveOldTypeSkc(@RequestBody List<TemuShopBatchSaveSkcReqVO> saveSkcReqVOList) {
        return success(temuShopOldTypeService.batchSaveOldTypeSkc(saveSkcReqVOList));
    }

    @GetMapping("/get")
    @Operation(summary = "获取合规单信息")
    public CommonResult<List<TemuShopOldTypeSkcDO>> getOldTypeInfo(@Validated TemuShopOldTypeReqVO reqVO) {
        return success(temuShopOldTypeService.getOldTypeInfo(reqVO));
    }
}
