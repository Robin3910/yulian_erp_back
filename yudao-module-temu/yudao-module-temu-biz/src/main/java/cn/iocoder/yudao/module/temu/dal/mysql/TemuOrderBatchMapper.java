package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchPageVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.*;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;

import javax.annotation.Resource;

@Mapper
public interface TemuOrderBatchMapper extends BaseMapperX<TemuOrderBatchDO> {
	Logger log = LoggerFactory.getLogger(TemuOrderBatchMapper.class);
	
	default PageResult<TemuOrderBatchDO> selectBatchPage(TemuOrderBatchPageVO pageVO) {
		MPJLambdaWrapperX<TemuOrderBatchDO> batchWrapper = new MPJLambdaWrapperX<>();
		batchWrapper.select(TemuOrderBatchDO::getId)
				.leftJoin(TemuOrderBatchRelationDO.class, TemuOrderBatchRelationDO::getBatchId, TemuOrderBatchDO::getId)
				.leftJoin(TemuOrderDO.class, TemuOrderDO::getId, TemuOrderBatchRelationDO::getOrderId)
				.leftJoin(TemuOrderBatchTaskDO.class, TemuOrderBatchTaskDO::getBatchOrderId, TemuOrderBatchDO::getId)
				.leftJoin(AdminUserDO.class, AdminUserDO::getId, TemuOrderBatchTaskDO::getUserId)
				.eqIfExists(TemuOrderBatchDO::getStatus, pageVO.getStatus())
				.eqIfExists(TemuOrderBatchDO::getIsDispatchTask, pageVO.getIsDispatchTask())
				.eqIfExists(TemuOrderDO::getShopId, pageVO.getShopId())
				.like(StringUtils.isNotEmpty(pageVO.getBatchNo()), TemuOrderBatchDO::getBatchNo, pageVO.getBatchNo())
				.like(StringUtils.isNotEmpty(pageVO.getCustomSku()), TemuOrderDO::getCustomSku, pageVO.getCustomSku())
				.like(StringUtils.isNotEmpty(pageVO.getOrderNo()), TemuOrderDO::getOrderNo, pageVO.getOrderNo());
		
		// 时间范围查询
		if (pageVO.getCreateTime() != null && pageVO.getCreateTime().length == 2) {
			batchWrapper.between(TemuOrderBatchDO::getCreateTime, pageVO.getCreateTime()[0], pageVO.getCreateTime()[1]);
		}
		// 根据订单类目查询数据
		if (pageVO.getCategoryId()!=null&&!pageVO.getCategoryId().isEmpty()){
			batchWrapper.in(TemuOrderDO::getCategoryId, pageVO.getCategoryId());
		}
		// 按批次分组并按创建时间倒序
		batchWrapper.groupBy(TemuOrderBatchDO::getId)
				.orderByDesc(TemuOrderBatchDO::getCreateTime);
		
		return selectJoinPage(pageVO, TemuOrderBatchDO.class, batchWrapper);
	}
	
	default List<TemuOrderBatchDO> selectBatchList(List<Long> batchIds) {
		MPJLambdaWrapperX<TemuOrderBatchDO> batchDetailWrapper = new MPJLambdaWrapperX<>();
		batchDetailWrapper.selectAll(TemuOrderBatchDO.class)
				.in(TemuOrderBatchDO::getId, batchIds)
				.orderByDesc(TemuOrderBatchDO::getCreateTime);
		
		return selectJoinList(TemuOrderBatchDO.class, batchDetailWrapper);
	}

	/**
	 * 根据分类ID查询当天最新的批次
	 *
	 * @param batchCategoryId 批次分类ID
	 * @param startTime       开始时间
	 * @param endTime         结束时间
	 * @return 最新的批次信息
	 */
	
	TemuOrderBatchDO selectLatestBatchByCategoryId(@Param("batchCategoryId") String batchCategoryId,
												   @Param("startTime") LocalDateTime startTime,
												   @Param("endTime") LocalDateTime endTime);
	//查询当天有多少订单
	List<TemuOrderBatchDO> selectByCreateTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

