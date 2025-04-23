package cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping;

import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "temu管理 - 待发货列表 Response VO")
@Data
public class TemuOrderShippingRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "物流单号")
    private String trackingNumber;

    @Schema(description = "快递面单图片URL")
    private String expressImageUrl;

    @Schema(description = "快递面单外单图片URL")
    private String expressOutsideImageUrl;

    @Schema(description = "快递面SKU图片URL")
    private String expressSkuImageUrl;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "发货单创建时间")
    private LocalDateTime createTime;

    // 联表查询字段
    @Schema(description = "订单集合")
    private List<TemuOrderListRespVO> orderList;

    @Schema(description = "店铺名称")
    private String shopName;

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

    }
}
