package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.workertask.TemuWorkerTaskPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.workertask.TemuWorkerTaskRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuWorkerTaskDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


/**
 * 工作人员任务记录 Mapper
 *
 * @author wujunlin
 */
@Mapper
public interface TemuWorkerTaskMapper extends BaseMapperX<TemuWorkerTaskDO> {

    default PageResult<TemuWorkerTaskRespVO> selectPage(TemuWorkerTaskPageReqVO reqVO) {
        MPJLambdaWrapper<TemuWorkerTaskDO> wrapper = new MPJLambdaWrapper<TemuWorkerTaskDO>()
            .selectAll(TemuWorkerTaskDO.class)
            .selectAs(TemuShopDO::getShopName, TemuWorkerTaskRespVO::getShopName)
            .leftJoin(TemuShopDO.class, TemuShopDO::getShopId, TemuWorkerTaskDO::getShopId)
            .orderByDesc(TemuWorkerTaskDO::getId);

        if (reqVO.getWorkerId() != null) wrapper.eq(TemuWorkerTaskDO::getWorkerId, reqVO.getWorkerId());
        if (reqVO.getWorkerName() != null && !reqVO.getWorkerName().isEmpty()) wrapper.like(TemuWorkerTaskDO::getWorkerName, reqVO.getWorkerName());
        if (reqVO.getTaskType() != null) wrapper.eq(TemuWorkerTaskDO::getTaskType, reqVO.getTaskType());
        if (reqVO.getTaskStatus() != null) wrapper.eq(TemuWorkerTaskDO::getTaskStatus, reqVO.getTaskStatus());
        if (reqVO.getOrderId() != null) wrapper.eq(TemuWorkerTaskDO::getOrderId, reqVO.getOrderId());
        if (reqVO.getOrderNo() != null && !reqVO.getOrderNo().isEmpty()) wrapper.eq(TemuWorkerTaskDO::getOrderNo, reqVO.getOrderNo());
        if (reqVO.getBatchOrderId() != null) wrapper.eq(TemuWorkerTaskDO::getBatchOrderId, reqVO.getBatchOrderId());
        if (reqVO.getCustomSku() != null && !reqVO.getCustomSku().isEmpty()) wrapper.like(TemuWorkerTaskDO::getCustomSku, reqVO.getCustomSku());
        if (reqVO.getSkuQuantity() != null) wrapper.eq(TemuWorkerTaskDO::getSkuQuantity, reqVO.getSkuQuantity());
        if (reqVO.getShopId() != null) wrapper.eq(TemuWorkerTaskDO::getShopId, reqVO.getShopId());
        if (reqVO.getShopName() != null && !reqVO.getShopName().isEmpty()) wrapper.like(TemuShopDO::getShopName, reqVO.getShopName());
        if (reqVO.getTaskCompleteTime() != null && reqVO.getTaskCompleteTime().length == 2)
            wrapper.between(TemuWorkerTaskDO::getTaskCompleteTime, reqVO.getTaskCompleteTime()[0], reqVO.getTaskCompleteTime()[1]);
        if (reqVO.getCreateTime() != null && reqVO.getCreateTime().length == 2)
            wrapper.between(TemuWorkerTaskDO::getCreateTime, reqVO.getCreateTime()[0], reqVO.getCreateTime()[1]);

        return selectJoinPage(reqVO, TemuWorkerTaskRespVO.class, wrapper);
    }

    /**
     * 查询某个用户已处理过的不同 custom_sku 数量
     */
    @Select("SELECT COUNT(DISTINCT custom_sku) FROM temu_worker_task WHERE worker_id = #{workerId}")
    int selectDistinctCustomSkuCountByWorkerId(@Param("workerId") Long workerId);

    /**
     * 判断某个用户是否已处理过某个 custom_sku
     */
    @Select("SELECT COUNT(1) FROM temu_worker_task WHERE worker_id = #{workerId} AND custom_sku = #{customSku}")
    boolean existsByWorkerIdAndCustomSku(@Param("workerId") Long workerId, @Param("customSku") String customSku);

    /**
     * 根据订单编号、定制SKU和任务类型查询最新的作图任务记录
     */
    @Select("SELECT * FROM temu_worker_task WHERE order_no = #{orderNo} AND custom_sku = #{customSku} AND task_type = #{taskType} ORDER BY create_time DESC LIMIT 1")
    TemuWorkerTaskDO selectLatestDrawTaskByOrderNoAndCustomSku(@Param("orderNo") String orderNo, @Param("customSku") String customSku, @Param("taskType") Byte taskType);

}