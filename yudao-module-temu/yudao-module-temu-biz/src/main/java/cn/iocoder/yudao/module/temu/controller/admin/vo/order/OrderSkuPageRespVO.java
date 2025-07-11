package cn.iocoder.yudao.module.temu.controller.admin.vo.order;

import lombok.Data;
import java.util.List;

@Data
public class OrderSkuPageRespVO {
    private List<OrderGroupBySortingSequenceVO> list;
    private long total;
    private int pageNo;
    private int pageSize;
} 