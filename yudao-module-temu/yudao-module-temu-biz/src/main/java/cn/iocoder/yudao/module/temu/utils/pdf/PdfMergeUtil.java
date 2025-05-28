package cn.iocoder.yudao.module.temu.utils.pdf;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.temu.service.oss.TemuOssService;
import cn.iocoder.yudao.module.temu.utils.io.ThrottledInputStream;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.PDF_PARSE_LIMIT_NOT_EXISTS;

import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

/**
 * PDF合并工具类
 * 用于合并合规单和商品条码PDF
 */
@Slf4j
@Component
public class PdfMergeUtil {

    private static ConfigApi configApi;

    @Resource
    public void setConfigApi(ConfigApi configApi) {
        PdfMergeUtil.configApi = configApi;
    }

    // 下载限速：128KB/s
    private static final int DOWNLOAD_SPEED_LIMIT = 50 * 1024;

    // 上传限速：128KB/s
    private static final int UPLOAD_SPEED_LIMIT = 50 * 1024;

    /**
     * 从URL下载PDF文档，并应用带宽限制
     */
    private static PDDocument downloadPdfWithThrottle(String pdfUrl) throws IOException {
        URL url = new URL(pdfUrl);
        try (InputStream inputStream = url.openStream();
        
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
                // 应用限速
                String limitStr = configApi.getConfigValueByKey("yulian.pdf_parse_limit");
                int limit = DOWNLOAD_SPEED_LIMIT;
                if (StrUtil.isNotEmpty(limitStr)) {
                    try {
                        limit = Integer.parseInt(limitStr);
                        if (limit <= 0) {
                            limit = DOWNLOAD_SPEED_LIMIT;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("PDF解析限速配置格式错误，使用默认值");
                    }
                }
                ThrottledInputStream throttledInputStream = new ThrottledInputStream(bufferedInputStream, limit);
    
            // ThrottledInputStream throttledInputStream = new ThrottledInputStream(bufferedInputStream,
                    // DOWNLOAD_SPEED_LIMIT);
            return PDDocument.load(throttledInputStream);
        }
    }

    /**
     * 带限速的文件上传到OSS
     */
    private static String uploadFileWithThrottle(MultipartFile file, TemuOssService ossService) throws IOException {
        // 使用缓冲流来读取文件
        try (BufferedInputStream bis = new BufferedInputStream(file.getInputStream())) {
            // 创建限速输入流
            // ThrottledInputStream throttledInputStream = new ThrottledInputStream(bis, UPLOAD_SPEED_LIMIT);

            String limitStr = configApi.getConfigValueByKey("yulian.pdf_parse_limit");
            int limit = UPLOAD_SPEED_LIMIT;
            if (StrUtil.isNotEmpty(limitStr)) {
                try {
                    limit = Integer.parseInt(limitStr);
                    if (limit <= 0) {
                        limit = UPLOAD_SPEED_LIMIT;
                    }
                } catch (NumberFormatException e) {
                    log.warn("PDF解析限速配置格式错误，使用默认值");
                }
            }
            ThrottledInputStream throttledInputStream = new ThrottledInputStream(bis, limit);

            // 创建新的MultipartFile对象，使用限速输入流
            MultipartFile throttledFile = new MockMultipartFile(
                    file.getName(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    throttledInputStream);

            // 上传到OSS
            return ossService.uploadFile(throttledFile);
        }
    }

    /**
     * 合并两个PDF文件并上传到OSS（统一入口）
     *
     * @param firstPdfUrl  第一个PDF的URL（合规单）
     * @param secondPdfUrl 第二个PDF的URL（商品条码）
     * @param ossService   OSS服务
     * @return 合并后的PDF的OSS URL，如果合并失败则返回null
     */
    public static String mergePdfsAndUpload(String firstPdfUrl, String secondPdfUrl, TemuOssService ossService) {
        if (StrUtil.hasBlank(firstPdfUrl, secondPdfUrl)) {
            return null;
        }

        try {
            // 验证文件类型
            if (!isPdfFile(firstPdfUrl) || !isPdfFile(secondPdfUrl)) {
                log.error("文件类型错误，必须是PDF文件。firstPdfUrl: {}, secondPdfUrl: {}", firstPdfUrl, secondPdfUrl);
                return null;
            }

            // 直接合并PDF
            byte[] mergedPdfBytes = mergePdfsWithScaling(firstPdfUrl, secondPdfUrl);
            if (mergedPdfBytes == null) {
                return null;
            }

            // 生成文件名
            String fileName = String.format("merged_compliance_%s.pdf", StrUtil.uuid());
            // 创建MultipartFile对象
            MultipartFile multipartFile = new MockMultipartFile(
                    fileName,
                    fileName,
                    "application/pdf",
                    mergedPdfBytes);
            // 使用限速上传到OSS
            return uploadFileWithThrottle(multipartFile, ossService);
        } catch (Exception e) {
            log.error("合并PDF失败。firstPdfUrl: {}, secondPdfUrl: {}", firstPdfUrl, secondPdfUrl, e);
            return null;
        }
    }

    /**
     * 合并两个PDF文件，并对合规单进行缩放（内部方法）
     */
    private static byte[] mergePdfsWithScaling(String firstPdfUrl, String secondPdfUrl) throws IOException {
        // 使用临时文件来减少内存使用
        File tempFile = null;
        try (PDDocument firstDoc = downloadPdfWithThrottle(firstPdfUrl);
                PDDocument secondDoc = downloadPdfWithThrottle(secondPdfUrl);
                PDDocument mergedDoc = new PDDocument()) {

            // 获取两个PDF的第一页
            PDPage compliancePage = firstDoc.getPage(0);
            PDPage barcodePage = secondDoc.getPage(0);

            // 获取页面尺寸
            PDRectangle complianceSize = compliancePage.getMediaBox();
            PDRectangle barcodeSize = barcodePage.getMediaBox();

            // 创建新页面，使用合规单的尺寸
            PDPage newPage = new PDPage(complianceSize);
            mergedDoc.addPage(newPage);

            // 创建LayerUtility用于导入页面
            LayerUtility layerUtility = new LayerUtility(mergedDoc);
            PDFormXObject barcodeForm = layerUtility.importPageAsForm(secondDoc, 0);
            PDFormXObject complianceForm = layerUtility.importPageAsForm(firstDoc, 0);

            float targetComplianceHeight = complianceSize.getHeight() - barcodeSize.getHeight();
            float heightScale = targetComplianceHeight / complianceSize.getHeight();

            // 创建临时文件
            tempFile = File.createTempFile("merged_pdf_", ".pdf");

            // 使用临时文件写入合并后的PDF
            try (PDPageContentStream contentStream = new PDPageContentStream(mergedDoc, newPage)) {
                // 绘制商品条码
                contentStream.saveGraphicsState();
                float barcodeX = (complianceSize.getWidth() - barcodeSize.getWidth()) / 2;
                float barcodeY = complianceSize.getHeight() - barcodeSize.getHeight();
                contentStream.transform(Matrix.getTranslateInstance(barcodeX, barcodeY));
                contentStream.drawForm(barcodeForm);
                contentStream.restoreGraphicsState();

                // 绘制合规单
                contentStream.saveGraphicsState();
                contentStream.transform(Matrix.getTranslateInstance(0, 0));
                contentStream.transform(Matrix.getScaleInstance(1.0f, heightScale));
                contentStream.drawForm(complianceForm);
                contentStream.restoreGraphicsState();
            }

            mergedDoc.save(tempFile);

            // 使用NIO读取临时文件
            byte[] result = Files.readAllBytes(tempFile.toPath());
            return result;

        } catch (Exception e) {
            log.error("合并PDF文件失败", e);
            return null;
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    log.warn("删除临时文件失败", e);
                }
            }
        }
    }

