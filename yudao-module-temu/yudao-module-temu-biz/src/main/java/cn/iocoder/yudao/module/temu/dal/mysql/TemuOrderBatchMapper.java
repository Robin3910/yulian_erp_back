package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchPageVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.*;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface TemuOrderBatchMapper extends BaseMapperX<TemuOrderBatchDO> {
	default PageResult<TemuOrderBatchDetailDO> selectPage(TemuOrderBatchPageVO temuOrderBatchPageVO) {
		MPJLambdaWrapperX<TemuOrderBatchDO> wrapper = new MPJLambdaWrapperX<>();
		wrapper.selectAll(TemuOrderBatchDO.class)
				.leftJoin(TemuOrderBatchRelationDO.class, TemuOrderBatchRelationDO::getBatchId, TemuOrderBatchDO::getId)
				.leftJoin(TemuOrderDO.class, TemuOrderDO::getId, TemuOrderBatchRelationDO::getOrderId)
				.selectCollection(TemuOrderDO.class, TemuOrderBatchDetailDO::getOrderList)
				.eqIfExists(TemuOrderBatchDO::getStatus, temuOrderBatchPageVO.getStatus())
				.likeIfExists(TemuOrderDO::getOrderNo, temuOrderBatchPageVO.getBatchNo());
		  
		
		//判断数组是否为空
		if (temuOrderBatchPageVO.getCreateTime() != null && temuOrderBatchPageVO.getCreateTime().length==2) {
			wrapper.between(TemuOrderDO::getCreateTime, temuOrderBatchPageVO.getCreateTime()[0], temuOrderBatchPageVO.getCreateTime()[1]);
		}
		//按照订单时间倒序排列
		wrapper.orderByDesc(TemuOrderDO::getCreateTime);
		
		return selectJoinPage(temuOrderBatchPageVO, TemuOrderBatchDetailDO.class, wrapper);
	}
}
