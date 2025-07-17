package cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(description = "TemuApi - 发货列表 Response VO")
@Data
public class TemuDeliveryOrderSimpleVO {
    private String deliveryOrderSn;         // 发货单号
    private String expressDeliverySn;       // 快递单号
    private String expressCompany;          // 快递公司名称
    private Long expressCompanyId;          // 快递公司id
    private Long supplierId;                // 供应商id
    private String subPurchaseOrderSn;      // 采购子单号
    private Integer urgencyType;            // 是否紧急
    private Integer deliverPackageNum;      // 实发包裹数
    private Integer receivePackageNum;      // 实收包裹数
    private Integer predictTotalPackageWeight; // 预估总包裹重量
    private Integer status;                 // 状态
    private Long deliverTime;               // 发货时间
    private Long receiveTime;               // 收货时间
    private String subWarehouseName;        // 子仓名称
    private List<PackageVO> packageList;    // 包裹列表
    private List<PackageDetailVO> packageDetailList; // 包裹详情
    private ReceiveAddressInfoVO receiveAddressInfo; // 收货地址

    private Long purchaseTime; // 需在时间前 发货
    private Long expectPickUpGoodsTime; //预约取货时间
    private String productSkcPicture; //货品图片
    private String productSkcId; //skc
    private String expressBatchSn; //发货批次
    private Integer purchaseQuantity; //发货数量




    @Data
    public static class PackageVO {
        private Integer skcNum;
        private String packageSn;
    }

    @Data
    public static class PackageDetailVO {
        private Long productSkuId;
        private Integer skuNum;
    }

    @Data
    public static class ReceiveAddressInfoVO {
        private String receiverName;
        private String phone;
        private String provinceName;
        private String cityName;
        private String districtName;
        private String detailAddress;
    }


}