    /**
     * 在PDF文档中查找包含指定SKU的页面
     */
    private static int findPageWithSku(PDDocument document, String customSku) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();

        // 遍历每一页
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);
            String pageText = stripper.getText(document);

            // 检查页面文本是否包含SKU
            if (pageText.contains(customSku)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 判断是否为PDF文件
     */
    private static boolean isPdfFile(String url) {
        return "pdf".equalsIgnoreCase(FileUtil.getSuffix(url));
    }

    /**
     * 商品条码PDF多页截取定制sku对应页
     *
     * @param goodsSn    商品条码的URL
     * @param customSku  定制SKU
     * @param ossService OSS服务
     * @return 提取后的单页PDF的OSS URL，如果提取失败则返回null
     */
    public static String extractPageBySku(String goodsSn, String customSku, TemuOssService ossService) {
        if (StrUtil.hasBlank(goodsSn, customSku)) {
            return null;
        }

        try {
            // 验证文件类型
            if (!isPdfFile(goodsSn)) {
                log.error("文件类型错误，必须是PDF文件。pdfUrl: {}", goodsSn);
                return null;
            }

            try (PDDocument document = downloadPdfWithThrottle(goodsSn)) {
                int pageCount = document.getNumberOfPages();

                // 如果是单页PDF，直接返回原URL
                if (pageCount == 1) {
                    return goodsSn;
                }

                // 查找包含SKU的页面
                int targetPage = findPageWithSku(document, customSku);
                if (targetPage == -1) {
                    log.error("未找到包含SKU的页面。SKU: {}", customSku);
                    return null;
                }

                // 创建新的PDF文档，只包含目标页面
                try (PDDocument newDoc = new PDDocument()) {
                    newDoc.addPage(document.getPage(targetPage));
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    newDoc.save(baos);

                    // 创建MultipartFile对象
                    String fileName = String.format("extracted_page_%s.pdf", StrUtil.uuid());
                    MultipartFile multipartFile = new MockMultipartFile(
                            fileName,
                            fileName,
                            "application/pdf",
                            baos.toByteArray());

                    // 使用限速上传到OSS
                    return uploadFileWithThrottle(multipartFile, ossService);
                }
            }
        } catch (Exception e) {
            log.error("提取PDF页面失败。pdfUrl: {}, customSku: {}", goodsSn, customSku, e);
            return null;
        }
    }

}