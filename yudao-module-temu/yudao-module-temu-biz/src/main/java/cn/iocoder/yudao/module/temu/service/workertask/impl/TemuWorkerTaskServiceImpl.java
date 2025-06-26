package cn.iocoder.yudao.module.temu.service.workertask.impl;

import cn.iocoder.yudao.module.temu.controller.admin.vo.workertask.TemuWorkerTaskPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.workertask.TemuWorkerTaskSaveReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.workertask.TemuWorkerTaskRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuWorkerTaskDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuWorkerTaskMapper;
import cn.iocoder.yudao.module.temu.service.workertask.TemuWorkerTaskService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;


import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.*;

/**
 * 工作人员任务记录 Service 实现类
 *
 * @author wujunlin
 */
@Service
@Validated
public class TemuWorkerTaskServiceImpl implements TemuWorkerTaskService {

    @Resource
    private TemuWorkerTaskMapper workerTaskMapper;

    @Override
    public Long createWorkerTask(TemuWorkerTaskSaveReqVO createReqVO) {
        // 插入
        TemuWorkerTaskDO workerTask = BeanUtils.toBean(createReqVO, TemuWorkerTaskDO.class);
        workerTaskMapper.insert(workerTask);
        // 返回
        return workerTask.getId();
    }

    @Override
    public void updateWorkerTask(TemuWorkerTaskSaveReqVO updateReqVO) {
        // 校验存在
//        validateWorkerTaskExists(updateReqVO.getId());
        // 更新
        TemuWorkerTaskDO updateObj = BeanUtils.toBean(updateReqVO, TemuWorkerTaskDO.class);
        workerTaskMapper.updateById(updateObj);
    }

    @Override
    public void deleteWorkerTask(Long id) {
        // 校验存在
//        validateWorkerTaskExists(id);
        // 删除
        workerTaskMapper.deleteById(id);
    }

//    private void validateWorkerTaskExists(Long id) {
//        if (workerTaskMapper.selectById(id) == null) {
//            throw exception(WORKER_TASK_NOT_EXISTS);
//        }
//    }

    @Override
    public TemuWorkerTaskDO getWorkerTask(Long id) {
        return workerTaskMapper.selectById(id);
    }

    @Override
    public PageResult<TemuWorkerTaskRespVO> getWorkerTaskPage(TemuWorkerTaskPageReqVO pageReqVO) {
        return workerTaskMapper.selectPage(pageReqVO);
    }

}