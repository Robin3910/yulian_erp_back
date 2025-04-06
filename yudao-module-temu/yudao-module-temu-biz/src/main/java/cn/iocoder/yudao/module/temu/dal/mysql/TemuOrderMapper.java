package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.client.TemuOrderRequestVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;

import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDetailDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;
import sun.reflect.generics.tree.ArrayTypeSignature;

@Mapper
public interface TemuOrderMapper extends BaseMapperX<TemuOrderDO> {
	default PageResult<TemuOrderDetailDO> selectPage(TemuOrderRequestVO temuOrderRequestVO) {
		//连表分页查询
		MPJLambdaWrapper<TemuOrderDO> wrapper = new MPJLambdaWrapper<>();
		wrapper.selectAll(TemuOrderDO.class)
				.leftJoin(TemuShopDO.class, TemuShopDO::getShopId, TemuOrderDO::getShopId)
				.selectAs(TemuShopDO::getShopName, TemuOrderDetailDO::getShopName)
				.eqIfExists(TemuOrderDO::getOrderStatus, temuOrderRequestVO.getOrderStatus())// 订单状态
				.likeIfExists(TemuOrderDO::getSku, temuOrderRequestVO.getSku())// SKU
				.likeIfExists(TemuOrderDO::getSkc, temuOrderRequestVO.getSkc())// SKC
				.likeIfExists(TemuOrderDO::getCustomSku, temuOrderRequestVO.getCustomSku())// 定制SKU
				.eqIfExists(TemuOrderDO::getCategoryId, temuOrderRequestVO.getCategoryId())// 分类ID
				.eqIfExists(TemuShopDO::getShopId, temuOrderRequestVO.getShopId());// 店铺ID
		//判断数组是否为空
		if (temuOrderRequestVO.getBookingTime() != null && temuOrderRequestVO.getBookingTime().length == 2) {
			wrapper.between(TemuOrderDO::getBookingTime, temuOrderRequestVO.getBookingTime()[0], temuOrderRequestVO.getBookingTime()[1]);
		}
		return selectJoinPage(temuOrderRequestVO, TemuOrderDetailDO.class, wrapper);
	}
	
	
	/**
	 * 根据定制SKU查询订单
	 *
	 * @param customSku 定制SKU
	 * @return 订单信息
	 */
	default TemuOrderDO selectByCustomSku(String customSku) {
		return selectOne(TemuOrderDO::getCustomSku, customSku);
	}
}
