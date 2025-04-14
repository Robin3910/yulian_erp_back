package cn.iocoder.yudao.module.temu.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.TemuOrderBatchOrderReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.TemuOrderRequestVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.TemuOrderUpdateCategoryReqVo;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDetailDO;

import java.util.List;
import java.util.Map;

public interface ITemuOrderService {
	PageResult<TemuOrderDetailDO> list(TemuOrderRequestVO temuOrderRequestVO);
	PageResult<TemuOrderDetailDO> list(TemuOrderRequestVO temuOrderRequestVO,Long userId);
	Boolean beatchUpdateStatus(List<TemuOrderDO> requestVO);
	
	/**
	 * 保存订单数据
	 *
	 * @param shopId       店铺ID
	 * @param shopName     店铺名称
	 * @param ordersList   订单列表数据
	 * @param originalJson 原始JSON数据
	 * @return 保存的订单数量
	 */
	int saveOrders(String shopId, String shopName, List<Map<String, Object>> ordersList, String originalJson);
	
	/**
	 * 更新订单分类
	 *
	 * @param requestVO 订单分类更新请求
	 * @return 更新的记录数
	 */
	int updateCategory(TemuOrderUpdateCategoryReqVo requestVO);
	
	/**
	 * 批量下单
	 *
	 * @param requestVO 批量下单请求
	 * @return 批量下单结果
	 */
	int beatchSaveOrder( List<TemuOrderBatchOrderReqVO> requestVO);
}
