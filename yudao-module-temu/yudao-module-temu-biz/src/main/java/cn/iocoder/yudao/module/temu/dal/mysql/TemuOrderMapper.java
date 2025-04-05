package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.client.TemuOrderRequestVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TemuOrderMapper extends BaseMapperX<TemuOrderDO> {
	default PageResult<TemuOrderDO> selectPage(TemuOrderRequestVO temuOrderRequestVO) {
		LambdaQueryWrapperX<TemuOrderDO> queryWrapper = new LambdaQueryWrapperX<>();
		//店铺ID
		queryWrapper.likeIfPresent(TemuOrderDO::getShopId, temuOrderRequestVO.getShopId());
		queryWrapper.likeIfPresent(TemuOrderDO::getSku, temuOrderRequestVO.getSku());
		queryWrapper.likeIfPresent(TemuOrderDO::getSkc, temuOrderRequestVO.getSkc());
		queryWrapper.likeIfPresent(TemuOrderDO::getCustomSku, temuOrderRequestVO.getCustomSku());
		queryWrapper.likeIfPresent(TemuOrderDO::getOrderStatus, temuOrderRequestVO.getOrderStatus());
		queryWrapper.likeIfPresent(TemuOrderDO::getCategoryId, temuOrderRequestVO.getCategoryId());
		return selectPage(temuOrderRequestVO,queryWrapper);
	}
}
