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
} 