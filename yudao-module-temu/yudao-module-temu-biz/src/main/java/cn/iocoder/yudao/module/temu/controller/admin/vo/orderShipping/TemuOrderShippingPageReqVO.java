package cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Schema(description = "Temu管理 - 待发货列表分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemuOrderShippingPageReqVO extends PageParam {

    @Schema(description = "订单ID", example = "1024")
    private Long orderId;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "物流单号")
    private String trackingNumber;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "订单状态", example = "3")
    private Integer orderStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "创建时间", example = "2024-01-01", title = "查询的时间范围")
    private LocalDate[] createTime;

}

