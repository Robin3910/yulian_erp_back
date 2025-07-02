package cn.iocoder.yudao.module.temu.service.operationlog;

import javax.validation.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogSaveReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.UserRechargeRecordPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.UserRechargeRecordRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.OrderPlacementRecordPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.OrderPlacementRecordRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.OrderPlacementAmountStatisticsRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOperationLogDO;

import java.util.List;

/**
 * 操作日志 Service 接口
 *
 * @author 禹链科技
 */
public interface TemuOperationLogService {

    /**
     * 创建操作日志
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createOperationLog(@Valid TemuOperationLogSaveReqVO createReqVO);

    /**
     * 更新操作日志
     *
     * @param updateReqVO 更新信息
     */
    void updateOperationLog(@Valid TemuOperationLogSaveReqVO updateReqVO);

    /**
     * 删除操作日志
     *
     * @param id 编号
     */
    void deleteOperationLog(Long id);

    /**
     * 获得操作日志
     *
     * @param id 编号
     * @return 操作日志
     */
    TemuOperationLogDO getOperationLog(Long id);

    /**
     * 获得操作日志分页
     *
     * @param pageReqVO 分页查询
     * @return 操作日志分页
     */
    PageResult<TemuOperationLogDO> getOperationLogPage(TemuOperationLogPageReqVO pageReqVO);

    /**
     * 获得用户充值记录分页
     *
     * @param pageReqVO 分页查询
     * @return 用户充值记录分页
     */
    PageResult<UserRechargeRecordRespVO> getUserRechargeRecordPage(UserRechargeRecordPageReqVO pageReqVO);

    /**
     * 获得用户充值记录列表（用于导出）
     *
     * @param pageReqVO 查询条件
     * @return 用户充值记录列表
     */
    List<UserRechargeRecordRespVO> getUserRechargeRecordList(UserRechargeRecordPageReqVO pageReqVO);

    /**
     * 异步创建操作日志（供切面调用）
     *
     * @param module 操作模块
     * @param operationType 操作类型
     * @param requestParams 请求参数
     * @param responseResult 响应结果
     * @param className 类名
     * @param methodName 方法名
     * @param userId 用户ID
     * @param userName 用户名
     * @param ipAddress IP地址
     */
    void createOperationLogAsync(String module, String operationType, String requestParams, 
                                String responseResult, String className, String methodName,
                                String userId, String userName, String ipAddress);

    /**
     * 获得批量下单记录分页
     *
     * @param pageReqVO 分页查询
     * @return 批量下单记录分页
     */
    PageResult<OrderPlacementRecordRespVO> getOrderPlacementRecordPage(OrderPlacementRecordPageReqVO pageReqVO);

    /**
     * 获得批量下单记录列表，用于导出
     *
     * @param pageReqVO 查询条件
     * @return 批量下单记录列表
     */
    List<OrderPlacementRecordRespVO> getOrderPlacementRecordList(OrderPlacementRecordPageReqVO pageReqVO);

    /**
     * 获得批量下单记录总金额统计
     *
     * @param pageReqVO 查询条件
     * @return 批量下单记录总金额统计
     */
    OrderPlacementAmountStatisticsRespVO getOrderPlacementAmountStatistics(OrderPlacementRecordPageReqVO pageReqVO);
}