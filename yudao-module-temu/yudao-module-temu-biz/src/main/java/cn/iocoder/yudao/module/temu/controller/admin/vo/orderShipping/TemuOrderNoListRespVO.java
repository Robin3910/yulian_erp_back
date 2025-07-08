package cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - Temu订单号及其订单列表 Response VO")
@Data
public class TemuOrderNoListRespVO {

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "订单列表")
    private List<TemuOrderListRespVO> orderList;

    @Schema(description = "快递面单图片")
    private String expressImageUrl;

    @Schema(description = "快递外包装图片")
    private String expressOutsideImageUrl;

    @Schema(description = "快递商品图片")
    private String expressSkuImageUrl;

    @Schema(description = "分拣序号，用于标识订单分拣的顺序")
    private Integer sortingSequence;
}
