package cn.iocoder.yudao.module.temu.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.user.UserSimpleRespVo;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.mq.message.weixin.WeiXinNotifyMessage;

import java.util.List;

public interface ICommonService {
	PageResult<TemuProductCategoryDO> list();
	
	PageResult<TemuShopDO> listShop();
	
	PageResult<TemuShopDO> listShop(Long loginUserId);
	
	List<UserSimpleRespVo> getUserByRoleCode(String roleCode);
	
	Object testTemuOpenApi();
	
	void doWeiXinNotifyMessage(WeiXinNotifyMessage message);
}
