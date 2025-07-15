package cn.iocoder.yudao.module.temu.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuOpenapiShopPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuOpenapiShopPageRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.user.UserSimpleRespVo;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOpenapiShopDO;
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
	
	void saveTemuOpenapiShop(TemuOpenapiShopDO shopDO);
	
	/**
	 * 分页查询Temu OpenAPI店铺列表
	 *
	 * @param reqVO 查询条件
	 * @return 分页结果
	 */
	PageResult<TemuOpenapiShopPageRespVO> getTemuOpenapiShopPage(TemuOpenapiShopPageReqVO reqVO);
}
