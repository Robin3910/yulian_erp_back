package cn.iocoder.yudao.module.temu.service.shop;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopUpdateReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;

public interface TemuShopService {
	
	/**
	 * 创建店铺
	 *
	 * @param createReqVO 创建信息
	 * @return 店铺ID
	 */
	Long createShop(TemuShopCreateReqVO createReqVO);
	
	/**
	 * 更新店铺
	 *
	 * @param updateReqVO 更新信息
	 */
	void updateShop(TemuShopUpdateReqVO updateReqVO);
	
	/**
	 * 删除店铺
	 *
	 * @param id 编号
	 */
	void deleteShop(Long id);
	
	/**
	 * 获得店铺
	 *
	 * @param id 编号
	 * @return 店铺
	 */
	TemuShopRespVO getShop(Long id);
	
	/**
	 * 获得店铺分页
	 *
	 * @param pageReqVO 分页查询
	 * @return 店铺分页
	 */
	PageResult<TemuShopRespVO> getShopPage(TemuShopPageReqVO pageReqVO);
	
	/**
	 * 获得店铺分页 用户版
	 */
	PageResult<TemuShopRespVO> getShopPageForUser(TemuShopPageReqVO pageReqVO);
	
	/**
	 * 根据店铺ID获取店铺
	 *
	 * @param shopId 店铺ID
	 * @return 店铺
	 */
	TemuShopDO getShopByShopId(Long shopId);
} 