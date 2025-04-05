package cn.iocoder.yudao.module.temu.service.oss;

import cn.iocoder.yudao.module.temu.config.TemuOssConfig;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云OSS服务实现类
 */
@Service
@Slf4j
public class TemuOssServiceImpl implements TemuOssService {

    @Resource
    private OSS ossClient;

    @Resource
    private TemuOssConfig ossConfig;

    @Override
    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // 生成文件名 - 当前日期目录 + UUID + 后缀
        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectName = ossConfig.getDirectoryPrefix() + todayStr + "/" 
                + UUID.randomUUID().toString().replace("-", "") + suffix;
        
        try {
            // 创建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossConfig.getBucketName(), objectName, file.getInputStream());
            
            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(getContentType(suffix));
            metadata.setContentLength(file.getSize());
            putObjectRequest.setMetadata(metadata);
            
            // 上传文件
            ossClient.putObject(putObjectRequest);
            
            // 返回访问URL
            return ossConfig.getDomain() + "/" + objectName;
        } catch (IOException e) {
            log.error("上传文件到OSS失败", e);
            throw new RuntimeException("上传文件失败", e);
        }
    }
    
    /**
     * 获取文件的内容类型
     */
    private String getContentType(String suffix) {
        suffix = suffix.toLowerCase();
        if (suffix.equals(".jpg") || suffix.equals(".jpeg")) {
            return "image/jpeg";
        } else if (suffix.equals(".png")) {
            return "image/png";
        } else if (suffix.equals(".gif")) {
            return "image/gif";
        } else if (suffix.equals(".bmp")) {
            return "image/bmp";
        } else if (suffix.equals(".webp")) {
            return "image/webp";
        } else {
            return "application/octet-stream";
        }
    }
}