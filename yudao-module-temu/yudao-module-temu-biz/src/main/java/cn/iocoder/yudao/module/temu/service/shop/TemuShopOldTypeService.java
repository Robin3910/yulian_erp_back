package cn.iocoder.yudao.module.temu.service.shop;

import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopBatchSaveSkcReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopOldTypeReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopOldTypeRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopOldTypeUpdateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopOldTypeDeleteReqVO;

import java.util.List;

public interface TemuShopOldTypeService {
    //批量保存
    Integer batchSaveOldTypeSkc(List<TemuShopBatchSaveSkcReqVO> saveSkcReqVOList);

    //获取合规单信息列表
    List<TemuShopOldTypeRespVO> getOldTypeInfo(TemuShopOldTypeReqVO reqVO);

    // 批量更新合规单信息
    void batchUpdateOldTypeInfo(List<TemuShopOldTypeUpdateReqVO> updateReqList);

    //批量删除合规单信息(逻辑删除）
    void batchDeleteOldTypeInfo(TemuShopOldTypeDeleteReqVO deleteReqVO);
}
