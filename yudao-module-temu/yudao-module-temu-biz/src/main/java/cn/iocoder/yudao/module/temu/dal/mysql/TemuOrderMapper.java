package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.TemuOrderRequestVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.TemuOrderStatisticsRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;

import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDetailDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mapper
public interface TemuOrderMapper extends BaseMapperX<TemuOrderDO> {
	/**
	 * 构建查询条件
	 *
	 * @param temuOrderRequestVO 条件
	 * @return 查询条件
	 */
	default MPJLambdaWrapper<TemuOrderDO> builderOrderWrapper(TemuOrderRequestVO temuOrderRequestVO) {
		//连表分页查询
		MPJLambdaWrapper<TemuOrderDO> wrapper = new MPJLambdaWrapper<>();
		wrapper
				.leftJoin(TemuShopDO.class, TemuShopDO::getShopId, TemuOrderDO::getShopId)
				.leftJoin(TemuProductCategoryDO.class, TemuProductCategoryDO::getCategoryId, TemuOrderDO::getCategoryId)
				.selectAs(TemuProductCategoryDO::getUnitPrice, TemuOrderDetailDO::getCategoryPriceRule)
				.selectAs(TemuProductCategoryDO::getRuleType, TemuOrderDetailDO::getCategoryRuleType)
				.selectAs(TemuProductCategoryDO::getCategoryName, TemuOrderDetailDO::getProductCategoryName)
				.selectAs(TemuShopDO::getShopName, TemuOrderDetailDO::getShopName)
				.eqIfExists(TemuOrderDO::getOrderStatus, temuOrderRequestVO.getOrderStatus())// 订单状态
				.likeIfExists(TemuOrderDO::getSku, temuOrderRequestVO.getSku())// SKU
				.likeIfExists(TemuOrderDO::getSkc, temuOrderRequestVO.getSkc())// SKC
				.likeIfExists(TemuOrderDO::getCustomSku, temuOrderRequestVO.getCustomSku())// 定制SKU
				//.eqIfExists(TemuOrderDO::getCategoryId, temuOrderRequestVO.getCategoryId())// 分类ID
				.eqIfExists(TemuShopDO::getShopId, temuOrderRequestVO.getShopId());// 店铺ID
		//判断数组是否为空
		if (temuOrderRequestVO.getBookingTime() != null && temuOrderRequestVO.getBookingTime().length == 2) {
			wrapper.between(TemuOrderDO::getBookingTime, temuOrderRequestVO.getBookingTime()[0], temuOrderRequestVO.getBookingTime()[1]);
		}
		//按照订单时间倒序排列
		wrapper.orderByDesc(TemuOrderDO::getBookingTime);
		//分类ID查询
		if (temuOrderRequestVO.getCategoryId() != null && temuOrderRequestVO.getCategoryId().length > 0) {
			wrapper.in(TemuOrderDO::getCategoryId, Arrays.asList(temuOrderRequestVO.getCategoryId()));
		}
		//判断是否存在分类
		if (temuOrderRequestVO.getHasCategory() != null) {
			switch (temuOrderRequestVO.getHasCategory()) {
				case 0:
					wrapper.isNull(TemuOrderDO::getCategoryId);
					break;
				case 1:
					wrapper.isNotNull(TemuOrderDO::getCategoryId);
					break;
			}
		}
		return wrapper;
	}
	
	default List<TemuOrderDetailDO> selectListByOrderIds(List<Long> orderIds) {
		//连表分页查询
		MPJLambdaWrapper<TemuOrderDO> wrapper = new MPJLambdaWrapper<>();
		wrapper
				.leftJoin(TemuShopDO.class, TemuShopDO::getShopId, TemuOrderDO::getShopId)
				.leftJoin(TemuProductCategoryDO.class, TemuProductCategoryDO::getCategoryId, TemuOrderDO::getCategoryId)
				.selectAs(TemuProductCategoryDO::getUnitPrice, TemuOrderDetailDO::getCategoryPriceRule)
				//.selectAs(TemuProductCategoryDO::getDefaultPrice, TemuOrderDetailDO::getDefaultPrice)
				.selectAs(TemuProductCategoryDO::getCategoryName, TemuOrderDetailDO::getCategoryName)
				.selectAs(TemuShopDO::getShopName, TemuOrderDetailDO::getShopName)
				.selectAll(TemuOrderDO.class);
		if (orderIds == null || orderIds.isEmpty()) {
			return new ArrayList<>();
		}
		wrapper.in(TemuOrderDO::getId, orderIds);
		return selectJoinList(TemuOrderDetailDO.class, wrapper);
	}
	
	/**
	 * 根据条件查询订单信息
	 *
	 * @param temuOrderRequestVO 条件
	 * @return 订单信息
	 */
	default PageResult<TemuOrderDetailDO> selectPage(TemuOrderRequestVO temuOrderRequestVO) {
		MPJLambdaWrapper<TemuOrderDO> wrapper = builderOrderWrapper(temuOrderRequestVO);
		wrapper.selectAll(TemuOrderDO.class);
		return selectJoinPage(temuOrderRequestVO, TemuOrderDetailDO.class, wrapper);
	}
	
	default TemuOrderStatisticsRespVO statistics(TemuOrderRequestVO temuOrderRequestVO) {
		MPJLambdaWrapper<TemuOrderDO> wrapper = builderOrderWrapper(temuOrderRequestVO);
		wrapper.selectSum(TemuOrderDO::getTotalPrice, TemuOrderDO::getTotalPrice);
		TemuOrderDO temuOrderDO = selectOne(wrapper);
		
		TemuOrderStatisticsRespVO temuOrderStatisticsRespVO = new TemuOrderStatisticsRespVO();
		if (temuOrderDO != null) {
			temuOrderStatisticsRespVO.setTotalPrice(temuOrderDO.getTotalPrice());
		}
		return temuOrderStatisticsRespVO;
	}
	
	default TemuOrderStatisticsRespVO statistics(TemuOrderRequestVO temuOrderRequestVO, List<String> shopIds) {
		MPJLambdaWrapper<TemuOrderDO> wrapper = builderOrderWrapper(temuOrderRequestVO);
		wrapper.selectSum(TemuOrderDO::getTotalPrice, TemuOrderDO::getTotalPrice);
		wrapper.in(TemuOrderDO::getShopId, shopIds);
		TemuOrderDO temuOrderDO = selectOne(wrapper);
		TemuOrderStatisticsRespVO temuOrderStatisticsRespVO = new TemuOrderStatisticsRespVO();
		if (temuOrderDO != null) {
			temuOrderStatisticsRespVO.setTotalPrice(temuOrderDO.getTotalPrice());
		}
		return temuOrderStatisticsRespVO;
	}
	
	default PageResult<TemuOrderDetailDO> selectPage(TemuOrderRequestVO temuOrderRequestVO, List<String> shopIds) {
		MPJLambdaWrapper<TemuOrderDO> wrapper = builderOrderWrapper(temuOrderRequestVO);
		wrapper.selectAll(TemuOrderDO.class);
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
	
	/**
	 * 根据定制SKU查询所有订单
	 *
	 * @param customSku 定制SKU
	 * @return 订单信息
	 */
	default List<TemuOrderDO> selectListByCustomSku(List<String> customSku) {
		LambdaQueryWrapperX<TemuOrderDO> queryWrapperX = new LambdaQueryWrapperX<>();
		if (customSku == null || customSku.isEmpty()) {
			return new ArrayList<>();
		}
		queryWrapperX.in(TemuOrderDO::getCustomSku, customSku);
		return selectList(queryWrapperX);
	}
}
