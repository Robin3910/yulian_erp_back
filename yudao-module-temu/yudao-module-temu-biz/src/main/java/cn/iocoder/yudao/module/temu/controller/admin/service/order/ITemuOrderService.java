package cn.iocoder.yudao.module.temu.controller.admin.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.controller.vo.client.TemuOrderRequestVO;
import cn.iocoder.yudao.module.temu.controller.admin.dal.dataobject.TemuOrderDO;

import java.util.List;

public interface ITemuOrderService {
	public PageResult<TemuOrderDO> list(TemuOrderRequestVO temuOrderRequestVO);
}
