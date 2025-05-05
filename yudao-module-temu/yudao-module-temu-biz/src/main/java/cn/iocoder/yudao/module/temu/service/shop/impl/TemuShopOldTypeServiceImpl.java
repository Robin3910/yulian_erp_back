package cn.iocoder.yudao.module.temu.service.shop.impl;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopBatchSaveSkcReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopOldTypeReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopOldTypeSkcDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopOldTypeSkcMapper;
import cn.iocoder.yudao.module.temu.service.shop.TemuShopOldTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TemuShopOldTypeServiceImpl implements TemuShopOldTypeService {

    private final TemuShopOldTypeSkcMapper temuShopOldTypeSkcMapper;

    // 批量保存合规单SKC
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer batchSaveOldTypeSkc(List<TemuShopBatchSaveSkcReqVO> saveSkcReqVOList) {
        if (saveSkcReqVOList == null || saveSkcReqVOList.isEmpty()) {
            return 0;
        }

        List<TemuShopOldTypeSkcDO> oldTypeSkcDOList = new ArrayList<>();
        for (TemuShopBatchSaveSkcReqVO reqVO : saveSkcReqVOList) {
            TemuShopOldTypeSkcDO oldTypeSkcDO = new TemuShopOldTypeSkcDO();
            oldTypeSkcDO.setShopId(Long.valueOf(reqVO.getShopId()));
            oldTypeSkcDO.setSkc(reqVO.getSkc());
            oldTypeSkcDO.setOldTypeUrl(reqVO.getOldTypeUrl());
            oldTypeSkcDO.setOldType(reqVO.getOldType());
            oldTypeSkcDOList.add(oldTypeSkcDO);
        }

        // 批量插入数据
        temuShopOldTypeSkcMapper.insertBatch(oldTypeSkcDOList);
        log.info("oldTypeSkcDOList.size={}", oldTypeSkcDOList.size());
        return oldTypeSkcDOList.size();
    }

    @Override
    public List<TemuShopOldTypeSkcDO> getOldTypeInfo(TemuShopOldTypeReqVO reqVO) {
        log.info("查询参数：shopId={}, skc={}, oldType={}", reqVO.getShopId(), reqVO.getSkc(), reqVO.getOldType());

        // 构建查询条件
        LambdaQueryWrapperX<TemuShopOldTypeSkcDO> queryWrapper = new LambdaQueryWrapperX<TemuShopOldTypeSkcDO>()
                .eqIfPresent(TemuShopOldTypeSkcDO::getShopId, reqVO.getShopId())
                .eqIfPresent(TemuShopOldTypeSkcDO::getSkc, reqVO.getSkc())
                .eqIfPresent(TemuShopOldTypeSkcDO::getOldType, reqVO.getOldType());

        // 查询并返回列表结果
        List<TemuShopOldTypeSkcDO> result = temuShopOldTypeSkcMapper.selectList(queryWrapper);
        log.info("查询到记录数：{}", result.size());

        return result;
    }
}
