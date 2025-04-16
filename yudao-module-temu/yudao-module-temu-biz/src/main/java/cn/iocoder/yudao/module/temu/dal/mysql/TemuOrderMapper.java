package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.TemuOrderRequestVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;

import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDetailDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TemuOrderMapper extends BaseMapperX<TemuOrderDO> {
	default MPJLambdaWrapper<TemuOrderDO> buliderOrderWapper(TemuOrderRequestVO temuOrderRequestVO) {
		//连表分页查询
		MPJLambdaWrapper<TemuOrderDO> wrapper = new MPJLambdaWrapper<>();
		wrapper.selectAll(TemuOrderDO.class)
				.leftJoin(TemuShopDO.class, TemuShopDO::getShopId, TemuOrderDO::getShopId)
				.leftJoin(TemuProductCategoryDO.class, TemuProductCategoryDO::getCategoryId, TemuOrderDO::getCategoryId)
				.selectAs(TemuProductCategoryDO::getUnitPrice, TemuOrderDetailDO::getCategoryPriceRule)
				.selectAs(TemuProductCategoryDO::getDefaultPrice, TemuOrderDetailDO::getDefaultPrice)
				.selectAs(TemuProductCategoryDO::getCategoryName, TemuOrderDetailDO::getCategoryName)
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
		//按照订单时间倒序排列
		wrapper.orderByDesc(TemuOrderDO::getBookingTime);
		return wrapper;
	}
	
	/**
	 * 根据条件查询订单信息
	 *
	 * @param temuOrderRequestVO 条件
	 * @return 订单信息
	 */
	default PageResult<TemuOrderDetailDO> selectPage(TemuOrderRequestVO temuOrderRequestVO) {
		return selectJoinPage(temuOrderRequestVO, TemuOrderDetailDO.class, buliderOrderWapper(temuOrderRequestVO));
	}
	
	default PageResult<TemuOrderDetailDO> selectPage(TemuOrderRequestVO temuOrderRequestVO, List<String> shopIds) {
		MPJLambdaWrapper<TemuOrderDO> wrapper = buliderOrderWapper(temuOrderRequestVO);
		wrapper.in(TemuOrderDO::getShopId, shopIds);
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
