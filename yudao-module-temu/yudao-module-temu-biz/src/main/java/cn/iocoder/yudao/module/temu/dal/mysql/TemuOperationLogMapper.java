package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.PageUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.UserRechargeRecordPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.OrderPlacementRecordPageReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOperationLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作日志 Mapper
 *
 * @author 禹链科技
 */
@Mapper
public interface TemuOperationLogMapper extends BaseMapperX<TemuOperationLogDO> {

    default PageResult<TemuOperationLogDO> selectPage(TemuOperationLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TemuOperationLogDO>()
                .likeIfPresent(TemuOperationLogDO::getUserName, reqVO.getUserName())
                .likeIfPresent(TemuOperationLogDO::getUserId, reqVO.getUserId())
                .betweenIfPresent(TemuOperationLogDO::getOperationTime, reqVO.getOperationTime())
                .likeIfPresent(TemuOperationLogDO::getModule, reqVO.getModule())
                .likeIfPresent(TemuOperationLogDO::getOperationType, reqVO.getOperationType())
                .likeIfPresent(TemuOperationLogDO::getIpAddress, reqVO.getIpAddress())
                .likeIfPresent(TemuOperationLogDO::getClassName, reqVO.getClassName())
                .likeIfPresent(TemuOperationLogDO::getMethodName, reqVO.getMethodName())
                .likeIfPresent(TemuOperationLogDO::getRequestParams, reqVO.getRequestParams())
                .orderByDesc(TemuOperationLogDO::getId));
    }

    /**
     * 查询用户充值记录
     *
     * @param reqVO 查询条件
     * @return 用户充值记录列表
     */
    @Select("<script>" +
            "SELECT " +
            "   t.id, " +
            "   t.user_id, " +
            "   t.user_name, " +
            "   t.operation_time, " +
            "   t.ip_address, " +
            "   t.response_result " +
            "FROM temu_operation_log t " +
            "WHERE t.method_name = 'createWalletRecharge' " +
            "<if test='reqVO.userId != null and reqVO.userId != \"\"'>" +
            "   AND t.user_id LIKE CONCAT('%', #{reqVO.userId}, '%') " +
            "</if>" +
            "<if test='reqVO.nickname != null and reqVO.nickname != \"\"'>" +
            "   AND t.user_name LIKE CONCAT('%', #{reqVO.nickname}, '%') " +
            "</if>" +
            "<if test='reqVO.rechargeTime != null and reqVO.rechargeTime.length == 2'>" +
            "   AND t.operation_time BETWEEN #{reqVO.rechargeTime[0]} AND #{reqVO.rechargeTime[1]} " +
            "</if>" +
            "<if test='reqVO.ip != null and reqVO.ip != \"\"'>" +
            "   AND t.ip_address LIKE CONCAT('%', #{reqVO.ip}, '%') " +
            "</if>" +
            "ORDER BY t.operation_time DESC " +
            "LIMIT #{reqVO.pageSize} OFFSET #{offset}" +
            "</script>")
    List<TemuOperationLogDO> selectUserRechargeRecords(@Param("reqVO") UserRechargeRecordPageReqVO reqVO, 
                                                       @Param("offset") Integer offset);

    /**
     * 查询用户充值记录（用于导出，不分页）
     *
     * @param reqVO 查询条件
     * @return 用户充值记录列表
     */
    @Select("<script>" +
            "SELECT " +
            "   t.id, " +
            "   t.user_id, " +
            "   t.user_name, " +
            "   t.operation_time, " +
            "   t.ip_address, " +
            "   t.response_result " +
            "FROM temu_operation_log t " +
            "WHERE t.method_name = 'createWalletRecharge' " +
            "<if test='reqVO.userId != null and reqVO.userId != \"\"'>" +
            "   AND t.user_id LIKE CONCAT('%', #{reqVO.userId}, '%') " +
            "</if>" +
            "<if test='reqVO.nickname != null and reqVO.nickname != \"\"'>" +
            "   AND t.user_name LIKE CONCAT('%', #{reqVO.nickname}, '%') " +
            "</if>" +
            "<if test='reqVO.rechargeTime != null and reqVO.rechargeTime.length == 2'>" +
            "   AND t.operation_time BETWEEN #{reqVO.rechargeTime[0]} AND #{reqVO.rechargeTime[1]} " +
            "</if>" +
            "<if test='reqVO.ip != null and reqVO.ip != \"\"'>" +
            "   AND t.ip_address LIKE CONCAT('%', #{reqVO.ip}, '%') " +
            "</if>" +
            "ORDER BY t.operation_time DESC" +
            "</script>")
    List<TemuOperationLogDO> selectUserRechargeRecordsForExport(@Param("reqVO") UserRechargeRecordPageReqVO reqVO);

    /**
     * 统计用户充值记录总数
     *
     * @param reqVO 查询条件
     * @return 总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM temu_operation_log t " +
            "WHERE t.method_name = 'createWalletRecharge' " +
            "<if test='reqVO.userId != null and reqVO.userId != \"\"'>" +
            "   AND t.user_id LIKE CONCAT('%', #{reqVO.userId}, '%') " +
            "</if>" +
            "<if test='reqVO.nickname != null and reqVO.nickname != \"\"'>" +
            "   AND t.user_name LIKE CONCAT('%', #{reqVO.nickname}, '%') " +
            "</if>" +
            "<if test='reqVO.rechargeTime != null and reqVO.rechargeTime.length == 2'>" +
            "   AND t.operation_time BETWEEN #{reqVO.rechargeTime[0]} AND #{reqVO.rechargeTime[1]} " +
            "</if>" +
            "<if test='reqVO.ip != null and reqVO.ip != \"\"'>" +
            "   AND t.ip_address LIKE CONCAT('%', #{reqVO.ip}, '%') " +
            "</if>" +
            "</script>")
    Long selectUserRechargeRecordCount(@Param("reqVO") UserRechargeRecordPageReqVO reqVO);

}