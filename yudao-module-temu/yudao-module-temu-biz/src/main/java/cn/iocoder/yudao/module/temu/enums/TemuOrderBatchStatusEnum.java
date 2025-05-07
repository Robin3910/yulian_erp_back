package cn.iocoder.yudao.module.temu.enums;

public interface TemuOrderBatchStatusEnum {
	//0待生产-> 1已生产
	int IN_PRODUCTION = 0;
	int PRODUCTION_COMPLETE = 1;
	
	//	任务派发状态 0未分配 1 已分配
	int NOT_DISPATCH_TASK = 0;
	int DISPATCH_TASK = 1;
	
	//任务类型	1 作图 2 生产
	int TASK_TYPE_ART = 1;
	int TASK_TYPE_PRODUCTION = 2;
	//任务状态 0已分配 1待处理  2已完成
	int TASK_STATUS_NOT_HANDLED = 0;
	int TASK_STATUS_WAIT = 1;
	int TASK_STATUS_COMPLETE = 2;

	
}
