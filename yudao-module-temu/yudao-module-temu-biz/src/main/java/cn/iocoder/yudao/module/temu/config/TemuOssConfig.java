package cn.iocoder.yudao.module.temu.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "temu.oss")
@Data
public class TemuOssConfig {

    /**
     * OSS的endpoint
     */
    private String endpoint;

    /**
     * OSS的accessKeyId
     */
    private String accessKeyId;

    /**
     * OSS的accessKeySecret
     */
    private String accessKeySecret;

    /**
     * OSS的bucketName
     */
    private String bucketName;
    
    /**
     * OSS访问域名前缀，用于拼接返回给前端的URL
     */
    private String domain;
    
    /**
     * 文件目录前缀
     */
    private String directoryPrefix = "temu/images/";

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
} 