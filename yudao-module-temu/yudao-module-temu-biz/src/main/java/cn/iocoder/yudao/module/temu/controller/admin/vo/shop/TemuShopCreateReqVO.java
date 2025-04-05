package cn.iocoder.yudao.module.temu.controller.admin.vo.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 店铺创建 Request VO")
@Data
public class TemuShopCreateReqVO {
    
    @Schema(description = "店铺ID", required = true, example = "2048")
    @NotNull(message = "店铺ID不能为空")
    private Long shopId;
    
    @Schema(description = "店铺名称", required = true, example = "测试店铺")
    @NotEmpty(message = "店铺名称不能为空")
    private String shopName;
} 