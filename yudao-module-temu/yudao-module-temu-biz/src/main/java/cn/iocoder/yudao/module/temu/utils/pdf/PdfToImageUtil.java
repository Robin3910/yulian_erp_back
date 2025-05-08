package cn.iocoder.yudao.module.temu.utils.pdf;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.temu.service.oss.TemuOssService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 合规单 PDF转图片工具类
 */
@Slf4j
public class PdfToImageUtil {

    // 缓存每种类型的转换结果，key为oldType和url的组合，value为转换后的图片URL
    private static final Map<String, String> IMAGE_URL_CACHE = new ConcurrentHashMap<>();

    /**
     * 根据文件URL和类型获取图片URL
     * 如果是PDF则转换为图片，如果是图片则直接返回
     *
     * @param fileUrl    文件URL
     * @param oldType    类型 (0: 0+, 1: 3+, 2: 14+)
     * @param ossService OSS服务
     * @return 图片URL
     */
    public static String getImageUrl(String fileUrl, String oldType, TemuOssService ossService) {
        if (StrUtil.isBlank(fileUrl)) {
            return null;
        }
        // 生成缓存key
        String cacheKey = oldType + "_" + fileUrl;
        // 检查缓存
        String cachedImageUrl = IMAGE_URL_CACHE.get(cacheKey);
        if (cachedImageUrl != null) {
            return cachedImageUrl;
        }
        try {
            // 判断文件类型
            String suffix = FileUtil.getSuffix(fileUrl).toLowerCase();
            if (!isPdfFile(suffix)) {
                // 如果不是PDF文件，直接返回原URL
                IMAGE_URL_CACHE.put(cacheKey, fileUrl);
                return fileUrl;
            }
            // 转换PDF为图片
            byte[] imageBytes = convertPdfToImage(fileUrl);
            if (imageBytes == null) {
                return null;
            }
            // 生成文件名
            String fileName = String.format("old_type_image_%s_%s.png", oldType,
                    StrUtil.uuid());

            // 创建MultipartFile对象
            MultipartFile multipartFile = new MockMultipartFile(
                    fileName,
                    fileName,
                    "image/png",
                    imageBytes);
            // 上传到OSS
            String imageUrl = ossService.uploadFile(multipartFile);
            // 存入缓存
            IMAGE_URL_CACHE.put(cacheKey, imageUrl);

            return imageUrl;
        } catch (Exception e) {
            log.error("处理文件失败，fileUrl: {}, oldType: {}", fileUrl, oldType, e);
            return null;
        }
    }
    /**
     * 判断是否为PDF文件
     */
    private static boolean isPdfFile(String suffix) {
        return "pdf".equalsIgnoreCase(suffix);
    }
    /**
     * 将PDF转换为图片
     */
    private static byte[] convertPdfToImage(String pdfUrl) throws IOException {
        PDDocument document = null;
        try {
            // 下载PDF文件
            document = PDDocument.load(new URL(pdfUrl).openStream());
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            // 转换第一页为图片
            BufferedImage image = pdfRenderer.renderImageWithDPI(0, 300);

            // 将图片转换为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();

        } finally {
            if (document != null) {
                document.close();
            }
        }
    }
    /**
     * 清除缓存
     */
    public static void clearCache() {
        IMAGE_URL_CACHE.clear();
    }
}