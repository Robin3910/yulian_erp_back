package cn.iocoder.yudao.module.temu.service.orderStatistics;

import cn.iocoder.yudao.module.temu.controller.admin.vo.orderStatistics.OrderStatisticsReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderStatistics.OrderStatisticsRespVO;

public interface IOrderStatisticsService {
    OrderStatisticsRespVO getOrderStatistics(OrderStatisticsReqVO reqVO);
    OrderStatisticsRespVO getReturnOrderStatistics(OrderStatisticsReqVO reqVO);
}
