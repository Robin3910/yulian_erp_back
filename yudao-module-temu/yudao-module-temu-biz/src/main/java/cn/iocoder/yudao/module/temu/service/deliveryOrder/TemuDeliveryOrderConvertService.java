package cn.iocoder.yudao.module.temu.service.deliveryOrder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderSimpleVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuBoxMarkQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuBoxMarkRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.goods.TemuCustomGoodsLabelQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.goods.TemuCustomGoodsLabelRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderTrackingValidateRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.print.TemuPrintDataKeyRespVO;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TemuDeliveryOrderConvertService {

    TemuDeliveryOrderSimpleVO convert(JsonNode item);

    PageResult<TemuDeliveryOrderSimpleVO> queryTemuLogisticsPage(TemuDeliveryOrderQueryReqVO reqVO);

    /**
     * 查询物流面单信息
     *
     * @param reqVO 查询参数
     * @return 物流面单列表
     */
    List<TemuBoxMarkRespVO> queryBoxMark(TemuBoxMarkQueryReqVO reqVO);

    /**
     * 查询定制sku条码信息
     *
     * @param reqVO 查询参数
     * @return 定制sku条码信息
     */
    TemuCustomGoodsLabelRespVO queryCustomGoodsLabel(TemuCustomGoodsLabelQueryReqVO reqVO);

    /**
     * 获取物流面单打印数据Key
     *
     * @param reqVO 查询参数
     * @return 打印数据Key
     */
    TemuPrintDataKeyRespVO getBoxMarkPrintDataKey(TemuBoxMarkQueryReqVO reqVO);

    /**
     * 获取定制sku条码打印数据Key
     *
     * @param reqVO 查询参数
     * @return 打印数据Key
     */
    TemuPrintDataKeyRespVO getCustomGoodsLabelPrintDataKey(TemuCustomGoodsLabelQueryReqVO reqVO);

    /**
     * 验证物流单号
     *
     * @param shopTrackingNumbers 按shopId分组的物流单号列表，key为shopId，value为该shopId下的物流单号列表
     * @return 验证结果
     */
    TemuOrderTrackingValidateRespVO validateTrackingNumber(Map<String, Set<String>> shopTrackingNumbers);
}
