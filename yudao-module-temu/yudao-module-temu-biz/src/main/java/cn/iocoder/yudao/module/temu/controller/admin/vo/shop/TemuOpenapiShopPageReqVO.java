package cn.iocoder.yudao.module.temu.controller.admin.vo.shop;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - Temu OpenAPI 店铺分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemuOpenapiShopPageReqVO extends PageParam {

    @Schema(description = "店铺名称", example = "测试店铺")
    private String shopName;

    @Schema(description = "授权平台", example = "temu")
    private String platform;

} 