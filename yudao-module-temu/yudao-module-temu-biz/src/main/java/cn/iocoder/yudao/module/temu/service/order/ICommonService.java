package cn.iocoder.yudao.module.temu.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;

public interface ICommonService {
	public PageResult<TemuProductCategoryDO> list();
}
