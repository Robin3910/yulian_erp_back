package cn.iocoder.yudao.module.temu.controller.admin.vo.order;

import lombok.Data;

@Data
public class OrderSkuPageItemVO {
    private String customSku;      // 定制SKU
    private Integer quantity;      // 数量
    private Integer originalQuantity; // 官网数量
    private String categoryId;     // 分类ID
    private String categoryName;   // 分类名称
    private String effectiveImgUrl; // 合成预览图
} 