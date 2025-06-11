package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageAddReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageSearchReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageSearchRespVO;
import cn.iocoder.yudao.module.temu.service.imageSearch.TemuImageSearchService;
import cn.iocoder.yudao.module.temu.utils.imagesearch.ImageValidateUtils;
import com.aliyun.imagesearch20201214.models.AddImageResponse;
import com.aliyun.imagesearch20201214.models.SearchImageByPicResponse;
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
    public CommonResult<?> searchImage(@Validated TemuImageSearchReqVO reqVO) throws IOException {
        // 校验图片
        CommonResult<?> validateResult = ImageValidateUtils.validateImage(reqVO.getFile());
        if (validateResult != null) {
            return CommonResult.error(validateResult.getCode(), validateResult.getMsg());
        }
        // 确保returnHighestOnly有默认值
        if (reqVO.getReturnHighestOnly() == null) {
            reqVO.setReturnHighestOnly(true);
        }
        // 根据是否只返回最高分结果调用不同的服务方法

        if (Boolean.TRUE.equals(reqVO.getReturnHighestOnly())) {
            // 图片搜索阿里云图库，只返回最高分的定制sku,如果最高分相同的存在多个，则都返回
            List<TemuImageSearchRespVO> result = imageSearchService.searchImageByPicWithHighestScore(reqVO);
            return success(result);
        } else {
            // 返回默认10条，按分数排名
            SearchImageByPicResponse response = imageSearchService.searchImageByPic(reqVO);
            return success(response);
        }
    }
}
