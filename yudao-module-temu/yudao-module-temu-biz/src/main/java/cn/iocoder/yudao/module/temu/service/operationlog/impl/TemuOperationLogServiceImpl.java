package cn.iocoder.yudao.module.temu.service.operationlog.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogSaveReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOperationLogDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOperationLogMapper;
import cn.iocoder.yudao.module.temu.service.operationlog.TemuOperationLogService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.scheduling.annotation.Async;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 操作日志 Service 实现类
 *
 * @author 禹链科技
 */
@Service
@Validated
@Slf4j
public class TemuOperationLogServiceImpl implements TemuOperationLogService {

    @Resource
    private TemuOperationLogMapper operationLogMapper;

    @Override
    public Long createOperationLog(TemuOperationLogSaveReqVO createReqVO) {
        // 插入
        TemuOperationLogDO operationLog = BeanUtils.toBean(createReqVO, TemuOperationLogDO.class);
        operationLogMapper.insert(operationLog);
        // 返回
        return operationLog.getId();
    }

    @Override
    public void updateOperationLog(TemuOperationLogSaveReqVO updateReqVO) {
//        // 校验存在
//        validateOperationLogExists(updateReqVO.getId());
        // 更新
        TemuOperationLogDO updateObj = BeanUtils.toBean(updateReqVO, TemuOperationLogDO.class);
        operationLogMapper.updateById(updateObj);
    }

    @Override
    public void deleteOperationLog(Long id) {
//        // 校验存在
//        validateOperationLogExists(id);
        // 删除
        operationLogMapper.deleteById(id);
    }

//    private void validateOperationLogExists(Long id) {
//        if (operationLogMapper.selectById(id) == null) {
//            throw exception(OPERATION_LOG_NOT_EXISTS);
//        }
//    }

    @Override
    public TemuOperationLogDO getOperationLog(Long id) {
        return operationLogMapper.selectById(id);
    }

    @Override
    public PageResult<TemuOperationLogDO> getOperationLogPage(TemuOperationLogPageReqVO pageReqVO) {
        return operationLogMapper.selectPage(pageReqVO);
    }

    @Override
    @Async("logExecutor")
    public void createOperationLogAsync(String module, String operationType, String requestParams, 
                                       String responseResult, String className, String methodName,
                                       String userId, String userName, String ipAddress) {
        try {
            TemuOperationLogDO operationLog = new TemuOperationLogDO();
            operationLog.setModule(module);
            operationLog.setOperationType(operationType);
            operationLog.setRequestParams(requestParams);
            operationLog.setResponseResult(responseResult);
            operationLog.setClassName(className);
            operationLog.setMethodName(methodName);
            operationLog.setUserId(userId);
            operationLog.setUserName(userName);
            operationLog.setIpAddress(ipAddress);
            operationLog.setOperationTime(LocalDateTime.now());
            
            operationLogMapper.insert(operationLog);
            log.debug("操作日志记录成功: {}", JsonUtils.toJsonString(operationLog));
        } catch (Exception e) {
            log.error("记录操作日志失败: module={}, operationType={}, error={}", 
                     module, operationType, e.getMessage(), e);
        }
    }
}