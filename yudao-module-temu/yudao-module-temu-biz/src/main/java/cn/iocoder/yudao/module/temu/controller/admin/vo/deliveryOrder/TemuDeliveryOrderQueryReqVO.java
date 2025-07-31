package cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.util.Set;

@Schema(description = "TemuApi - 发货列表 Request VO")
@Data
public class TemuDeliveryOrderQueryReqVO {
    @Schema(description = "skc列表")
    private List<Long> productSkcIdList;

    @Schema(description = "是否为定制品")
    private Boolean isCustomProduct;

    @Schema(description = "备货单号列表")
    private List<String> subPurchaseOrderSnList;

    @Schema(description = "物流单号列表")
    private Set<String> expressDeliverySnList;

    @Schema(description = "发货时间-开始")
    private Long deliverTimeFrom;
    @Schema(description = "发货时间-结束")
    private Long deliverTimeTo;

    @Schema(description = "店铺ID")
    private String shopId;

    private List<Integer> expressWeightFeedbackStatus;
    private Integer isPrintBoxMark;
    private String targetDeliveryAddress;
    private Boolean onlyTaxWarehouseWaitApply;
    private List<Long> subWarehouseIdList;

    private List<Integer> latestFeedbackStatusList;
    private Integer urgencyType;
    private String targetReceiveAddress;

    private List<String> skcExtCodeList;
    private List<String> deliveryOrderSnList;
    private List<Integer> inventoryRegion;
    private Integer isVmi;
    private Integer sortType;
    private Boolean isJit;
    private String sortFieldName;
    private Integer status;


    private Integer pageSize;
    private Integer pageNo;
}