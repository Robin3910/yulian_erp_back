package cn.iocoder.yudao.module.temu.service.order.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuProductCategoryMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopMapper;
import cn.iocoder.yudao.module.temu.service.order.ICommonService;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
@Service
public class CommonService implements ICommonService {
	@Resource private TemuProductCategoryMapper temuProductCategoryMapper;
	@Resource private TemuShopMapper temuShopMapper;
	@Override
	public PageResult<TemuProductCategoryDO> list() {
		return temuProductCategoryMapper.selectPage();
	}
	
	@Override
	public PageResult<TemuShopDO> listShop() {
		return temuShopMapper.selectPage();
	}
}
