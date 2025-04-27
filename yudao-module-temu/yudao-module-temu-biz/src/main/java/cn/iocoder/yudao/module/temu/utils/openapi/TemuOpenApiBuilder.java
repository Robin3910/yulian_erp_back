package cn.iocoder.yudao.module.temu.utils.openapi;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class TemuOpenApiBuilder {
	@Value("${temu.open-api.base-url}")
	private String baseUrl;
	@Value("${temu.open-api.app-key}")
	private String appKey;
	@Value("${temu.open-api.app-secret}")
	private String appSecret;
	
	@Value("${temu.open-api.app-secret}")
	public TemuOpenApiUtil builder(String accessToken) {
		TemuOpenApiUtil temuOpenApiUtil = new TemuOpenApiUtil();
		temuOpenApiUtil.setAppKey(appKey);
		temuOpenApiUtil.setAccessToken(accessToken);
		temuOpenApiUtil.setBaseUrl(baseUrl);
		temuOpenApiUtil.setAppSecret(appSecret);
		return temuOpenApiUtil;
	}
}
