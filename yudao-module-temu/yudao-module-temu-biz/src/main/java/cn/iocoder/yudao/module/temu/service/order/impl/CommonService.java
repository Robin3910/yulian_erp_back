package cn.iocoder.yudao.module.temu.service.order.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuProductCategoryMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopMapper;
import cn.iocoder.yudao.module.temu.service.order.ICommonService;

import cn.iocoder.yudao.module.temu.utils.openapi.TemuOpenApiBuilder;
import cn.iocoder.yudao.module.temu.utils.openapi.TemuOpenApiUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.TreeMap;

@Service
public class CommonService implements ICommonService {
	
	@Resource
	private TemuProductCategoryMapper temuProductCategoryMapper;
	@Resource
	private TemuShopMapper temuShopMapper;
	@Resource
	private TemuOpenApiBuilder temuOpenApiBuilder;
	
	@Override
	public PageResult<TemuProductCategoryDO> list() {
		return temuProductCategoryMapper.selectPage();
	}
	
	@Override
	public PageResult<TemuShopDO> listShop() {
		return temuShopMapper.selectPage();
	}
	
	@Override
	public PageResult<TemuShopDO> listShop(Long loginUserId) {
		return temuShopMapper.selectPage(loginUserId);
	}
	
	@Override
	public Object testTemuOpenApi() {
		TemuShopDO temuShopDO = temuShopMapper.selectById(25);
		TreeMap<String, Object> map = new TreeMap<>();
		map.put("pageNo", 1);
		map.put("pageSize", 10);
		return temuOpenApiBuilder.builder(temuShopDO.getAccessToken()).getOrderInfo(map);
	}
	
}
