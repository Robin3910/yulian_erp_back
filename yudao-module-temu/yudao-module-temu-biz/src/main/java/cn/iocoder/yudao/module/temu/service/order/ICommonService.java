package cn.iocoder.yudao.module.temu.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.mq.message.weixin.WeiXinNotifyMessage;

public interface ICommonService {
	PageResult<TemuProductCategoryDO> list();
	
	PageResult<TemuShopDO> listShop();
	
	PageResult<TemuShopDO> listShop(Long loginUserId);
	
	Object testTemuOpenApi();
	
	void doWeiXinNotifyMessage(WeiXinNotifyMessage message);
}
