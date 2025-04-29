package cn.iocoder.yudao.module.temu.enums.openapi;

public interface OrderInfoEnum {
	//0-待接单；1-已接单，待发货；2-已送货；3-已收货；4-已拒收；5-已验收，全部退回；6-已验收；7-已入库；8-作废；9-已超时
	
	/**
	 * 订单状态：待接单
	 */
	int STATUS_PENDING_ACCEPTANCE = 0;
	
	/**
	 * 订单状态：已接单，待发货
	 */
	int STATUS_ACCEPTED_PENDING_SHIPMENT = 1;
	
	/**
	 * 订单状态：已送货
	 */
	int STATUS_DELIVERED = 2;
	
	/**
	 * 订单状态：已收货
	 */
	int STATUS_RECEIVED = 3;
	
	/**
	 * 订单状态：已拒收
	 */
	int STATUS_REJECTED = 4;
	
	/**
	 * 订单状态：已验收，全部退回
	 */
	int STATUS_INSPECTED_RETURNED = 5;
	
	/**
	 * 订单状态：已验收
	 */
	int STATUS_INSPECTED = 6;
	
	/**
	 * 订单状态：已入库
	 */
	int STATUS_WAREHOUSED = 7;
	
	/**
	 * 订单状态：作废
	 */
	int STATUS_CANCELLED = 8;
	
	/**
	 * 订单状态：已超时
	 */
	int STATUS_TIMEOUT = 9;
}
