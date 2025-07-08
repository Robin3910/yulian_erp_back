package cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch;

import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 图片搜索结果 Response VO")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemuImageSearchOrderRespVO extends TemuOrderDO {

    @Schema(description = "定制sku", required = true, example = "51978533533490")
    private String productId;

    @Schema(description = "相似度得分", required = true, example = "0.98")
    private Float score;

    //店铺名称
    private String shopName;

    //店铺别名
    private String aliasName;

    //物流单号
    private String trackingNumber;

    //加急单URL
    private String expressImageUrl;

    //面单URL
    private String expressOutsideImageUrl;

    //条码URL
    private String expressSkuImageUrl;

    //物流单序号
    private Integer dailySequence;

    //物流单发货时间
    private LocalDateTime shippingTime;

    //分拣中包序号
    private Integer sortingSequence;


}
