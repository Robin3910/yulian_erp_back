package cn.iocoder.yudao.module.temu.utils.pdf;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.temu.service.oss.TemuOssService;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

/**
 * PDF合并工具类
 * 用于合并合规单和商品条码PDF
 */
@Slf4j
public class PdfMergeUtil {

    /**
     * 合并两个PDF文件并上传到OSS（统一入口）
     *
     * @param firstPdfUrl  第一个PDF的URL（合规单）
     * @param secondPdfUrl 第二个PDF的URL（商品条码）
     * @param customSku    商品SKU（可选，用于多页PDF时匹配指定页面）
     * @param ossService   OSS服务
     * @return 合并后的PDF的OSS URL，如果合并失败则返回null
     */
    public static String mergePdfsAndUpload(String firstPdfUrl, String secondPdfUrl, String customSku,
            TemuOssService ossService) {
        if (StrUtil.hasBlank(firstPdfUrl, secondPdfUrl)) {
            return null;
        }

        try {
            // 验证文件类型
            if (!isPdfFile(firstPdfUrl) || !isPdfFile(secondPdfUrl)) {
                log.error("文件类型错误，必须是PDF文件。firstPdfUrl: {}, secondPdfUrl: {}", firstPdfUrl, secondPdfUrl);
                return null;
            }
            // 合并PDF
            byte[] mergedPdfBytes;
            // 如果提供了SKU，先检查是否需要处理多页PDF
            if (StrUtil.isNotEmpty(customSku)) {
                try (PDDocument secondDoc = PDDocument.load(new URL(secondPdfUrl).openStream())) {
                    int pageCount = secondDoc.getNumberOfPages();

                    if (pageCount > 1) {
                        // 多页PDF，需要查找匹配页
                        int targetPage = findPageWithSku(secondDoc, customSku);
                        if (targetPage == -1) {
                            log.error("未找到包含SKU的商品条码页面。SKU: {}", customSku);
                            return null;
                        }
                        // 创建临时文件保存匹配的页面
                        try (PDDocument tempDoc = new PDDocument()) {
                            tempDoc.addPage(secondDoc.getPage(targetPage));
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            tempDoc.save(baos);
                            // 创建临时文件
                            String tempFileName = String.format("temp_barcode_%s.pdf", StrUtil.uuid());
                            MultipartFile tempFile = new MockMultipartFile(
                                    tempFileName,
                                    tempFileName,
                                    "application/pdf",
                                    baos.toByteArray());
                            // 上传临时文件到OSS并获取URL
                            String tempUrl = ossService.uploadFile(tempFile);
                            // 使用临时URL进行合并
                            mergedPdfBytes = mergePdfsWithScaling(firstPdfUrl, tempUrl);
                        }
                    } else {
                        // 单页PDF，直接合并
                        mergedPdfBytes = mergePdfsWithScaling(firstPdfUrl, secondPdfUrl);
                    }
                }
            } else {
                // 未提供SKU，按单页处理
                mergedPdfBytes = mergePdfsWithScaling(firstPdfUrl, secondPdfUrl);
            }
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
            // 上传到OSS
            return ossService.uploadFile(multipartFile);
        } catch (Exception e) {
            log.error("合并PDF失败。firstPdfUrl: {}, secondPdfUrl: {}, customSku: {}", firstPdfUrl, secondPdfUrl, customSku,
                    e);
            return null;
        }
    }
    /**
     * 合并两个PDF文件，并对合规单进行缩放（内部方法）
     */
    private static byte[] mergePdfsWithScaling(String firstPdfUrl, String secondPdfUrl) throws IOException {
        try (PDDocument firstDoc = PDDocument.load(new URL(firstPdfUrl).openStream());
                PDDocument secondDoc = PDDocument.load(new URL(secondPdfUrl).openStream());
                PDDocument mergedDoc = new PDDocument()) {
            // 获取两个PDF的第一页
            PDPage compliancePage = firstDoc.getPage(0); // 合规单
            PDPage barcodePage = secondDoc.getPage(0); // 商品条码

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

            // 计算合规单需要缩放的高度比例
            // 目标：商品条码原尺寸 + 缩放后的合规单高度 = 原合规单高度
            float targetComplianceHeight = complianceSize.getHeight() - barcodeSize.getHeight();
            float heightScale = targetComplianceHeight / complianceSize.getHeight();

            // 开始内容流
            try (PDPageContentStream contentStream = new PDPageContentStream(mergedDoc, newPage)) {
                // 绘制商品条码 - 原尺寸，居中放置在上方
                contentStream.saveGraphicsState();
                // 计算水平居中的位置
                float barcodeX = (complianceSize.getWidth() - barcodeSize.getWidth()) / 2;
                float barcodeY = complianceSize.getHeight() - barcodeSize.getHeight();
                contentStream.transform(Matrix.getTranslateInstance(barcodeX, barcodeY));
                contentStream.drawForm(barcodeForm);
                contentStream.restoreGraphicsState();

                // 绘制合规单 - 只缩小高度，放在下方
                contentStream.saveGraphicsState();
                // 计算水平居中的位置
                float complianceX = 0; // 不需要水平居中，保持原始宽度
                float complianceY = 0; // 从底部开始
                contentStream.transform(Matrix.getTranslateInstance(complianceX, complianceY));
                // 分别设置宽度和高度的缩放比例
                contentStream.transform(Matrix.getScaleInstance(1.0f, heightScale)); // 宽度比例为1，保持不变
                contentStream.drawForm(complianceForm);
                contentStream.restoreGraphicsState();
            }

            // 将合并后的文档转换为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mergedDoc.save(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("合并PDF文件失败", e);
            return null;
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

}