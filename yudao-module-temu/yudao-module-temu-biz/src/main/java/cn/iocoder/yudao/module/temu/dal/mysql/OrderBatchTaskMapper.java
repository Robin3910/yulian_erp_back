package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderBatch.TemuOrderBatchPageVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Mapper;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 订单批次任务分配 Mapper
 *
 * @author 禹链科技
 */
@Mapper
public interface OrderBatchTaskMapper extends BaseMapperX<TemuOrderBatchTaskDO> {

}