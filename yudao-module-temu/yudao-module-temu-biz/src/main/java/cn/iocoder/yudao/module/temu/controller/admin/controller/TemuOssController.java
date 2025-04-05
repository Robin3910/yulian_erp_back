package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.temu.service.oss.TemuOssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 文件上传")
@RestController
@RequestMapping("/temu/oss")
@Validated
@PermitAll
public class TemuOssController {

    @Resource
    private TemuOssService ossService;

    @PostMapping("/upload")
    @Operation(summary = "上传图片")
    // @PreAuthorize("@ss.hasPermission('temu:oss:upload')")
    public CommonResult<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return CommonResult.error(400, "上传文件不能为空");
        }
        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!isValidImageType(suffix)) {
                return CommonResult.error(400, "只支持jpg、jpeg、png、gif、bmp、webp格式的图片");
            }
        }
        // 检查文件大小（限制为10MB）
        if (file.getSize() > 20 * 1024 * 1024) {
            return CommonResult.error(400, "文件大小不能超过20MB");
        }
        
        // 上传文件到OSS
        String imageUrl = ossService.uploadFile(file);
        return success(imageUrl);
    }
    
    /**
     * 判断是否为有效的图片类型
     */
    private boolean isValidImageType(String suffix) {
        return "jpg".equals(suffix) || "jpeg".equals(suffix) || "png".equals(suffix) 
                || "gif".equals(suffix) || "bmp".equals(suffix) || "webp".equals(suffix);
    }
}