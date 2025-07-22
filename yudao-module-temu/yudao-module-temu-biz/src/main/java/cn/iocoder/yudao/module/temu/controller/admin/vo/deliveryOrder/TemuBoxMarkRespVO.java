package cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "TemuApi - 物流面单查询 Response VO")
@Data
public class TemuBoxMarkRespVO {

    @Schema(description = "体积类型 10小 20大")
    private Integer volumeType;

    @Schema(description = "供应商id")
    private Integer supplierId;

    @Schema(description = "是否是定制品")
    private Boolean isCustomProduct;

    @Schema(description = "发货方式 1-自送 2-第三方物流")
    private Integer deliveryMethod;

    @Schema(description = "第X箱")
    private Integer packageIndex;

    @Schema(description = "非服饰类目主销售属性列表")
    private List<SpecVO> nonClothMainSpecVOList;

    @Schema(description = "物流单号")
    private String expressDeliverySn;

    @Schema(description = "货品名称")
    private String productName;

    @Schema(description = "收货仓库英文名称")
    private String subWarehouseEnglishName;

    @Schema(description = "是否是服饰类目")
    private Boolean isClothCat;

    @Schema(description = "是否首单")
    private Boolean isFirst;

    @Schema(description = "备货类型 0-普通备货 1-jit备货")
    private Integer purchaseStockType;

    @Schema(description = "总箱数")
    private Integer totalPackageNum;

    @Schema(description = "快递公司名称")
    private String expressCompany;

    @Schema(description = "skcId")
    private Integer productSkcId;

    @Schema(description = "非服饰类目sku货号")
    private String nonClothSkuExtCode;

    @Schema(description = "发货单号")
    private String deliveryOrderSn;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "结算类型 0-非vmi 1-vmi")
    private Integer settlementType;

    @Schema(description = "skc货号")
    private String skcExtCode;

    @Schema(description = "货品skuId列表")
    private List<Integer> productSkuIdList;

    @Schema(description = "发货时间(毫秒)")
    private Long deliverTime;

    @Schema(description = "收货仓库Id")
    private Integer subWarehouseId;

    @Schema(description = "是否是紧急发货单，0-普通 1-急采")
    private Integer urgencyType;

    @Schema(description = "skc名称")
    private String productSkcName;

    @Schema(description = "包裹号")
    private String packageSn;

    @Schema(description = "快递公司英文名称")
    private String expressEnglishCompany;

    @Schema(description = "包裹中件数")
    private Integer packageSkcNum;

    @Schema(description = "非服饰类目次销售属性列表")
    private List<SpecVO> nonClothSecondarySpecVOList;

    @Schema(description = "收货仓库名称")
    private String subWarehouseName;

    @Schema(description = "采购子单号")
    private String subPurchaseOrderSn;

    @Schema(description = "司机姓名")
    private String driverName;

    @Schema(description = "司机手机号")
    private String driverPhone;

    @Schema(description = "下单时间(毫秒)")
    private Long purchaseTime;

    @Schema(description = "灰度key命中情况")
    private Map<String, Boolean> greyKeyHitMap;

    @Schema(description = "存储属性名称")
    private String storageAttrName;

    @Schema(description = "发货件数")
    private Integer deliverSkcNum;

    @Schema(description = "发货状态")
    private Integer deliveryStatus;

    @Data
    public static class SpecVO {
        @Schema(description = "规格id")
        private Integer specId;

        @Schema(description = "父规格名称")
        private String parentSpecName;

        @Schema(description = "父规格id")
        private Integer parentSpecId;

        @Schema(description = "规格名称")
        private String specName;
    }
}