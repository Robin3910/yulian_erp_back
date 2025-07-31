package cn.iocoder.yudao.module.temu.controller.admin.vo.rework;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 订单返工 Response VO")
@Data
public class TemuOrderReworkRespVO {

    @Schema(description = "主键ID", example = "1024")
    private Long id;

    @Schema(description = "订单编号", example = "ORDER_001")
    private String orderNo;

    @Schema(description = "商品标题", example = "高品质定制商品")
    private String productTitle;

    @Schema(description = "商品图片URL", example = "https://example.com/image.jpg")
    private String productImgUrl;

    @Schema(description = "商品属性", example = "颜色:红色,尺寸:L")
    private String productProperties;

    @Schema(description = "SKU编号", example = "SKU_001")
    private String sku;

    @Schema(description = "SKC编号", example = "SKC_001")
    private String skc;

    @Schema(description = "定制SKU", example = "CUSTOM_SKU_001")
    private String customSku;

    @Schema(description = "返工原因", example = "产品质量问题，需要重新生产")
    private String reworkReason;

    @Schema(description = "返工发起人姓名", example = "张三")
    private String reworkInitiatorName;

    @Schema(description = "返工作图人姓名", example = "李四")
    private String reworkDrawUserName;

    @Schema(description = "返工作图人ID", example = "12345")
    private Long reworkDrawUserId;

    @Schema(description = "上一次返工作图人姓名", example = "王五")
    private String lastDrawUserName;

    @Schema(description = "上一次返工作图人ID", example = "12344")
    private Long lastDrawUserId;

    @Schema(description = "是否完成 0未完成 1已完成", example = "0")
    private Integer isFinished;

    @Schema(description = "返工次数", example = "1")
    private Integer reworkCount;

    @Schema(description = "定制图片列表URL", example = "http://example.com/image1.jpg,http://example.com/image2.jpg")
    private String customImageUrls;

    @Schema(description = "定制文字列表", example = "定制文字1,定制文字2")
    private String customTextList;

    @Schema(description = "店铺ID", example = "1024")
    private Long shopId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

} 