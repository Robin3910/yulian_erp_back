package cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping;

import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - Temu订单物流 Response VO")
@Data
public class TemuOrderShippingRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "订单号列表")
    private List<TemuOrderNoListRespVO> orderNoList;

    @Schema(description = "物流单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String trackingNumber;

    @Schema(description = "店铺id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long shopId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "发货状态（0：未发货；1：已发货）")
    private Integer shippingStatus;

    @Schema(description = "是否加急")
    private Boolean isUrgent;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "物流单的序号")
    private Integer dailySequence;

    @Schema(description = "Temu管理 - 待发货列表保存请求 VO")
    @Data
    public static class TemuOrderShippingSaveRequestVO {

        @Schema(description = "订单编号", required = true)
        private String orderNo;

        @Schema(description = "物流单号")
        private String trackingNumber;

        @Schema(description = "快递面单图片URL", required = true)
        private String expressImageUrl;

        @Schema(description = "快递面单外单图片URL", required = true)
        private String expressOutsideImageUrl;

        @Schema(description = "快递面单SKU图片URL", required = true)
        private String expressSkuImageUrl;

        @Schema(description = "店铺id", required = true)
        private Long shopId;

        @Schema(description = "物流发货时间")
        private String shippingTime;

        @Schema(description = "是否加急", required = true)
        private Boolean isUrgent;
    }
}
