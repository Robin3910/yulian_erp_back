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
	ErrorCode ORDER_BATCH_TASK_TYPE_ERROR = new ErrorCode(1_001_002_009, "不存在的订单任务类型");
	ErrorCode ORDER_BATCH_TASK_DRAW_NOT_COMPLETE = new ErrorCode(1_001_002_010, "作图任务没有完成");
	
	
	ErrorCode WALLET_NOT_ENOUGH =  new ErrorCode(1_001_003_001, "钱包余额不足");

	ErrorCode PDF_PARSE_LIMIT_NOT_EXISTS = new ErrorCode(1002001000, "PDF解析限速配置不存在");

	// ========== 图像搜索相关 ==========
	ErrorCode TEMU_IMAGE_SIZE_EXCEEDS_LIMIT = new ErrorCode(1008001000, "图片大小超过限制");
	ErrorCode TEMU_IMAGE_TYPE_NOT_SUPPORTED = new ErrorCode(1008001001, "不支持的图片格式");

	// ========== 告警规则相关错误码 ==========
	ErrorCode ALERT_RULE_NOT_EXISTS = new ErrorCode(1005000000, "告警规则不存在");
}