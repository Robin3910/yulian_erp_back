package cn.iocoder.yudao.module.temu.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * Temu 模块的错误码枚举类
 */
public interface ErrorCodeConstants {
	
	// ========== 商品品类相关 1-001-000-000 ==========
	ErrorCode CATEGORY_NOT_EXISTS = new ErrorCode(1_001_000_000, "商品品类不存在");
	ErrorCode CATEGORY_SKU_NOT_EXISTS = new ErrorCode(1_001_001_002, "商品品类SKU关系不存在");
	ErrorCode CATEGORY_PRICE_NOT_EXISTS = new ErrorCode(1_001_001_003, "商品品类没有设置定价规则");
	
	// 订单相关
	ErrorCode ORDER_NOT_EXISTS = new ErrorCode(1_001_002_000, "订单不存在");
	ErrorCode ORDER_STATUS_ERROR = new ErrorCode(1_001_002_001, "当前订单状态下不允许操作");
	
} 