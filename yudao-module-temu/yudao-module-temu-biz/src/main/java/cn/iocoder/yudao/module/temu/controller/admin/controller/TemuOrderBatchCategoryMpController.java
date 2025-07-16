package cn.iocoder.yudao.module.temu.controller.admin.controller;


import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.batchCategoryMp.TemuOrderBatchCategoryRespVO;
import cn.iocoder.yudao.module.temu.service.batchCategoryMp.TemuOrderBatchCategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Temu管理 - 类目批次管理")
@RestController
@RequestMapping("/erp/temu-order-batch-category")
@Validated
@Slf4j
public class TemuOrderBatchCategoryMpController {

    @Autowired
    private TemuOrderBatchCategoryService service;

    // 1. 批量删除
    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam("ids") List<Long> ids) {
        boolean result = service.deleteBatch(ids);
        return success(result);
    }

    // 2. 按 batchCategoryId 查询
    @GetMapping("/list")
    public CommonResult<List<TemuOrderBatchCategoryRespVO>> listByBatchCategoryId(@RequestParam(value = "batchCategoryId", required = false) String batchCategoryId) {
        List<TemuOrderBatchCategoryRespVO> list = service.listByBatchCategoryId(batchCategoryId);
        return success(list);
    }

    // 3. 按 categoryId 查询
    @GetMapping("/list-by-category")
    public CommonResult<List<TemuOrderBatchCategoryRespVO>> listByCategoryId(@RequestParam(value = "categoryId", required = false) Long categoryId) {
        List<TemuOrderBatchCategoryRespVO> list = service.listByCategoryId(categoryId);
        return success(list);
    }

    // 4. 修改 batchCategoryId
    @PutMapping("/update-batch-category-id")
    public CommonResult<Boolean> updateBatchCategoryId(@RequestParam("batchCategoryId") String batchCategoryId,
                                                      @RequestParam("categoryId") Long categoryId) {
        boolean result = service.updateBatchCategoryId(batchCategoryId, categoryId);
        return success(result);
    }
}
