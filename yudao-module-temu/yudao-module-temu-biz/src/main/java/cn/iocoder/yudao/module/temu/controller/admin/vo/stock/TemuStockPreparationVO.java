package cn.iocoder.yudao.module.temu.controller.admin.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - Temu备货单信息 Response VO")
@Data
public class TemuStockPreparationVO {

    @Schema(description = "备货单号", example = "WB250723124404")
    private String subPurchaseOrderSn;

    @Schema(description = "标题", example = "示例商品")
    private String productName;

    @Schema(description = "商品SKCID", example = "SKC123456")
    private String productSkcId;

    @Schema(description = "商品类目", example = "卡片")
    private String category;

    @Schema(description = "供应商ID", example = "2048")
    private String supplierId;

    @Schema(description = "供应商名称", example = "示例供应商")
    private String supplierName;

    @Schema(description = "下单时间", example = "2024-01-20 10:00:00")
    private String purchaseTime;

    @Schema(description = "订单状态", example = "1", 
            allowableValues = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"})
    private Integer status; // 0-待接单；1-已接单，待发货；2-已送货；3-已收货；4-已拒收；5-已验收，全部退回；6-已验收；7-已入库；8-作废；9-已超时

    @Schema(description = "SKU详情列表")
    private List<SkuDetail> skuQuantityDetailList;

    @Data
    @Schema(description = "SKU详情")
    public static class SkuDetail {
        @Schema(description = "属性", example = "10pcs")
        private String className;

        @Schema(description = "产品图片")
        private List<String> thumbUrlList;

        @Schema(description = "商品SKU ID", example = "123456")
        private String productSkuId;

        @Schema(description = "定制SKU ID", example = "FSKU123456")
        private String fulfilmentProductSkuId;

        @Schema(description = "下单数量", example = "150")
        private Integer purchaseQuantity;
    }
} 