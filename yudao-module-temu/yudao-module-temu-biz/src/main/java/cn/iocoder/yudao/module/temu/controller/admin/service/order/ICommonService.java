package cn.iocoder.yudao.module.temu.controller.admin.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.dal.dataobject.TemuProductCateGoryDO;

public interface ICommonService {
	public PageResult<TemuProductCateGoryDO> list();
}
