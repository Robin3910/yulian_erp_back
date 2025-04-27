package cn.iocoder.yudao.module.temu.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;

public interface ICommonService {
	PageResult<TemuProductCategoryDO> list();
	
	PageResult<TemuShopDO> listShop();
	
	PageResult<TemuShopDO> listShop(Long loginUserId);
	
	Object testTemuOpenApi();
}
