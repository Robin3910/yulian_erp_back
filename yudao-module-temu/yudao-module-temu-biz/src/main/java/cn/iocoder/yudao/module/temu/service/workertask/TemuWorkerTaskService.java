package cn.iocoder.yudao.module.temu.service.workertask;

import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.temu.controller.admin.vo.workertask.TemuWorkerTaskPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.workertask.TemuWorkerTaskSaveReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.workertask.TemuWorkerTaskRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuWorkerTaskDO;

/**
 * 工作人员任务记录 Service 接口
 *
 * @author wujunlin
 */
public interface TemuWorkerTaskService {

    /**
     * 创建工作人员任务记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createWorkerTask(@Valid TemuWorkerTaskSaveReqVO createReqVO);

    /**
     * 更新工作人员任务记录
     *
     * @param updateReqVO 更新信息
     */
    void updateWorkerTask(@Valid TemuWorkerTaskSaveReqVO updateReqVO);

    /**
     * 删除工作人员任务记录
     *
     * @param id 编号
     */
    void deleteWorkerTask(Long id);

    /**
     * 获得工作人员任务记录
     *
     * @param id 编号
     * @return 工作人员任务记录
     */
    TemuWorkerTaskDO getWorkerTask(Long id);

    /**
     * 获得工作人员任务记录分页
     *
     * @param pageReqVO 分页查询
     * @return 工作人员任务记录分页
     */
    PageResult<TemuWorkerTaskRespVO> getWorkerTaskPage(TemuWorkerTaskPageReqVO pageReqVO);

}