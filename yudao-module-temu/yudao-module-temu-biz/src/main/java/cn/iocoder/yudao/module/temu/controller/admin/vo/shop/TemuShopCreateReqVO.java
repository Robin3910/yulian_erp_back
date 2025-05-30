package cn.iocoder.yudao.module.temu.controller.admin.vo.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Schema(description = "管理后台 - 店铺创建 Request VO")
@Data
public class TemuShopCreateReqVO {
	
	
	@Schema(description = "店铺ID", required = true, example = "2048")
	@NotNull(message = "店铺ID不能为空")
	private Long shopId;
	@Schema(description = "授权Token", required = true, example = "token")
//    @NotEmpty(message = "授权Token")
	private String accessToken;
	
	@Schema(description = "店铺名称", required = true, example = "测试店铺")
	@NotEmpty(message = "店铺名称不能为空")
	private String shopName;
	
	@Schema(description = "信息通知机器人webhook地址")
	private String webhook;
	
	@Schema(description = "店铺别名", required = true, example = "aliasName")
	private String aliasName;
	
	@Schema(description = "合规单类型配置", example = "{\"0\":\"http://example.com\"}")
	private Map<String, Object> oldTypeUrl;
} 