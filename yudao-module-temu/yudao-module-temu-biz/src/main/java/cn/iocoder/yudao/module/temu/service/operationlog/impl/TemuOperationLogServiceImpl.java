package cn.iocoder.yudao.module.temu.service.operationlog.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.object.PageUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderRespDTO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogSaveReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.UserRechargeRecordPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.UserRechargeRecordRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.OrderPlacementRecordPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.OrderPlacementRecordRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.OrderPlacementAmountStatisticsRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOperationLogDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDetailDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOperationLogMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderPlacementRecordMapper;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderPlacementRecordDO;
import cn.iocoder.yudao.module.temu.service.operationlog.TemuOperationLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Resource
    private TemuOrderMapper orderMapper;

    @Resource
    private PayOrderApi payOrderApi;

    @Resource
    private TemuOrderPlacementRecordMapper temuOrderPlacementRecordMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public PageResult<UserRechargeRecordRespVO> getUserRechargeRecordPage(UserRechargeRecordPageReqVO pageReqVO) {
        // 1. 查询操作日志记录
        int offset = PageUtils.getStart(pageReqVO);
        List<TemuOperationLogDO> operationLogs = operationLogMapper.selectUserRechargeRecords(pageReqVO, offset);
        Long total = operationLogMapper.selectUserRechargeRecordCount(pageReqVO);

        // 2. 转换为充值记录VO
        List<UserRechargeRecordRespVO> records = new ArrayList<>();
        for (TemuOperationLogDO operationLog : operationLogs) {
            UserRechargeRecordRespVO record = convertToRechargeRecord(operationLog);
            if (record != null) {
                records.add(record);
            }
        }

        // 3. 返回分页结果
        return new PageResult<>(records, total);
    }

    @Override
    public List<UserRechargeRecordRespVO> getUserRechargeRecordList(UserRechargeRecordPageReqVO pageReqVO) {
        // 1. 查询所有操作日志记录（不分页）
        List<TemuOperationLogDO> operationLogs = operationLogMapper.selectUserRechargeRecordsForExport(pageReqVO);
        
        // 2. 转换为充值记录VO
        List<UserRechargeRecordRespVO> records = new ArrayList<>();
        for (TemuOperationLogDO operationLog : operationLogs) {
            UserRechargeRecordRespVO record = convertToRechargeRecord(operationLog);
            if (record != null) {
                records.add(record);
            }
        }

        // 3. 返回记录列表
        return records;
    }

    /**
     * 将操作日志转换为充值记录
     */
    private UserRechargeRecordRespVO convertToRechargeRecord(TemuOperationLogDO operationLog) {
        try {
            UserRechargeRecordRespVO record = new UserRechargeRecordRespVO();
            record.setUserId(operationLog.getUserId());
            record.setNickname(operationLog.getUserName());
            record.setRechargeTime(operationLog.getOperationTime());
            record.setIp(operationLog.getIpAddress());

            // 解析response_result中的JSON数据
            String responseResult = operationLog.getResponseResult();
            if (StrUtil.isNotBlank(responseResult)) {
                JsonNode jsonNode = objectMapper.readTree(responseResult);
                
                // 提取data.payOrderId
                JsonNode dataNode = jsonNode.get("data");
                if (dataNode != null && dataNode.has("payOrderId")) {
                    Long payOrderId = dataNode.get("payOrderId").asLong();
                    record.setPayOrderId(payOrderId);

                    // 查询支付状态
                    try {
                        PayOrderRespDTO payOrder = payOrderApi.getOrder(payOrderId);
                        if (payOrder != null) {
                            record.setPayStatus(payOrder.getStatus());
                            record.setPayStatusName(getPayStatusName(payOrder.getStatus()));
                            // 将分转换为元，保留两位小数
                            if (payOrder.getPrice() != null) {
                                BigDecimal amount = new BigDecimal(payOrder.getPrice())
                                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                                record.setAmount(amount);
                            } else {
                                record.setAmount(BigDecimal.ZERO);
                            }
                        } else {
                            record.setPayStatus(null);
                            record.setPayStatusName("支付订单不存在");
                            record.setAmount(BigDecimal.ZERO);
                        }
                    } catch (Exception e) {
                        log.warn("查询支付订单状态失败，payOrderId: {}, error: {}", payOrderId, e.getMessage());
                        record.setPayStatus(null);
                        record.setPayStatusName("查询支付状态失败");
                        record.setAmount(BigDecimal.ZERO);
                    }
                } else {
                    record.setPayOrderId(null);
                    record.setPayStatus(null);
                    record.setPayStatusName("未找到支付订单ID");
                    record.setAmount(BigDecimal.ZERO);
                }
            } else {
                record.setPayOrderId(null);
                record.setPayStatus(null);
                record.setPayStatusName("响应结果为空");
                record.setAmount(BigDecimal.ZERO);
            }

            return record;
        } catch (Exception e) {
            log.error("解析充值记录失败，operationLogId: {}, error: {}", operationLog.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取支付状态名称
     */
    private String getPayStatusName(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        
        switch (status) {
            case 0:
                return "未支付";
            case 10:
                return "支付成功";
            case 20:
                return "已退款";
            case 30:
                return "支付关闭";
            default:
                return "未知状态";
        }
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

    @Override
    public PageResult<OrderPlacementRecordRespVO> getOrderPlacementRecordPage(OrderPlacementRecordPageReqVO pageReqVO) {
        // 构造分页查询条件
        // 这里只做简单条件，复杂条件可扩展
        PageResult<TemuOrderPlacementRecordDO> pageResult = temuOrderPlacementRecordMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<TemuOrderPlacementRecordDO>()
                .eqIfPresent(TemuOrderPlacementRecordDO::getOrderNo, pageReqVO.getOrderNo())
                .eqIfPresent(TemuOrderPlacementRecordDO::getSku, pageReqVO.getSku())
                .eqIfPresent(TemuOrderPlacementRecordDO::getSkc, pageReqVO.getSkc())
                .eqIfPresent(TemuOrderPlacementRecordDO::getCustomSku, pageReqVO.getCustomSku())
                .inIfPresent(TemuOrderPlacementRecordDO::getCategoryId, pageReqVO.getCategoryId())
                .eqIfPresent(TemuOrderPlacementRecordDO::getIsReturnOrder, pageReqVO.getIsReturnOrder())
                .betweenIfPresent(TemuOrderPlacementRecordDO::getOperationTime, pageReqVO.getOperationTime())
                .inIfPresent(TemuOrderPlacementRecordDO::getShopId, pageReqVO.getShopId())
                .eqIfPresent(TemuOrderPlacementRecordDO::getOperator, pageReqVO.getOperator())
                .orderByDesc(TemuOrderPlacementRecordDO::getOperationTime)
        );
        // 转VO
        List<OrderPlacementRecordRespVO> voList = BeanUtils.toBean(pageResult.getList(), OrderPlacementRecordRespVO.class);
        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    public List<OrderPlacementRecordRespVO> getOrderPlacementRecordList(OrderPlacementRecordPageReqVO pageReqVO) {
        // 构造查询条件，不分页
        List<TemuOrderPlacementRecordDO> list = temuOrderPlacementRecordMapper.selectList(new LambdaQueryWrapperX<TemuOrderPlacementRecordDO>()
                .eqIfPresent(TemuOrderPlacementRecordDO::getOrderNo, pageReqVO.getOrderNo())
                .eqIfPresent(TemuOrderPlacementRecordDO::getSku, pageReqVO.getSku())
                .eqIfPresent(TemuOrderPlacementRecordDO::getSkc, pageReqVO.getSkc())
                .eqIfPresent(TemuOrderPlacementRecordDO::getCustomSku, pageReqVO.getCustomSku())
                .inIfPresent(TemuOrderPlacementRecordDO::getCategoryId, pageReqVO.getCategoryId())
                .eqIfPresent(TemuOrderPlacementRecordDO::getIsReturnOrder, pageReqVO.getIsReturnOrder())
                .betweenIfPresent(TemuOrderPlacementRecordDO::getOperationTime, pageReqVO.getOperationTime())
                .inIfPresent(TemuOrderPlacementRecordDO::getShopId, pageReqVO.getShopId())
                .eqIfPresent(TemuOrderPlacementRecordDO::getOperator, pageReqVO.getOperator())
                .orderByDesc(TemuOrderPlacementRecordDO::getOperationTime)
        );
        // 转VO
        return BeanUtils.toBean(list, OrderPlacementRecordRespVO.class);
    }

    @Override
    public OrderPlacementAmountStatisticsRespVO getOrderPlacementAmountStatistics(OrderPlacementRecordPageReqVO pageReqVO) {
        // 构造查询条件
        List<TemuOrderPlacementRecordDO> list = temuOrderPlacementRecordMapper.selectList(new LambdaQueryWrapperX<TemuOrderPlacementRecordDO>()
                .eqIfPresent(TemuOrderPlacementRecordDO::getOrderNo, pageReqVO.getOrderNo())
                .eqIfPresent(TemuOrderPlacementRecordDO::getSku, pageReqVO.getSku())
                .eqIfPresent(TemuOrderPlacementRecordDO::getSkc, pageReqVO.getSkc())
                .eqIfPresent(TemuOrderPlacementRecordDO::getCustomSku, pageReqVO.getCustomSku())
                .inIfPresent(TemuOrderPlacementRecordDO::getCategoryId, pageReqVO.getCategoryId())
                .eqIfPresent(TemuOrderPlacementRecordDO::getIsReturnOrder, pageReqVO.getIsReturnOrder())
                .betweenIfPresent(TemuOrderPlacementRecordDO::getOperationTime, pageReqVO.getOperationTime())
                .inIfPresent(TemuOrderPlacementRecordDO::getShopId, pageReqVO.getShopId())
                .eqIfPresent(TemuOrderPlacementRecordDO::getOperator, pageReqVO.getOperator())
        );
        
        // 计算总金额
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (TemuOrderPlacementRecordDO record : list) {
            if (record.getTotalPrice() != null) {
                totalPrice = totalPrice.add(record.getTotalPrice());
            }
        }
        
        OrderPlacementAmountStatisticsRespVO statistics = new OrderPlacementAmountStatisticsRespVO();
        statistics.setTotalPrice(totalPrice);
        
        return statistics;
    }
}