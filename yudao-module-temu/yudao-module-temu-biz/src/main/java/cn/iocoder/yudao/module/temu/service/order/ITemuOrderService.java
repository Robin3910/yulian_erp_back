package cn.iocoder.yudao.module.temu.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.client.TemuOrderRequestVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;

import java.util.List;
import java.util.Map;

public interface ITemuOrderService {
	public PageResult<TemuOrderDO> list(TemuOrderRequestVO temuOrderRequestVO);

	/**
	 * 保存订单数据
	 * 
	 * @param shopId 店铺ID
	 * @param shopName 店铺名称
	 * @param ordersList 订单列表数据
	 * @param originalJson 原始JSON数据
	 * @return 保存的订单数量
	 */
	int saveOrders(String shopId, String shopName, List<Map<String, Object>> ordersList, String originalJson);
}
