package cn.iocoder.yudao.module.temu.controller.admin.vo.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - Temu OpenAPI 店铺保存 Request VO")
@Data
public class TemuOpenapiShopSaveReqVO {

    @Schema(description = "店铺token", requiredMode = Schema.RequiredMode.REQUIRED, example = "token123")
    @NotBlank(message = "店铺token不能为空")
    private String token;

    @Schema(description = "店铺名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "测试店铺")
    @NotBlank(message = "店铺名称不能为空")
    private String shopName;

    @Schema(description = "店铺ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "店铺ID不能为空")
    private String shopId;

    @Schema(description = "店铺负责人", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "店铺负责人不能为空")
    private String owner;

    @Schema(description = "授权平台", requiredMode = Schema.RequiredMode.REQUIRED, example = "temu")
    @NotBlank(message = "授权平台不能为空")
    private String platform;

    @Schema(description = "应用appKey", requiredMode = Schema.RequiredMode.REQUIRED, example = "appKey123")
    @NotBlank(message = "应用appKey不能为空")
    private String appKey;

    @Schema(description = "应用appSecret", requiredMode = Schema.RequiredMode.REQUIRED, example = "appSecret123")
    @NotBlank(message = "应用appSecret不能为空")
    private String appSecret;

} 