package cn.iocoder.yudao.module.temu.controller.admin.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 更新订单定制图片 Request VO
 */
@Schema(description = "管理后台 - 更新订单定制图片 Request VO")
@Data
public class TemuOrderUpdateCustomImagesReqVO {

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "定制图片URL列表，多个URL使用逗号分隔", example = "http://example.com/image1.jpg,http://example.com/image2.jpg")
    @Size(max = 10000, message = "定制图片URL总长度不能超过10000个字符")
    private String customImageUrls;
} 