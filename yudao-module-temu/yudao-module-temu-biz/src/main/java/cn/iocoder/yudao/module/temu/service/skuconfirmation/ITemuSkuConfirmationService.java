package cn.iocoder.yudao.module.temu.service.skuconfirmation;

import cn.iocoder.yudao.module.temu.controller.admin.vo.skuconfirmation.TemuSkuConfirmationRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuSkuConfirmationDO;

import java.util.List;

/**
 * SKU确认 Service 接口
 */
public interface ITemuSkuConfirmationService {

    /**
     * 确认SKU
     *
     * @param shopId 店铺ID
     * @param sku    SKU编号
     * @return 是否成功
     */
    Boolean confirmSku(String shopId, String sku);

    /**
     * 通过订单ID确认SKU
     *
     * @param orderId 订单ID
     * @return 是否成功
     */
    Boolean confirmSkuByOrderId(Long orderId);

    /**
     * 取消确认SKU
     *
     * @param id SKU确认ID
     * @return 是否成功
     */
    Boolean cancelConfirmation(Long id);

    /**
     * 获取店铺已确认的SKU列表
     *
     * @param shopId 店铺ID
     * @return 已确认的SKU列表
     */
    List<TemuSkuConfirmationRespVO> getConfirmedSkuList(String shopId);

    /**
     * 检查SKU是否已确认
     *
     * @param shopId 店铺ID
     * @param sku    SKU编号
     * @return 是否已确认
     */
    Boolean isSkuConfirmed(String shopId, String sku);
}