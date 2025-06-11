package cn.iocoder.yudao.module.temu.config;

import com.aliyun.imagesearch20201214.Client;
import com.aliyun.teaopenapi.models.Config;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 图像搜索配置类
 */
@Configuration
@ConfigurationProperties(prefix = "temu.imagesearch")
@Data
@Slf4j
public class TemuImageSearchConfig {

    //实例名称
    private String instanceName;

    //区域ID
    private String regionId;

    //访问端点
    private String endpoint;

    /**
     * 创建图像搜索客户端
     */
    @Bean
    public Client imageSearchClient(TemuOssConfig ossConfig) throws Exception {
        // 使用OSS配置中的AccessKey
        Config authConfig = new Config();
        authConfig.accessKeyId = ossConfig.getAccessKeyId();
        authConfig.accessKeySecret = ossConfig.getAccessKeySecret();
        authConfig.type = "access_key";
        authConfig.regionId = regionId;
        authConfig.endpoint = endpoint;

        Client client = new Client(authConfig);
        log.info("[imageSearchClient][初始化图像搜索客户端完成，实例({})]", instanceName);
        return client;
    }
}