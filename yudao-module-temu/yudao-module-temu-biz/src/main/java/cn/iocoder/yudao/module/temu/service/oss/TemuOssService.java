package cn.iocoder.yudao.module.temu.service.oss;

import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云OSS服务接口
 */
public interface TemuOssService {

    /**
     * 上传文件到OSS
     *
     * @param file 文件
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file);
} 