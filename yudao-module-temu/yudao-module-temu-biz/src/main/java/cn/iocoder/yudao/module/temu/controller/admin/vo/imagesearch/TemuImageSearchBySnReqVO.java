package cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 根据条码编号或定制SKU查询订单 Request VO")
@Data
public class TemuImageSearchBySnReqVO {

    @Schema(description = "商品编号", example = "123456789")
    private String goodsSnNo;

    @Schema(description = "自定义SKU", example = "CUSTOM_SKU_001")
    private String customSku;

    /**
     * 校验参数：goodsSnNo和customSku至少需要提供一个
     */
    public boolean isValid() {
        return (goodsSnNo != null && !goodsSnNo.trim().isEmpty()) || 
               (customSku != null && !customSku.trim().isEmpty());
    }
} 