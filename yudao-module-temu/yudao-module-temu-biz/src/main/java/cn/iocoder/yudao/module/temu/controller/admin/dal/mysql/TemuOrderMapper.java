package cn.iocoder.yudao.module.temu.controller.admin.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.controller.vo.client.TemuOrderRequestVO;
import cn.iocoder.yudao.module.temu.controller.admin.dal.dataobject.TemuOrderDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
