package cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "temu管理 - 订单集合 Response VO")
@Data
public class TemuOrderListRespVO {

    @Schema(description = "主键ID")
    private Long id;

    private String orderNo;

    private String productTitle;

    private Integer orderStatus;

    private String sku;

    private String skc;

    private BigDecimal salePrice;

    private String customSku;

    private Integer quantity;

    private Integer originalQuantity;

    private String productProperties;

    private Long shopId;

    private String customImageUrls;

    private String customTextList;

    private String productImgUrl;

    private String categoryId;

    private String effectiveImgUrl;

    private String complianceUrl; // 合规单URL

    @Schema(description = "合规单URL")
    private String oldTypeUrl;

    @Schema(description = "合规单图片URL")
    private String complianceImageUrl;

    @Schema(description = "合规单和商品条码PDF合并URL")
    private String complianceGoodsMergedUrl;

    @Schema(description = "是否完成生产任务")
    private Integer isCompleteProducerTask;

    @Schema(description = "分拣序号，用于标识订单分拣的顺序，支持下划线格式")
    private String sortingSequence;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "平台订单时间")
    private LocalDateTime bookingTime;

    @Schema(description = "是否找齐：1-是，0-否")
    private Integer isFoundAll;

    @Schema(description = "发货人ID")
    private Long senderId;

    @Schema(description = "发货人姓名")
    private String senderName;
}
