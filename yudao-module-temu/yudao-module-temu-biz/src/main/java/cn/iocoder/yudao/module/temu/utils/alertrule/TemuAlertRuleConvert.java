package cn.iocoder.yudao.module.temu.utils.alertrule;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRuleCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRuleRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.alertRule.TemuAlertRuleUpdateReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuAlertRuleDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 告警规则 Convert
 *
 * @author 芋道源码
 */
@Mapper(componentModel = "spring")
public interface TemuAlertRuleConvert {

    TemuAlertRuleConvert INSTANCE = Mappers.getMapper(TemuAlertRuleConvert.class);

    TemuAlertRuleDO convert(TemuAlertRuleCreateReqVO bean);

    TemuAlertRuleDO convert(TemuAlertRuleUpdateReqVO bean);

    TemuAlertRuleRespVO convert(TemuAlertRuleDO bean);

    List<TemuAlertRuleRespVO> convertList(List<TemuAlertRuleDO> list);

    PageResult<TemuAlertRuleRespVO> convertPage(PageResult<TemuAlertRuleDO> page);

}
