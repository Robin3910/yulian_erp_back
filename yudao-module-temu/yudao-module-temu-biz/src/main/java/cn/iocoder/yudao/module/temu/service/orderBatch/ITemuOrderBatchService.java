package cn.iocoder.yudao.module.temu.service.orderBatch;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchCreateVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchPageVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchUpdateFileVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchUpdateStatusVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderBatchDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderBatchDetailDO;

public interface ITemuOrderBatchService {
	Integer createBatch(TemuOrderBatchCreateVO temuOrderBatchCreateVO);
	PageResult<TemuOrderBatchDetailDO> list(TemuOrderBatchPageVO temuOrderBatchPageVO);
	int updateBatchFile(TemuOrderBatchUpdateFileVO temuOrderBatchUpdateFileVO);
	int updateStatus(TemuOrderBatchUpdateStatusVO temuOrderBatchUpdateStatusVO);
}
