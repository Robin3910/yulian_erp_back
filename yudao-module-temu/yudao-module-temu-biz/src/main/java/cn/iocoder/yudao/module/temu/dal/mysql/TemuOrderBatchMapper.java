package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchPageVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.*;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.commons.lang3.StringUtils;

@Mapper
public interface TemuOrderBatchMapper extends BaseMapperX<TemuOrderBatchDO> {
	default PageResult<TemuOrderBatchDetailDO> selectPage(TemuOrderBatchPageVO temuOrderBatchPageVO) {
		MPJLambdaWrapperX<TemuOrderBatchDO> wrapper = new MPJLambdaWrapperX<>();

		// 基础查询
		wrapper.selectAll(TemuOrderBatchDO.class)
				.leftJoin(TemuOrderBatchRelationDO.class, TemuOrderBatchRelationDO::getBatchId, TemuOrderBatchDO::getId)
				.leftJoin(TemuOrderDO.class, TemuOrderDO::getId, TemuOrderBatchRelationDO::getOrderId)
				.selectCollection(TemuOrderDO.class, TemuOrderBatchDetailDO::getOrderList)
				.eqIfExists(TemuOrderBatchDO::getStatus, temuOrderBatchPageVO.getStatus())
				.like(StringUtils.isNotEmpty(temuOrderBatchPageVO.getBatchNo()), TemuOrderBatchDO::getBatchNo,
						temuOrderBatchPageVO.getBatchNo())
				.like(StringUtils.isNotEmpty(temuOrderBatchPageVO.getCustomSku()), TemuOrderDO::getCustomSku,
						temuOrderBatchPageVO.getCustomSku());

		// 时间范围查询
		if (temuOrderBatchPageVO.getCreateTime() != null && temuOrderBatchPageVO.getCreateTime().length == 2) {
			wrapper.between(TemuOrderBatchDO::getCreateTime, temuOrderBatchPageVO.getCreateTime()[0],
					temuOrderBatchPageVO.getCreateTime()[1]);
		}

		// 是否按批次分组
		if (Boolean.TRUE.equals(temuOrderBatchPageVO.getGroupByBatch())) {
			wrapper.groupBy(TemuOrderBatchDO::getBatchNo);
		}

		// 按照订单时间倒序排列
		wrapper.orderByDesc(TemuOrderDO::getCreateTime);

		// 执行分页查询
		PageResult<TemuOrderBatchDetailDO> pageResult = selectJoinPage(temuOrderBatchPageVO,
				TemuOrderBatchDetailDO.class, wrapper);
		// 设置分页参数
		pageResult.setPageNo(temuOrderBatchPageVO.getPageNo());
		pageResult.setPageSize(temuOrderBatchPageVO.getPageSize());

		return pageResult;
	}
}
