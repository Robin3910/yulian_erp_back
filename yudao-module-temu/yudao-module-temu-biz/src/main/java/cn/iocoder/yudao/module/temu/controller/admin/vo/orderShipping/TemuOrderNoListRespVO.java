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
}
