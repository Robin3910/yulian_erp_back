package cn.iocoder.yudao.module.temu.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.*;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDetailDO;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

public interface ITemuOrderService {
	
	PageResult<TemuOrderDetailDO> list(TemuOrderRequestVO temuOrderRequestVO);
	
	TemuOrderStatisticsRespVO statistics(TemuOrderRequestVO temuOrderRequestVO);
	
	PageResult<TemuOrderDetailDO> list(TemuOrderRequestVO temuOrderRequestVO, Long userId);
	TemuOrderStatisticsRespVO statistics(TemuOrderRequestVO temuOrderRequestVO, Long userId);
	/**
	 * 批量修改订单状态
	 *
	 * @param requestVO 批量修改订单状态请求
	 * @return Boolean
	 */
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
	int batchSaveOrder(List<TemuOrderBatchOrderReqVO> requestVO);
	
	/**
	 * 根据订单ID获取合规单信息和商品码
	 *
	 * @param orderId 订单ID
	 * @return 合规单信息和商品码
	 */
	TemuOrderExtraInfoRespVO getOrderExtraInfo(String orderId);
	
	/**
	 * 保存订单备注
	 *
	 * @param requestVO 订单备注保存请求
	 * @return 是否保存成功
	 */
	Boolean saveOrderRemark(TemuOrderSaveOrderRemarkReqVO requestVO);
	
	/**
	 * 更新订单定制图片
	 *
	 * @param orderId 订单ID
	 * @param customImageUrls 定制图片URL列表，多个URL使用逗号分隔
	 * @return 更新是否成功
	 */
	Boolean updateOrderCustomImages(Long orderId, String customImageUrls);

	/**
	 * 批量更新订单状态
	 * @param reqVOList 订单列表，包含订单ID和新的状态
	 * @return 更新是否成功
	 */
	Boolean updateOrderStatus(List<TemuOrderDO> reqVOList);

    OrderSkuPageRespVO orderSkuPage(TemuOrderRequestVO req, Integer pageNo, Integer pageSize);

    /**
     * 切换或直接设置订单的isFoundAll字段
     * @param orderId 订单ID
     * @param isFoundAll 目标值（可为null，null时为切换，非null时为直接赋值）
     * @return 是否成功
     */
    Boolean toggleIsFoundAll(Long orderId, Integer isFoundAll);

    /**
     * 批量根据sortingSequence更新sender_id
     * @param sortingSequenceList 中包序号列表
     * @param senderId 发货人ID
     * @param conditionFlag 是否强制覆盖
     * @return 是否成功
     */
    Boolean batchUpdateSenderIdBySortingSequence(List<String> sortingSequenceList, Long senderId, Boolean conditionFlag);

	/**
	 * Vip账号批量下单
	 *
	 * @param requestVO 批量下单请求
	 * @return 批量下单结果
	 */
	int VipBatchSaveOrder(List<TemuOrderBatchOrderReqVO> requestVO);
}
