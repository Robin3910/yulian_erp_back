package cn.iocoder.yudao.module.temu.service.rework.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.temu.controller.admin.vo.rework.TemuOrderReworkCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.rework.TemuOrderReworkRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderReworkDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderReworkMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuWorkerTaskMapper;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuWorkerTaskDO;
import cn.iocoder.yudao.module.temu.service.rework.TemuOrderReworkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.ORDER_NOT_EXISTS;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.rework.TemuOrderReworkPageReqVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 订单返工 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class TemuOrderReworkServiceImpl implements TemuOrderReworkService {

    @Resource
    private TemuOrderReworkMapper temuOrderReworkMapper;

    @Resource
    private TemuOrderMapper temuOrderMapper;

    @Resource
    private TemuWorkerTaskMapper temuWorkerTaskMapper;

    /**
     * 清空返工发起人和作图人
     */
    private void clearReworkNames(TemuOrderReworkDO rework) {
        rework.setReworkInitiatorName(null);
        rework.setReworkDrawUserName(null);
        rework.setReworkDrawUserId(null);
    }

    /**
     * 设置返工作图人信息（与上一次作图人保持一致）
     */
    private void setReworkDrawUserInfo(TemuOrderReworkDO rework, String lastDrawUserName, Long lastDrawUserId) {
        rework.setReworkDrawUserName(lastDrawUserName);
        rework.setReworkDrawUserId(lastDrawUserId);
    }

    @Override
    public Long createRework(TemuOrderReworkCreateReqVO createReqVO) {
        // 1. 根据 orderNo 和 customSku 联合查找订单信息（确保唯一）
        TemuOrderDO order = temuOrderMapper.selectByCustomSkuAndOrderNo(createReqVO.getCustomSku(), createReqVO.getOrderNo());
        if (order == null) {
            throw new ServiceException(ORDER_NOT_EXISTS);
        }

        // 2. 查找该订单的作图任务记录，获取上一次作图人信息
        TemuWorkerTaskDO lastDrawTask = temuWorkerTaskMapper.selectLatestDrawTaskByOrderNoAndCustomSku(
                createReqVO.getOrderNo(), createReqVO.getCustomSku(), (byte)1);
        
        // 3. 检查是否已经存在返工记录
        TemuOrderReworkDO existingRework = temuOrderReworkMapper.selectOne(
                TemuOrderReworkDO::getCustomSku, createReqVO.getCustomSku());
        
        if (existingRework != null) {
            // 如果已存在返工记录，则更新返工次数和状态
            return updateExistingRework(existingRework, createReqVO.getReworkReason(), lastDrawTask);
        }

        // 4. 创建新的返工记录
        return createNewRework(order, createReqVO.getReworkReason(), lastDrawTask);
    }

    /**
     * 更新已存在的返工记录（重复返工）
     */
    private Long updateExistingRework(TemuOrderReworkDO existingRework, String newReworkReason, TemuWorkerTaskDO lastDrawTask) {
        // 获取上一次的作图人信息
        String lastDrawUserName = existingRework.getReworkDrawUserName();
        Long lastDrawUserId = existingRework.getReworkDrawUserId();
        
        // 如果返工记录中没有作图人信息，但有作图任务记录，则使用作图任务记录的信息
        if (lastDrawUserName == null && lastDrawTask != null) {
            lastDrawUserName = lastDrawTask.getWorkerName();
            lastDrawUserId = lastDrawTask.getWorkerId();
        }
        
        // 更新返工记录
        existingRework.setReworkReason(newReworkReason);
        existingRework.setLastDrawUserName(lastDrawUserName);
        existingRework.setLastDrawUserId(lastDrawUserId);
        // 设置返工作图人信息与上一次作图人信息保持一致
        setReworkDrawUserInfo(existingRework, lastDrawUserName, lastDrawUserId);
        existingRework.setIsFinished(0); // 重置为未完成状态
        existingRework.setReworkCount(existingRework.getReworkCount() + 1); // 返工次数+1
        // 更新数据库
        temuOrderReworkMapper.updateById(existingRework);
        log.info("[updateExistingRework][customSku({}) 更新返工记录成功，返工记录ID({})，返工次数({})，上一次作图人({})，上一次作图人ID({})]", 
                existingRework.getCustomSku(), existingRework.getId(), existingRework.getReworkCount(), lastDrawUserName, lastDrawUserId);
        return existingRework.getId();
    }

    /**
     * 创建新的返工记录
     */
    private Long createNewRework(TemuOrderDO order, String reworkReason, TemuWorkerTaskDO lastDrawTask) {
        // 获取上一次作图人信息
        String lastDrawUserName = null;
        Long lastDrawUserId = null;
        
        // 优先使用作图任务记录的信息
        if (lastDrawTask != null) {
            lastDrawUserName = lastDrawTask.getWorkerName();
            lastDrawUserId = lastDrawTask.getWorkerId();
        } else {
            // 如果没有作图任务记录，查询该订单的历史返工记录
            List<TemuOrderReworkDO> historyReworks = temuOrderReworkMapper.selectList(
                    new LambdaQueryWrapper<TemuOrderReworkDO>()
                            .eq(TemuOrderReworkDO::getOrderNo, order.getOrderNo())
                            .orderByDesc(TemuOrderReworkDO::getCreateTime)
                            .last("LIMIT 1")
            );
            if (!historyReworks.isEmpty()) {
                TemuOrderReworkDO lastRework = historyReworks.get(0);
                lastDrawUserName = lastRework.getReworkDrawUserName();
                lastDrawUserId = lastRework.getReworkDrawUserId();
            }
        }
        
        // 创建返工记录
        TemuOrderReworkDO rework = new TemuOrderReworkDO();
        rework.setOrderNo(order.getOrderNo());
        rework.setProductTitle(order.getProductTitle());
        rework.setProductImgUrl(order.getProductImgUrl());
        rework.setProductProperties(order.getProductProperties());
        rework.setSku(order.getSku());
        rework.setSkc(order.getSkc());
        rework.setCustomSku(order.getCustomSku());
        rework.setReworkReason(reworkReason);
        rework.setShopId(order.getShopId());
        // 复制定制图片和文字信息
        rework.setCustomImageUrls(order.getCustomImageUrls());
        rework.setCustomTextList(order.getCustomTextList());
        // 设置上一次作图人信息（如果是第一次返工则为null）
        rework.setLastDrawUserName(lastDrawUserName);
        rework.setLastDrawUserId(lastDrawUserId);
        // 设置返工作图人信息与上一次作图人信息保持一致
        setReworkDrawUserInfo(rework, lastDrawUserName, lastDrawUserId);
        // 设置返工次数为1
        rework.setReworkCount(1);
        // 设置初始状态为未完成
        rework.setIsFinished(0);
        // 只清空发起人，保留作图人信息
        rework.setReworkInitiatorName(null);
        // 插入数据库
        temuOrderReworkMapper.insert(rework);
        // 强制数据库字段置空
        temuOrderReworkMapper.clearReworkNamesByCustomSku(rework.getCustomSku());
        log.info("[createNewRework][customSku({}) 创建返工记录成功，返工记录ID({})，上一次作图人({})，上一次作图人ID({})]", 
                order.getCustomSku(), rework.getId(), lastDrawUserName, lastDrawUserId);
        return rework.getId();
    }

    @Override
    public TemuOrderReworkDO getReworkByCustomSku(String customSku) {
        return temuOrderReworkMapper.selectOne(
                TemuOrderReworkDO::getCustomSku, customSku);
    }



    @Override
    public void finishRework(String customSku) {
        int updated = temuOrderReworkMapper.finishReworkByCustomSku(customSku);
        if (updated == 0) {
            throw new ServiceException(BAD_REQUEST.getCode(), "未找到对应的返工订单");
        }
    }



    @Override
    public TemuOrderReworkDO finishReworkAndReturn(String customSku) {
        int updated = temuOrderReworkMapper.finishReworkByCustomSku(customSku);
        if (updated == 0) {
            throw new ServiceException(BAD_REQUEST.getCode(), "未找到对应的返工订单");
        }
        return temuOrderReworkMapper.selectOne(TemuOrderReworkDO::getCustomSku, customSku);
    }

    @Override
    public PageResult<TemuOrderReworkDO> getReworkPage(TemuOrderReworkPageReqVO reqVO) {
        Page<TemuOrderReworkDO> page = new Page<>(reqVO.getPageNo(), reqVO.getPageSize());
        LambdaQueryWrapper<TemuOrderReworkDO> wrapper = new LambdaQueryWrapper<>();
        if (reqVO.getCustomSku() != null && !reqVO.getCustomSku().isEmpty()) {
            wrapper.eq(TemuOrderReworkDO::getCustomSku, reqVO.getCustomSku());
        }
        if (reqVO.getIsFinished() != null) {
            wrapper.eq(TemuOrderReworkDO::getIsFinished, reqVO.getIsFinished());
        }
        // 先按 is_finished 升序（未完成0在前），再按 id 倒序
        wrapper.orderByAsc(TemuOrderReworkDO::getIsFinished).orderByDesc(TemuOrderReworkDO::getId);
        Page<TemuOrderReworkDO> result = temuOrderReworkMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    @Override
    public void updateLastDrawUserInfo(String customSku, String lastDrawUserName, Long lastDrawUserId) {
        int updated = temuOrderReworkMapper.updateLastDrawUserInfo(customSku, lastDrawUserName, lastDrawUserId);
        if (updated == 0) {
            throw new ServiceException(BAD_REQUEST.getCode(), "未找到对应的返工订单");
        }
    }

    @Override
    public void updateReworkInitiator(Long reworkId, String reworkInitiatorName) {
        int updated = temuOrderReworkMapper.updateReworkInitiator(reworkId, reworkInitiatorName);
        if (updated == 0) {
            throw new ServiceException(BAD_REQUEST.getCode(), "未找到对应的返工订单");
        }
        log.info("[updateReworkInitiator][reworkId({}) 更新返工发起人成功，发起人({})]", reworkId, reworkInitiatorName);
    }

    @Override
    public TemuOrderReworkDO getReworkById(Long reworkId) {
        return temuOrderReworkMapper.selectById(reworkId);
    }

    @Override
    public void updateReworkDrawUser(Long reworkId, String reworkDrawUserName, Long reworkDrawUserId) {
        int updated = temuOrderReworkMapper.updateReworkDrawUser(reworkId, reworkDrawUserName, reworkDrawUserId);
        if (updated == 0) {
            throw new ServiceException(BAD_REQUEST.getCode(), "未找到对应的返工订单");
        }
        log.info("[updateReworkDrawUser][reworkId({}) 更新返工作图人成功，作图人({})，作图人ID({})]", 
                reworkId, reworkDrawUserName, reworkDrawUserId);
    }
} 