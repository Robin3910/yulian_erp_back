package cn.iocoder.yudao.module.temu.controller.admin.vo.order;

import lombok.Data;
import java.util.List;

@Data
public class OrderGroupBySortingSequenceVO {
    private String sortingSequence;  // 分拣序号，支持下划线格式
    private String orderNo;        // 订单编号
    private String sku;            // SKU
    private Long bookingTime;      // 平台订单创建时间
    private List<OrderSkuPageItemVO> orders;
} 