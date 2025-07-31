package cn.iocoder.yudao.module.temu.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.stock.TemuStockPreparationPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.stock.TemuStockPreparationVO;

public interface TemuStockPreparationService {

    /**
     * 获取备货单分页列表
     *
     * @param reqVO 查询条件
     * @return 备货单分页列表
     */
    PageResult<TemuStockPreparationVO> getStockPreparationPage(TemuStockPreparationPageReqVO reqVO);

    /**
     * 保存备货单
     *
     * @return 成功保存的订单数量
     */
    int saveStockPreparation();
} 