package cn.iocoder.yudao.module.temu.controller.admin.vo.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - 店铺 Response VO")
@Data
public class TemuShopRespVO {
    
    @Schema(description = "编号", required = true, example = "1024")
    private Long id;
    
    @Schema(description = "店铺ID", required = true, example = "2048")
    private Long shopId;
    
    @Schema(description = "店铺名称", required = true, example = "测试店铺")
    private String shopName;
    
    @Schema(description = "授权Token", required = true, example = "token")
    private String accessToken;
    
    @Schema(description = "信息通知机器人webhook地址")
    private String webhook;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "合规单类型配置", example = "{\"0\":\"http://example.com\"}")
    private Map<String, Object> oldTypeUrl;

} 