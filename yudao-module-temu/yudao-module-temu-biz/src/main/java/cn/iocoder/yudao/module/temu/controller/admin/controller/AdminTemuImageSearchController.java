package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageAddReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageSearchOrderRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageSearchReqVO;
import cn.iocoder.yudao.module.temu.service.imageSearch.TemuImageSearchService;
import cn.iocoder.yudao.module.temu.utils.imagesearch.ImageValidateUtils;
import com.aliyun.imagesearch20201214.models.AddImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 阿里云图像搜索服务")
@RestController
@RequestMapping("/temu/image-search")
@Validated
@Slf4j
public class AdminTemuImageSearchController {

    @Resource
    private TemuImageSearchService imageSearchService;

    @PostMapping("/add")
    @Operation(summary = "上传图片到阿里云图库")
    public CommonResult<AddImageResponse> addImage(@Validated TemuImageAddReqVO reqVO) throws IOException {
        // 校验图片
        CommonResult<?> validateResult = ImageValidateUtils.validateImage(reqVO.getFile());
        if (validateResult != null) {
            return CommonResult.error(validateResult.getCode(), validateResult.getMsg());
        }

        // 调用服务添加图片
        AddImageResponse response = imageSearchService.addImage(reqVO);
        return success(response);
    }

    @PostMapping("/search")
    @Operation(summary = "图片搜索阿里云图库")
    public CommonResult<List<TemuImageSearchOrderRespVO>> searchImage(@Validated TemuImageSearchReqVO reqVO) throws IOException {
        // 校验图片
        CommonResult<?> validateResult = ImageValidateUtils.validateImage(reqVO.getFile());
        if (validateResult != null) {
            return CommonResult.error(validateResult.getCode(), validateResult.getMsg());
        }
        // 调用服务进行图片搜索并返回订单详情
        List<TemuImageSearchOrderRespVO> result = imageSearchService.searchImageByPicWithHighestScore(reqVO);
        return success(result);
    }
}
