package cn.iocoder.yudao.module.temu.controller.admin.vo.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Schema(description = "管理后台 - Temu OpenAPI 店铺分页查询 Response VO")
@Data
public class TemuOpenapiShopPageRespVO {

    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "店铺名称", example = "测试店铺")
    private String shopName;

    @Schema(description = "授权平台", example = "temu")
    private String platform;

    @Schema(description = "店铺ID", example = "123456")
    private String shopId;

    @Schema(description = "店铺token", example = "token123")
    private String token;

    @Schema(description = "店铺负责人", example = "张三")
    private String owner;

    @Schema(description = "授权时间")
    private Date authTime;

    @Schema(description = "授权到期时间")
    private Date authExpireTime;

    @Schema(description = "是否是半托管店铺", example = "false")
    private Boolean semiManagedMall;

    @Schema(description = "是否是二手店", example = "false")
    private Boolean isThriftStore;

    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "应用appKey", example = "appKey123")
    private String appKey;

    @Schema(description = "应用appSecret", example = "appSecret123")
    private String appSecret;

} 