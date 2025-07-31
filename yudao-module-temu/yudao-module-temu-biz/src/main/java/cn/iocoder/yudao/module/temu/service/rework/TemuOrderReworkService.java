package cn.iocoder.yudao.module.temu.service.rework;

import cn.iocoder.yudao.module.temu.controller.admin.vo.rework.TemuOrderReworkCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.rework.TemuOrderReworkRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.rework.TemuOrderReworkPageReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderReworkDO;

import javax.validation.Valid;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

/**
 * 订单返工 Service 接口
 *
 * @author 芋道源码
 */
public interface TemuOrderReworkService {

    /**
     * 创建订单返工记录
     *
     * @param createReqVO 创建信息
     * @return 返工记录ID
     */
    Long createRework(@Valid TemuOrderReworkCreateReqVO createReqVO);

    /**
     * 根据定制SKU查询返工记录
     *
     * @param customSku 定制SKU
     * @return 返工记录
     */
    TemuOrderReworkDO getReworkByCustomSku(String customSku);



    /**
     * 完成返工
     */
    void finishRework(String customSku);

    /**
     * 完成返工并返回最新DO
     */
    TemuOrderReworkDO finishReworkAndReturn(String customSku);

    /**
     * 分页查询返工订单
     */
    PageResult<TemuOrderReworkDO> getReworkPage(TemuOrderReworkPageReqVO reqVO);

    /**
     * 更新上一次作图人信息
     */
    void updateLastDrawUserInfo(String customSku, String lastDrawUserName, Long lastDrawUserId);

    /**
     * 更新返工发起人信息
     */
    void updateReworkInitiator(Long reworkId, String reworkInitiatorName);

    /**
     * 根据ID获取返工记录
     */
    TemuOrderReworkDO getReworkById(Long reworkId);

    /**
     * 更新返工作图人信息
     */
    void updateReworkDrawUser(Long reworkId, String reworkDrawUserName, Long reworkDrawUserId);
} 