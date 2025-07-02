package cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderPlacementRecordRespVO {
    @Schema(description = "记录ID")
    private Long id;
    @Schema(description = "订单编号")
    private String orderNo;
    @Schema(description = "店铺ID")
    private Long shopId;
    @Schema(description = "店铺名称")
    private String shopName;
    @Schema(description = "商品标题")
    private String productTitle;
    @Schema(description = "商品属性")
    private String productProperties;
    @Schema(description = "类目名称")
    private String categoryName;
    @Schema(description = "官网数量")
    private Integer originalQuantity;
    @Schema(description = "制作数量")
    private Integer quantity;
    @Schema(description = "单价")
    private BigDecimal unitPrice;
    @Schema(description = "总价")
    private BigDecimal totalPrice;
    @Schema(description = "SKU编号")
    private String sku;
    @Schema(description = "SKC编号")
    private String skc;
    @Schema(description = "定制SKU")
    private String customSku;
    @Schema(description = "是否返单")
    private Integer isReturnOrder;
    @Schema(description = "操作人")
    private String operator;
    @Schema(description = "操作时间")
    private LocalDateTime operationTime;
} 