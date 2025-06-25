package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRulePageReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuAlertRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警规则 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface TemuAlertRuleMapper extends BaseMapperX<TemuAlertRuleDO> {

    /**
     * 分页查询告警规则
     *
     * @param reqVO 查询条件
     * @return 告警规则分页结果
     */
    default PageResult<TemuAlertRuleDO> selectPage(TemuAlertRulePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TemuAlertRuleDO>()
                .likeIfPresent(TemuAlertRuleDO::getName, reqVO.getName())
                .eqIfPresent(TemuAlertRuleDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(TemuAlertRuleDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TemuAlertRuleDO::getId));
    }

    /**
     * 获取所有启用的告警规则
     *
     * @return 启用的告警规则列表
     */
    default List<TemuAlertRuleDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<TemuAlertRuleDO>()
                .eq(TemuAlertRuleDO::getStatus, status));
    }

    /**
     * 更新告警规则的最后执行时间
     *
     * @param id              规则编号
     * @param lastExecuteTime 最后执行时间
     * @return 更新结果
     */
    default int updateLastExecuteTime(Long id, LocalDateTime lastExecuteTime) {
        TemuAlertRuleDO updateObj = new TemuAlertRuleDO();
        updateObj.setId(id);
        updateObj.setLastExecuteTime(lastExecuteTime);
        return updateById(updateObj);
    }
}
