package cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 上传图片 Request VO")
@Data
public class TemuImageAddReqVO {

    @Schema(description = "图片文件，支持PNG、JPG、JPEG、BMP、GIF、WEBP、TIFF、PPM格式，大小不超过4MB", required = true)
    @NotNull(message = "图片文件不能为空")
    private MultipartFile file;

    @Schema(description = "定制sku，最多256个字符", required = true, example = "51978533533490")
    @NotBlank(message = "定制sku不能为空")
    @Size(max = 256, message = "定制sku不能超过256个字符")
    private String productId;


    @Schema(description = "图片名称，最多256个字符", required = true, example = "51978533533490_1_20250611")
    @NotBlank(message = "图片名称不能为空")
    @Size(max = 256, message = "图片名称不能超过256个字符")
    private String picName;



}