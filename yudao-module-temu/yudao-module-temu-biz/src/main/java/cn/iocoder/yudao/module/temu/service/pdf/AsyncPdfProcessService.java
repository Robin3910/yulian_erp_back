package cn.iocoder.yudao.module.temu.service.pdf;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.service.oss.TemuOssService;
import cn.iocoder.yudao.module.temu.utils.pdf.PdfMergeUtil;
import cn.iocoder.yudao.module.temu.utils.pdf.PdfToImageUtil;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.CompletableFuture;


/**
 * 异步PDF处理服务
 * 负责处理PDF合并、页面提取等耗时操作，通过异步线程池提升系统吞吐量。
 * @author ykh
 */
@Service
@Slf4j
public class AsyncPdfProcessService {

    @Resource
    private TemuOrderMapper temuOrderMapper;

    @Resource
    private ConfigApi configApi;

    /**
     * 异步合并PDF文件并上传至OSS
     * 流程说明：
     *   1.从合规单URL（complianceUrl）下载PDF文件
     *   2.从商品条形码URL（goodsSnUrl）下载PDF文件
     *   3.合并两个PDF文件，生成新的PDF
     *   4.将合并后的PDF上传至OSS，返回访问地址
     * @param complianceUrl  合规单URL（
     * @param goodsSnUrl     商品条形码URL
     * @param temuOssService OSS服务接口，用于文件上传操作
     * @return CompletableFuture<String> 异步返回合并后的PDF文件OSS地址
     * @throws Exception 异步任务内部异常（已捕获并记录日志，返回null）
     */
    @Async("pdfProcessExecutor")// 指定自定义线程池执行异步任务
    public CompletableFuture<String> processPdfAsync(String complianceUrl, String goodsSnUrl,
            TemuOssService temuOssService) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                log.info("[PDF合并] 开始处理 - customSku: {}, complianceUrl: {}, goodsSnUrl: {}",
                         complianceUrl, goodsSnUrl);

                // 核心操作：合并PDF并上传OSS
                String result = PdfMergeUtil.mergePdfsAndUpload(complianceUrl, goodsSnUrl, temuOssService);

                long endTime = System.currentTimeMillis();
                log.info("[PDF合并] 处理完成 - customSku: {}, 耗时: {}ms, 结果URL: {}",
                        (endTime - startTime), result);

                return result;
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                log.error("[PDF合并] 处理失败 - customSku: {}, 耗时: {}ms, 错误: {}",
                         (endTime - startTime), e.getMessage(), e);
                return null;
            }
        });
    }

    /**
     * 异步从PDF文件中提取指定页面并上传至OSS
     * 场景：从多页商品条形码PDF中提取与SKU匹配的特定页面
     * @param goodsSnUrl     商品条形码URL（多页）
     * @param customSku      定制SKU，用于匹配需要提取的页面
     * @param temuOssService OSS服务接口，用于文件上传操作
     * @return CompletableFuture<String> 异步返回提取后的PDF文件OSS地址
     * @throws Exception 异步任务内部异常（已捕获并记录日志，返回null）
     */
    @Async("pdfProcessExecutor")
    public CompletableFuture<String> extractPageAsync(String goodsSnUrl, String customSku,
            TemuOssService temuOssService) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                log.info("[PDF提取] 开始处理 - customSku: {}, goodsSnUrl: {}", customSku, goodsSnUrl);

                // 提取指定页面并上传到OSS
                String extractedPdfUrl = PdfMergeUtil.extractPageBySku(goodsSnUrl, customSku, temuOssService);
                
                if (StrUtil.isNotBlank(extractedPdfUrl)) {
                    // 将提取后的PDF转换为图片
                    String imageUrl = PdfToImageUtil.getImageUrl(extractedPdfUrl, "barcode", temuOssService);
                    
                    if (StrUtil.isNotBlank(imageUrl)) {
                        // 检查是否启用条码解析功能
                        String enableBarcodeStr = configApi.getConfigValueByKey("yulian.barcode_parse.enabled");
                        boolean enableBarcode = Boolean.parseBoolean(enableBarcodeStr);
                        
                        if (enableBarcode) {
                            try {
                                // 下载图片并使用ZXing解析条码
                                BufferedImage image = ImageIO.read(new URL(imageUrl).openStream());
                                if (image != null) {
                                    BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
                                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                                    Result result = new MultiFormatReader().decode(bitmap);
                                    
                                    if (result != null) {
                                        String barcodeValue = result.getText();
                                        if (StrUtil.isNotBlank(barcodeValue)) {
                                            // 更新订单的条码值
                                            temuOrderMapper.updateGoodsSnNoByCustomSku(customSku, barcodeValue);
                                            log.info("[条码解析] 成功解析条码 - customSku: {}, barcodeValue: {}", customSku, barcodeValue);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.error("[条码解析] 解析失败 - customSku: {}, error: {}", customSku, e.getMessage(), e);
                            }
                        } else {
                            log.info("[条码解析] 功能未启用 - customSku: {}", customSku);
                        }
                    }
                }

                long endTime = System.currentTimeMillis();
                log.info("[PDF提取] 处理完成 - customSku: {}, 耗时: {}ms, 结果URL: {}", 
                        customSku, (endTime - startTime), extractedPdfUrl);

                return extractedPdfUrl;
            } catch (Exception e) {
                long endTime = System.currentTimeMillis();
                log.error("[PDF提取] 处理失败 - customSku: {}, 耗时: {}ms, 错误: {}", 
                        customSku, (endTime - startTime), e.getMessage(), e);
                return null;
            }
        });
    }
}