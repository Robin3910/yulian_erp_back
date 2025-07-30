package cn.iocoder.yudao.module.temu.controller.admin.vo.goods;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "TemuApi - 定制品商品条码查询 Request VO")
@Data
public class TemuCustomGoodsLabelQueryReqVO {

    @Schema(description = "店铺ID", required = true)
    private String shopId;

    @Schema(description = "货品sku id列表")
    private List<Long> productSkuIdList;

    @Schema(description = "货品skc id列表")
    private List<Long> productSkcIdList;

    @Schema(description = "定制sku id列表")
    private List<Long> personalProductSkuIdList;

    @Schema(description = "创建时间结束")
    private Integer createTimeEnd;

    @Schema(description = "页面大小")
    private Integer pageSize;

    @Schema(description = "页码")
    private Integer page;

    @Schema(description = "创建时间开始")
    private Integer createTimeStart;

    @Schema(description = "标签条码")
    private Long labelCode;
}