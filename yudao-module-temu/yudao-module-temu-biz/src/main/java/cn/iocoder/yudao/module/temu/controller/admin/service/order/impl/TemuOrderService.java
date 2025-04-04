package cn.iocoder.yudao.module.temu.controller.admin.service.order.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.controller.vo.client.TemuOrderRequestVO;
import cn.iocoder.yudao.module.temu.controller.admin.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.controller.admin.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.controller.admin.service.order.ITemuOrderService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Service
public class TemuOrderService implements ITemuOrderService {
	@Resource
	private TemuOrderMapper temuOrderMapper;
	@Override
	public PageResult<TemuOrderDO> list(TemuOrderRequestVO temuOrderRequestVO) {
		return temuOrderMapper.selectPage(temuOrderRequestVO);
	}
}
