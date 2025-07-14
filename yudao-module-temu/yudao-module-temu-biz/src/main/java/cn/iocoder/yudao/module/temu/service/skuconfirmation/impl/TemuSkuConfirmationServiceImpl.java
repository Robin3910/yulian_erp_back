package cn.iocoder.yudao.module.temu.service.skuconfirmation.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.temu.controller.admin.vo.skuconfirmation.TemuSkuConfirmationRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuSkuConfirmationDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuSkuConfirmationMapper;
import cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.temu.service.skuconfirmation.ITemuSkuConfirmationService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * SKU确认 Service 实现类
 */
@Service
@Validated
public class TemuSkuConfirmationServiceImpl implements ITemuSkuConfirmationService {

    @Resource
    private TemuSkuConfirmationMapper temuSkuConfirmationMapper;

    @Resource
    private TemuOrderMapper temuOrderMapper;

    @Override
    public Boolean confirmSku(String shopId, String sku) {
        // 检查是否已存在
        TemuSkuConfirmationDO existingConfirmation = temuSkuConfirmationMapper.selectByShopIdAndSku(shopId, sku);
        if (existingConfirmation != null) {
            // 如果已存在但状态为未确认，则更新为已确认
            if (existingConfirmation.getStatus() != 1) {
                existingConfirmation.setStatus(1);
                temuSkuConfirmationMapper.updateById(existingConfirmation);
            }
            return true;
        }

        // 创建新记录
        TemuSkuConfirmationDO confirmation = new TemuSkuConfirmationDO();
        confirmation.setShopId(shopId);
        confirmation.setSku(sku);
        confirmation.setStatus(1); // 已确认
        temuSkuConfirmationMapper.insert(confirmation);
        return true;
    }

    @Override
    public Boolean confirmSkuByOrderId(Long orderId) {
        // 通过订单ID查询订单信息
        TemuOrderDO order = temuOrderMapper.selectById(orderId);
        if (order == null) {
            throw exception(ErrorCodeConstants.ORDER_NOT_EXISTS);
        }

        // 获取shopId和sku
        String shopId = String.valueOf(order.getShopId());
        String sku = order.getSku();

        // 调用已有的确认方法
        return confirmSku(shopId, sku);
    }

    @Override
    public Boolean cancelConfirmation(Long id) {
        TemuSkuConfirmationDO confirmation = temuSkuConfirmationMapper.selectById(id);
        if (confirmation == null) {
            return false;
        }

        // 更新状态为未确认
        confirmation.setStatus(0);
        temuSkuConfirmationMapper.updateById(confirmation);
        return true;
    }

    @Override
    public List<TemuSkuConfirmationRespVO> getConfirmedSkuList(String shopId) {
        List<TemuSkuConfirmationDO> confirmationList = temuSkuConfirmationMapper.selectListByShopId(shopId);
        List<TemuSkuConfirmationRespVO> result = new ArrayList<>(confirmationList.size());

        for (TemuSkuConfirmationDO confirmation : confirmationList) {
            TemuSkuConfirmationRespVO respVO = new TemuSkuConfirmationRespVO();
            BeanUtil.copyProperties(confirmation, respVO);
            result.add(respVO);
        }

        return result;
    }

    @Override
    public Boolean isSkuConfirmed(String shopId, String sku) {
        TemuSkuConfirmationDO confirmation = temuSkuConfirmationMapper.selectByShopIdAndSku(shopId, sku);
        return confirmation != null && confirmation.getStatus() == 1;
    }
}