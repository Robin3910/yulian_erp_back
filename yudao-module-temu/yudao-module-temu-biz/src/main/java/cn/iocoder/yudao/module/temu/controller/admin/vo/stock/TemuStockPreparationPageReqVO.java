package cn.iocoder.yudao.module.temu.controller.admin.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - Temu备货单分页查询 Request VO")
@Data
public class TemuStockPreparationPageReqVO {

    @Schema(description = "每页记录数", required = true, example = "1500")
    private Integer pageSize;

    @Schema(description = "页码", required = true, example = "1")
    private Integer pageNo;

    @Schema(description = "供应商ID列表")
    private List<Long> supplierIdList;

    @Schema(description = "商品SKC ID列表")
    private List<Long> productSkcIdList;

    @Schema(description = "备货单号列表")
    private List<String> subPurchaseOrderSnList;

    @Schema(description = "下单时间起始值", example = "1642608000")
    private Long purchaseTimeFrom;

    @Schema(description = "下单时间结束值", example = "1642694400")
    private Long purchaseTimeTo;

    @Schema(description = "订单状态列表", example = "[1,2,3]",
            allowableValues = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"})
    private List<Integer> statusList; // 0-待接单；1-已接单，待发货；2-已送货；3-已收货；4-已拒收；5-已验收，全部退回；6-已验收；7-已入库；8-作废；9-已超时
} 