package cn.iocoder.yudao.module.temu.api.openapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderInfoDTO {
    private Result result;
    private boolean success;
    private String requestId;
    private Integer errorCode;
    private String errorMsg;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Result {
        private Integer total;
        private List<SubOrderForSupplier> subOrderForSupplierList;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubOrderForSupplier {
        private String originalPurchaseOrderSn;
        private Integer source;
        private String productName;
        private Boolean isFirst;
        private DeliverInfo deliverInfo;
        private List<SkuQuantityDetail> skuQuantityDetailList;
        private Long productSkcId;
        private Boolean isCloseJit;
        private Long warehouseGroupId;
        private Long productId;
        private Integer hasQcBill;
        private Integer applyDeleteStatus;
        private Integer supplyStatus;
        private SkuQuantityTotalInfo skuQuantityTotalInfo;
        private Boolean isCanJoinDeliverPlatform;
        private Integer categoryType;
        private String subPurchaseOrderSn;
        private Integer status;
        private Long supplierId;
        private Integer appealStatus;
        private Boolean isCustomProduct;
        private Boolean supportIncreaseNum;
        private String productSkcPicture;
        private List<LackOrSoldOutTag> lackOrSoldOutTagList;
        private Integer qcReject;
        private Integer purchaseStockType;
        private List<SkuLackItem> skuLackItemList;
        private Integer skuLackSnapshot;
        private Integer settlementType;
        private String supplierName;
        private Integer urgencyType;
        private Integer expectLatestArrivalIntervalDays;
        private Boolean todayCanDeliver;
        private Long purchaseTime;
        private Integer applyChangeSupplyStatus;
        private String category;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeliverInfo {
        private Long expectLatestArrivalTimeOrDefault;
        private Long expectLatestDeliverTimeOrDefault;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkuQuantityDetail {
        private String currencyType;
        private String className;
        private Boolean supportIncreaseNum;
        private Integer realReceiveAuthenticQuantity;
        private Long fulfilmentProductSkuId;
        private Integer customizationType;
        private Long productSkuId;
        private List<String> thumbUrlList;
        private Integer deliverQuantity;
        private Integer adviceQuantity;
        private String extCode;
        private Integer purchaseUpLimit;
        private Integer purchaseQuantity;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkuQuantityTotalInfo {
        private Integer realReceiveAuthenticQuantity;
        private Integer deliverQuantity;
        private Integer purchaseQuantity;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LackOrSoldOutTag {
        private Boolean isLack;
        private String skuDisplay;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkuLackItem {
        private String skuDisplay;
    }
}
