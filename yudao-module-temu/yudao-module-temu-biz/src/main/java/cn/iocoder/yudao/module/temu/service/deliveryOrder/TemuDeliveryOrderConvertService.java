package cn.iocoder.yudao.module.temu.service.deliveryOrder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderSimpleVO;
import com.fasterxml.jackson.databind.JsonNode;

public interface TemuDeliveryOrderConvertService {

    TemuDeliveryOrderSimpleVO convert(JsonNode item);

    PageResult<TemuDeliveryOrderSimpleVO> queryTemuLogisticsPage(TemuDeliveryOrderQueryReqVO reqVO);
}
