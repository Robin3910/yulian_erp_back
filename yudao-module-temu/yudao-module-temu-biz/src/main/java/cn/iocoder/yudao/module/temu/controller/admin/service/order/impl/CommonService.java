package cn.iocoder.yudao.module.temu.controller.admin.service.order.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.dal.dataobject.TemuProductCateGoryDO;
import cn.iocoder.yudao.module.temu.controller.admin.dal.mysql.TemuProductCateGoryMapper;
import cn.iocoder.yudao.module.temu.controller.admin.service.order.ICommonService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
@Service
public class CommonService implements ICommonService {
	@Resource private TemuProductCateGoryMapper temuProductCateGoryMapper;
	@Override
	public PageResult<TemuProductCateGoryDO> list() {
		return temuProductCateGoryMapper.selectPage();
	}
}
