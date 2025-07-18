package cn.iocoder.yudao.module.temu.service.orderBatch;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.*;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderBatchDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderBatchDetailDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderBatchUserDetailDO;

public interface ITemuOrderBatchService {
	Integer createBatch(TemuOrderBatchCreateVO temuOrderBatchCreateVO);
	
	PageResult<TemuOrderBatchDetailDO> list(TemuOrderBatchPageVO temuOrderBatchPageVO);
	
	PageResult<TemuOrderBatchUserDetailDO> taskList(TemuOrderBatchPageVO temuOrderBatchPageVO);
	
	int updateBatchFile(TemuOrderBatchUpdateFileVO temuOrderBatchUpdateFileVO);
	
	int updateBatchFileByTask(TemuOrderBatchUpdateFileByTaskVO temuOrderBatchUpdateFileVO);
	
	int updateStatus(TemuOrderBatchUpdateStatusVO temuOrderBatchUpdateStatusVO);
	
	int updateStatusByTask(TemuOrderBatchUpdateStatusByTaskVO temuOrderBatchUpdateStatusVO);
	
	Boolean saveOrderRemark(TemuOrderBatchSaveOrderRemarkReqVO requestVO);
	
	Boolean dispatchTask(TemuOrderBatchDispatchTaskVO requestVO);
	
	int completeBatchOrderTask(TemuOrderBatchCompleteOrderTaskVO requestVO);
	
	int completeBatchOrderTaskByAdmin(TemuOrderBatchCompleteOrderTaskByAdminVO requestVO);

	Boolean dispatchDrawProductTask(TemuOrderBatchDispatchTaskVO requestVO);

	
}
