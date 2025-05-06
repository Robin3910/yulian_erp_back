package cn.iocoder.yudao.module.temu.service.shop.impl;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopBatchSaveSkcReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopOldTypeReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopOldTypeRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopOldTypeUpdateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch.TemuShopOldTypeDeleteReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopOldTypeSkcDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopOldTypeSkcMapper;
import cn.iocoder.yudao.module.temu.service.shop.TemuShopOldTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

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
        // 1.参数校验：如果输入列表为空，直接返回0
        if (saveSkcReqVOList == null || saveSkcReqVOList.isEmpty()) {
            return 0;
        }
        // 2.准备待插入的实体列表
        List<TemuShopOldTypeSkcDO> oldTypeSkcDOList = new ArrayList<>();
        for (TemuShopBatchSaveSkcReqVO reqVO : saveSkcReqVOList) {
            // 3.将VO对象转换为DO实体
            TemuShopOldTypeSkcDO oldTypeSkcDO = new TemuShopOldTypeSkcDO();
            oldTypeSkcDO.setShopId(reqVO.getShopId());
            oldTypeSkcDO.setSkc(reqVO.getSkc());
            oldTypeSkcDO.setOldTypeUrl(reqVO.getOldTypeUrl());
            oldTypeSkcDO.setOldType(reqVO.getOldType());
            oldTypeSkcDOList.add(oldTypeSkcDO);
        }
        // 4.批量插入数据库
        temuShopOldTypeSkcMapper.insertBatch(oldTypeSkcDOList);
        log.info("oldTypeSkcDOList.size={}", oldTypeSkcDOList.size());
        // 返回成功插入的记录数
        return oldTypeSkcDOList.size();
    }

    //获取合规单信息
    @Override
    public List<TemuShopOldTypeRespVO> getOldTypeInfo(TemuShopOldTypeReqVO reqVO) {
        log.info("查询参数：shopId={}, skc={}, oldType={}", reqVO.getShopId(), reqVO.getSkc(), reqVO.getOldType());
        // 1.构建动态查询条件
        LambdaQueryWrapperX<TemuShopOldTypeSkcDO> queryWrapper = new LambdaQueryWrapperX<TemuShopOldTypeSkcDO>()
                .eqIfPresent(TemuShopOldTypeSkcDO::getShopId, reqVO.getShopId())
                .eqIfPresent(TemuShopOldTypeSkcDO::getSkc, reqVO.getSkc())
                .eqIfPresent(TemuShopOldTypeSkcDO::getOldType, reqVO.getOldType());
        // 2.查询并返回列表结果
        List<TemuShopOldTypeSkcDO> result = temuShopOldTypeSkcMapper.selectList(queryWrapper);
        log.info("查询到记录数：{}", result.size());
        // 3.转换为VO对象
        List<TemuShopOldTypeRespVO> respVOList = new ArrayList<>();
        for (TemuShopOldTypeSkcDO skcDO : result) {
            TemuShopOldTypeRespVO respVO = new TemuShopOldTypeRespVO();
            respVO.setShopId(skcDO.getShopId());
            respVO.setSkc(skcDO.getSkc());
            respVO.setOldTypeUrl(skcDO.getOldTypeUrl());
            respVO.setOldType(skcDO.getOldType());
            respVOList.add(respVO);
        }
        return respVOList;
    }

    //批量更新合规单信息
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateOldTypeInfo(List<TemuShopOldTypeUpdateReqVO> updateReqList) {
        // 1.参数校验：如果更新列表为空，直接返回
        if (CollUtil.isEmpty(updateReqList)) {
            return;
        }
        // 2.遍历每个更新请求
        for (TemuShopOldTypeUpdateReqVO updateReq : updateReqList) {
            // 2.1 跳过SKC为空的无效记录
            if (StrUtil.isEmpty(updateReq.getSkc())) {
                continue;
            }
            // 2.2 构建更新条件：按shopId和skc精确匹配
            LambdaQueryWrapperX<TemuShopOldTypeSkcDO> updateWrapper = new LambdaQueryWrapperX<TemuShopOldTypeSkcDO>()
                    .eq(TemuShopOldTypeSkcDO::getShopId, updateReq.getShopId())
                    .eq(TemuShopOldTypeSkcDO::getSkc, updateReq.getSkc());
            // 2.3 设置待更新的字段（只更新oldTypeUrl和oldType）
            TemuShopOldTypeSkcDO updateObj = new TemuShopOldTypeSkcDO();
            updateObj.setOldTypeUrl(updateReq.getOldTypeUrl());
            updateObj.setOldType(updateReq.getOldType());
            // 2.4 执行更新操作，并记录结果// 执行更新
            int rows = temuShopOldTypeSkcMapper.update(updateObj, updateWrapper);
            log.info("更新合规单信息，店铺ID：{}，SKC：{}，更新结果：{}", 
                    updateReq.getShopId(), updateReq.getSkc(), rows > 0 ? "成功" : "失败");
        }
    }

    //批量删除合规单信息
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteOldTypeInfo(TemuShopOldTypeDeleteReqVO deleteReqVO) {
        // 参数校验：如果SKC列表为空，直接返回
        if (CollUtil.isEmpty(deleteReqVO.getSkcList())) {
            return;
        }
        // 构建删除条件：
        // 1. 按 shopId 精确匹配
        // 2. 按 skc 列表批量匹配（IN语句）
        // 3. 可选 按 oldType 精确匹配（如果非空）
        LambdaQueryWrapperX<TemuShopOldTypeSkcDO> deleteWrapper = new LambdaQueryWrapperX<TemuShopOldTypeSkcDO>()
                .eq(TemuShopOldTypeSkcDO::getShopId, deleteReqVO.getShopId())
                .in(TemuShopOldTypeSkcDO::getSkc, deleteReqVO.getSkcList())
                .eqIfPresent(TemuShopOldTypeSkcDO::getOldType, deleteReqVO.getOldType());
        // 执行逻辑删除
        int rows = temuShopOldTypeSkcMapper.delete(deleteWrapper);
        log.info("批量物理删除合规单信息完成，店铺ID：{}，SKC数量：{}，实际删除数量：{}", 
                deleteReqVO.getShopId(), deleteReqVO.getSkcList().size(), rows);
    }
}
