package cn.iocoder.yudao.module.temu.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * Temu 模块的错误码枚举类
 */
public interface ErrorCodeConstants {
	//店铺相关
	ErrorCode SHOP_NOT_EXISTS = new ErrorCode(1_001_003_000, "店铺不存在");
	//合规单没有上传
	ErrorCode COMPLIANCE_NOT_UPLOAD = new ErrorCode(1_001_004_000, "合规单没有上传");
	// ========== 商品品类相关 1-001-000-000 ==========
	ErrorCode CATEGORY_NOT_EXISTS = new ErrorCode(1_001_000_000, "商品品类不存在");
	ErrorCode CATEGORY_SKU_NOT_EXISTS = new ErrorCode(1_001_001_002, "商品品类SKU关系不存在");
	ErrorCode CATEGORY_PRICE_NOT_EXISTS = new ErrorCode(1_001_001_003, "商品品类没有设置定价规则");
	
	// 订单相关
	ErrorCode ORDER_NOT_EXISTS = new ErrorCode(1_001_002_000, "订单不存在");
	ErrorCode ORDER_STATUS_ERROR = new ErrorCode(1_001_002_001, "当前订单状态下不允许操作");
	ErrorCode ORDER_BATCH_EXISTS = new ErrorCode(1_001_002_002, "当前订单已存在批次中");
	ErrorCode ORDER_BATCH_CREATE_FAIL = new ErrorCode(1_001_002_003, "订单批次创建失败");
	ErrorCode ORDER_BATCH_NOT_EXISTS = new ErrorCode(1_001_002_004, "订单批次不存在");
	ErrorCode ORDER_BATCH_STATUS_ERROR = new ErrorCode(1_001_002_005, "当前订单批次状态不允许操作");
	ErrorCode ORDER_BATCH_TASK_NOT_EXISTS = new ErrorCode(1_001_002_006, "订单批次任务不存在");
	ErrorCode ORDER_BATCH_TASK_NOT_OWNER = new ErrorCode(1_001_002_007, "不是任务的所有者无法处理当前任务");
	ErrorCode ORDER_BATCH_TASK_COMPLETE = new ErrorCode(1_001_002_008, "当前订单批次任务已经完成无法再次操作");
	ErrorCode ORDER_BATCH_TASK_STATUS_ERROR = new ErrorCode(1_001_002_001, "当前批次任务状态下不允许操作");
	
	
	ErrorCode WALLET_NOT_ENOUGH =  new ErrorCode(1_001_003_001, "钱包余额不足");
}