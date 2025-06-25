package cn.iocoder.yudao.module.temu.service.alertRule;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRuleCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRulePageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRuleUpdateReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuAlertRuleDO;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警规则 Service 接口
 *
 * @author 芋道源码
 */
public interface TemuAlertRuleService {

    /**
     * 创建告警规则
     *
     * @param createReqVO 创建信息
     * @return 告警规则编号
     */
    Long createAlertRule(@Valid TemuAlertRuleCreateReqVO createReqVO);

    /**
     * 更新告警规则
     *
     * @param updateReqVO 更新信息
     */
    void updateAlertRule(@Valid TemuAlertRuleUpdateReqVO updateReqVO);

    /**
     * 删除告警规则
     *
     * @param id 编号
     */
    void deleteAlertRule(Long id);

    /**
     * 获得告警规则
     *
     * @param id 编号
     * @return 告警规则
     */
    TemuAlertRuleDO getAlertRule(Long id);

    /**
     * 获得告警规则分页
     *
     * @param pageReqVO 分页查询
     * @return 告警规则分页
     */
    PageResult<TemuAlertRuleDO> getAlertRulePage(TemuAlertRulePageReqVO pageReqVO);

    /**
     * 获取所有启用的告警规则
     *
     * @return 启用的告警规则列表
     */
    List<TemuAlertRuleDO> getEnabledRules();

    /**
     * 获取所有告警规则，包括启用和禁用的
     *
     * @return 所有告警规则列表
     */
    List<TemuAlertRuleDO> getAllRules();

    /**
     * 更新告警规则的最后执行时间
     *
     * @param id              规则编号
     * @param lastExecuteTime 最后执行时间
     */
    void updateLastExecuteTime(Long id, LocalDateTime lastExecuteTime);

    /**
     * 修改告警规则状态
     *
     * @param id     规则编号
     * @param status 状态
     */
    void updateAlertRuleStatus(Long id, Integer status);

    /**
     * 测试执行告警规则
     *
     * @param id 规则编号
     * @return 执行结果
     */
    Integer testAlertRule(Long id);
}
