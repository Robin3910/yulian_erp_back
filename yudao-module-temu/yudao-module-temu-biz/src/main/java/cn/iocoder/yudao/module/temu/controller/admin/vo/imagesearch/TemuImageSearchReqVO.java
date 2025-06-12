package cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 图片搜索 Request VO")
@Data
public class TemuImageSearchReqVO {

    @Schema(description = "图片文件，支持 png、jpg、jpeg、bmp、gif 格式，最大 4MB", required = true)
    @NotNull(message = "图片文件不能为空")
    private MultipartFile file;

    @Schema(description = "返回结果数量，默认10")
    private Integer num;

    @Schema(description = "类目ID")
    private Integer categoryId;

    @Schema(description = "是否进行主体识别，默认为true")
    private Boolean crop;

    @Schema(description = "主体区域，格式：x1,y1,x2,y2")
    private String region;

}