package cn.iocoder.yudao.module.temu.utils.imagesearch;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.TEMU_IMAGE_SIZE_EXCEEDS_LIMIT;
import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.TEMU_IMAGE_TYPE_NOT_SUPPORTED;

/**
 * 图片验证工具类
 */
public class ImageValidateUtils {

    /**
     * 验证图片文件
     * 
     * @param file 图片文件
     * @return 如果验证失败，返回错误结果；如果验证成功，返回null
     */
    public static CommonResult<?> validateImage(MultipartFile file) {
        // 校验文件大小
        if (file.getSize() > 4 * 1024 * 1024) { // 4MB
            return CommonResult.error(TEMU_IMAGE_SIZE_EXCEEDS_LIMIT);
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (!isValidImageType(contentType)) {
            return CommonResult.error(TEMU_IMAGE_TYPE_NOT_SUPPORTED);
        }

        return null;
    }

    /**
     * 检查文件类型是否为支持的图片格式
     */
    private static boolean isValidImageType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.equals("image/png") ||
                contentType.equals("image/jpeg") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/bmp") ||
                contentType.equals("image/webp") ||
                contentType.equals("image/tiff") ||
                contentType.equals("image/x-portable-pixmap");
    }
}