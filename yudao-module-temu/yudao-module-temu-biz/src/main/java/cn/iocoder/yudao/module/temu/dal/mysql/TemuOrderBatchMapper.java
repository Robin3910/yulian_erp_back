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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper
public interface TemuOrderBatchMapper extends BaseMapperX<TemuOrderBatchDO> {
	default PageResult<TemuOrderBatchDetailDO> selectPage(TemuOrderBatchPageVO pageVO) {
		// 第一步：查询满足条件的批次ID
		MPJLambdaWrapperX<TemuOrderBatchDO> batchWrapper = new MPJLambdaWrapperX<>();
		batchWrapper.select(TemuOrderBatchDO::getId)
				.leftJoin(TemuOrderBatchRelationDO.class, TemuOrderBatchRelationDO::getBatchId, TemuOrderBatchDO::getId)
				.leftJoin(TemuOrderDO.class, TemuOrderDO::getId, TemuOrderBatchRelationDO::getOrderId)
				.leftJoin(TemuOrderBatchTaskDO.class, TemuOrderBatchTaskDO::getBatchOrderId, TemuOrderBatchDO::getId)
				.leftJoin(AdminUserDO.class,  AdminUserDO::getId, TemuOrderBatchTaskDO::getUserId)
				.eqIfExists(TemuOrderBatchDO::getStatus, pageVO.getStatus())
				.eqIfExists(TemuOrderBatchDO::getIsDispatchTask, pageVO.getIsDispatchTask())
				.like(StringUtils.isNotEmpty(pageVO.getBatchNo()), TemuOrderBatchDO::getBatchNo, pageVO.getBatchNo())
				.like(StringUtils.isNotEmpty(pageVO.getCustomSku()), TemuOrderDO::getCustomSku, pageVO.getCustomSku());

		// 时间范围查询
		if (pageVO.getCreateTime() != null && pageVO.getCreateTime().length == 2) {
			batchWrapper.between(TemuOrderBatchDO::getCreateTime, pageVO.getCreateTime()[0], pageVO.getCreateTime()[1]);
		}

		// 按批次分组并按创建时间倒序
		batchWrapper.groupBy(TemuOrderBatchDO::getId)
				.orderByDesc(TemuOrderBatchDO::getCreateTime);

		// 执行分页查询获取批次ID
		PageResult<TemuOrderBatchDO> batchPageResult = selectJoinPage(pageVO, TemuOrderBatchDO.class, batchWrapper);
		if (batchPageResult.getList().isEmpty()) {
			return new PageResult<>(new ArrayList<>(), batchPageResult.getTotal(), pageVO.getPageNo(),
					pageVO.getPageSize());
		}

		// 第二步：查询批次详细信息和关联的所有订单
		List<Long> batchIds = batchPageResult.getList().stream()
				.map(TemuOrderBatchDO::getId)
				.collect(Collectors.toList());

		MPJLambdaWrapperX<TemuOrderBatchDO> detailWrapper = new MPJLambdaWrapperX<>();
		detailWrapper.selectAll(TemuOrderBatchDO.class)
				.leftJoin(TemuOrderBatchRelationDO.class, TemuOrderBatchRelationDO::getBatchId, TemuOrderBatchDO::getId)
				.leftJoin(TemuOrderDO.class, TemuOrderDO::getId, TemuOrderBatchRelationDO::getOrderId)
				.selectCollection(TemuOrderDO.class, TemuOrderBatchDetailDO::getOrderList)
				.in(TemuOrderBatchDO::getId, batchIds)
				.like(StringUtils.isNotEmpty(pageVO.getCustomSku()), TemuOrderDO::getCustomSku, pageVO.getCustomSku())
				.orderByDesc(TemuOrderDO::getCreateTime);

		// 查询批次详情（包含订单列表）
		List<TemuOrderBatchDetailDO> batchDetailList = selectJoinList(TemuOrderBatchDetailDO.class, detailWrapper);

		// 根据批次ID对结果进行分组
		Map<Long, List<TemuOrderBatchDetailDO>> batchDetailMap = batchDetailList.stream()
				.collect(Collectors.groupingBy(TemuOrderBatchDetailDO::getId));

		// 按原批次顺序组装最终结果
		List<TemuOrderBatchDetailDO> resultList = new ArrayList<>();
		for (TemuOrderBatchDO batch : batchPageResult.getList()) {
			List<TemuOrderBatchDetailDO> details = batchDetailMap.get(batch.getId());
			if (details != null && !details.isEmpty()) {
				// 合并同一批次下的所有订单
				TemuOrderBatchDetailDO mergedDetail = details.get(0);
				if (details.size() > 1) {
					List<TemuOrderDetailDO> allOrders = details.stream()
							.filter(d -> d.getOrderList() != null)
							.flatMap(d -> d.getOrderList().stream())
							.collect(Collectors.toList());
					mergedDetail.setOrderList(allOrders);
				}
				resultList.add(mergedDetail);
			} else {
				// 如果没有关联订单，也要保留批次信息
				TemuOrderBatchDetailDO emptyDetail = new TemuOrderBatchDetailDO();
				emptyDetail.setId(batch.getId());
				resultList.add(emptyDetail);
			}
		}

		// 返回最终分页结果
		return new PageResult<>(resultList, batchPageResult.getTotal(), pageVO.getPageNo(), pageVO.getPageSize());
	}

}
