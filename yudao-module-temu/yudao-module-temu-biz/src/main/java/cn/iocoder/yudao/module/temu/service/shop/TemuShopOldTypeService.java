package cn.iocoder.yudao.module.temu.service.shop;

import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopBatchSaveSkcReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopOldTypeReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopOldTypeSkcDO;

import java.util.List;

public interface TemuShopOldTypeService {
    Integer batchSaveOldTypeSkc(List<TemuShopBatchSaveSkcReqVO> saveSkcReqVOList);

    /**
     * 获取合规单信息列表
     *
     * @param reqVO 查询条件
     * @return 合规单信息列表
     */
    List<TemuShopOldTypeSkcDO> getOldTypeInfo(TemuShopOldTypeReqVO reqVO);
}
