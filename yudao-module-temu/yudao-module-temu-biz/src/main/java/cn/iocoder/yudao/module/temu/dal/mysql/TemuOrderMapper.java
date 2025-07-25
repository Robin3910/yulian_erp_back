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
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
				.likeIfExists(TemuOrderDO::getOrderNo, temuOrderRequestVO.getOrderNo());//  订单编号
				//.eqIfExists(TemuOrderDO::getCategoryId, temuOrderRequestVO.getCategoryId())// 分类ID
				//.eqIfExists(TemuShopDO::getShopId, temuOrderRequestVO.getShopId());// 店铺ID

		// SKU列表查询
		if (temuOrderRequestVO.getSkuList() != null && !temuOrderRequestVO.getSkuList().isEmpty()) {
			wrapper.in(TemuOrderDO::getSku, temuOrderRequestVO.getSkuList());
		}

		// 定制SKU列表查询
    	if (temuOrderRequestVO.getCustomSkuList() != null && !temuOrderRequestVO.getCustomSkuList().isEmpty()) {
        	wrapper.in(TemuOrderDO::getCustomSku, temuOrderRequestVO.getCustomSkuList());
    	}
		//多店铺查询
		if (temuOrderRequestVO.getShopId() != null && temuOrderRequestVO.getShopId().length > 0) {
			wrapper.in(TemuOrderDO::getShopId, Arrays.asList(temuOrderRequestVO.getShopId()));
		}
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
		// 是否为返单查询
		if (temuOrderRequestVO.getIsReturnOrder() != null) {
			wrapper.eq(TemuOrderDO::getIsReturnOrder, temuOrderRequestVO.getIsReturnOrder());
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
//		// 联表temu_order_shipping_info，查daily_sequence
//		wrapper.leftJoin("temu_order_shipping_info shipping ON shipping.order_no = t.order_no");
//		wrapper.select("shipping.daily_sequence as dailySequence");
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
	 * @return 订单信息列表
	 */
	default List<TemuOrderDO> selectByCustomSku(String customSku) {
		LambdaQueryWrapperX<TemuOrderDO> queryWrapper = new LambdaQueryWrapperX<>();
		queryWrapper.eq(TemuOrderDO::getCustomSku, customSku);
		return selectList(queryWrapper);
	}

	/**
	 * 根据定制SKU和订单号查询订单
	 *
	 * @param customSku 定制SKU
	 * @param orderNo 订单号
	 * @return 订单信息
	 */
	default TemuOrderDO selectByCustomSkuAndOrderNo(String customSku, String orderNo) {
		LambdaQueryWrapperX<TemuOrderDO> queryWrapper = new LambdaQueryWrapperX<>();
		queryWrapper.eq(TemuOrderDO::getCustomSku, customSku)
				.eq(TemuOrderDO::getOrderNo, orderNo);
		return selectOne(queryWrapper);
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

	/**
	 * 根据定制SKU更新订单的goods_sn_no字段
	 *
	 * @param customSku 定制SKU
	 * @param goodsSnNo 条码值
	 * @return 更新的记录数
	 */
	@Update("UPDATE temu_order SET goods_sn_no = #{goodsSnNo} WHERE custom_sku = #{customSku}")
	int updateGoodsSnNoByCustomSku(@Param("customSku") String customSku, @Param("goodsSnNo") String goodsSnNo);

	/**
	 * 根据订单号查询订单
	 *
	 * @param orderNo 订单号
	 * @return 订单信息
	 */
	default TemuOrderDO selectByOrderNo(String orderNo) {
		LambdaQueryWrapperX<TemuOrderDO> queryWrapper = new LambdaQueryWrapperX<>();
		queryWrapper.eq(TemuOrderDO::getOrderNo, orderNo);
		return selectOne(queryWrapper);
	}

	/**
	 * 根据商品编号查询所有订单
	 *
	 * @param goodsSnNos 商品编号列表
	 * @return 订单信息列表
	 */
	default List<TemuOrderDO> selectListBygoodsSnNo(List<String> goodsSnNos) {
		LambdaQueryWrapperX<TemuOrderDO> queryWrapperX = new LambdaQueryWrapperX<>();
		if (goodsSnNos == null || goodsSnNos.isEmpty()) {
			return new ArrayList<>();
		}
		queryWrapperX.in(TemuOrderDO::getGoodsSnNo, goodsSnNos);
		return selectList(queryWrapperX);
	}
	
	/**
	 * 批量将订单的 isCompleteProducerTask 字段更新为 1
	 * @param ids 订单ID列表
	 * @return 更新条数
	 */
	@Update({
	    "<script>",
	    "UPDATE temu_order SET is_complete_producer_task = 1 WHERE id IN",
	    "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
	    "#{id}",
	    "</foreach>",
	    "</script>"
	})
	int updateIsCompleteProducerTaskBatch(@Param("ids") List<Long> ids);
	
	/**
	 * 根据订单编号查询所有订单
	 * @param orderNo 订单编号
	 * @return 订单列表
	 */
	default List<TemuOrderDO> selectListByOrderNo(String orderNo) {
		LambdaQueryWrapperX<TemuOrderDO> queryWrapper = new LambdaQueryWrapperX<>();
		queryWrapper.eq(TemuOrderDO::getOrderNo, orderNo);
		return selectList(queryWrapper);
	}

    /**
     * 查询某天所有订单（bookingTime在当天）
     * @param bookingDate 日期（LocalDate）
     * @return 订单列表
     */
    default List<TemuOrderDO> selectListByBookingDate(LocalDate bookingDate) {
        if (bookingDate == null) {
            return new ArrayList<>();
        }
        LocalDateTime start = bookingDate.atStartOfDay();
        LocalDateTime end = bookingDate.atTime(23, 59, 59);
        LambdaQueryWrapperX<TemuOrderDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.ge(TemuOrderDO::getBookingTime, start)
                    .le(TemuOrderDO::getBookingTime, end);
        return selectList(queryWrapper);
    }
}
