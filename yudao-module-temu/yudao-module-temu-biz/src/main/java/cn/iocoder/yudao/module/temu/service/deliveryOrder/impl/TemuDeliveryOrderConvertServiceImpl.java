package cn.iocoder.yudao.module.temu.service.deliveryOrder.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderSimpleVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuBoxMarkQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuBoxMarkRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.goods.TemuCustomGoodsLabelQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.goods.TemuCustomGoodsLabelRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderTrackingValidateRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.print.TemuPrintDataKeyRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOpenapiShopDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderShippingInfoDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOpenapiShopMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderShippingMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopMapper;
import cn.iocoder.yudao.module.temu.service.deliveryOrder.TemuDeliveryOrderConvertService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.temu.utils.openapi.TemuOpenApiUtil;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderQueryReqVO;
import java.util.function.Function;
import org.springframework.util.StringUtils;
import cn.iocoder.yudao.module.temu.mq.producer.weixin.WeiXinProducer;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemuDeliveryOrderConvertServiceImpl implements TemuDeliveryOrderConvertService {

    private final TemuOpenapiShopMapper temuOpenapiShopMapper;
    private final TemuOrderShippingMapper shippingInfoMapper;
    private final TemuOrderMapper orderMapper;
    private final TemuShopMapper temuShopMapper;
    private final WeiXinProducer weiXinProducer;

    /**
     * 查询Temu平台物流信息，并将结果转换为VO列表
     * 功能说明：
     * 1. 通过Temu开放平台的API获取物流订单列表
     * 2. 将返回的JSON数据转换为Java实体对象列表
     * @param reqVO 查询参数
     * @return 物流信息VO列表
     */
    public PageResult<TemuDeliveryOrderSimpleVO> queryTemuLogisticsPage(TemuDeliveryOrderQueryReqVO reqVO) {
        try {
            // 1. 获取shopId，如果没有则使用默认值
            String shopId = (reqVO.getShopId() != null && !reqVO.getShopId().isEmpty())
                    ? reqVO.getShopId()
                    : "634418222478497";
            // 2. 查询店铺信息
            TemuOpenapiShopDO shop = temuOpenapiShopMapper.selectByShopId(shopId);
            if (shop == null) {
                throw new RuntimeException("未找到对应的店铺信息，shopId=" + shopId);
            }
            // 3. 初始化Temu开放API工具，并赋值appKey、appSecret、accessToken
            TemuOpenApiUtil openApiUtil = new TemuOpenApiUtil();
            openApiUtil.setAppKey(shop.getAppKey());
            openApiUtil.setAppSecret(shop.getAppSecret());
            openApiUtil.setAccessToken(shop.getToken());
            openApiUtil.setBaseUrl("https://openapi.kuajingmaihuo.com/openapi/router");

            // 2. 执行API请求获取物流订单（新版V2接口）
            String apiResult = openApiUtil.getShipOrderListv2(reqVO);
            // 3. 配置Jackson JSON解析器
            ObjectMapper objectMapper = new ObjectMapper();
            // 4. 解析API返回的JSON数据结构
            JsonNode rootNode = objectMapper.readTree(apiResult);
            List<TemuDeliveryOrderSimpleVO> voList = new ArrayList<>();
            // 5. 提取结果中的订单列表节点
            JsonNode resultNode = rootNode.path("result");
            JsonNode list = resultNode.path("list");
            // 6. 遍历订单列表并转换为VO对象
            Long total = resultNode.path("total").asLong(0L);
            Integer pageNo = reqVO.getPageNo();
            Integer pageSize = reqVO.getPageSize();
            if (list.isArray()) {
                for (JsonNode item : list) {
                    voList.add(this.convert(item));
                }
            }
            return new PageResult<>(voList, total, pageNo, pageSize);
        } catch (Exception e) {
            throw new RuntimeException("物流查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将单个JsonNode对象转换为TemuDeliveryOrderSimpleVO对象
     * @param item 单个物流订单的JsonNode
     * @return 转换后的VO对象
     */
    public TemuDeliveryOrderSimpleVO convert(JsonNode item) {
        TemuDeliveryOrderSimpleVO vo = new TemuDeliveryOrderSimpleVO();
        vo.setDeliveryOrderSn(item.path("deliveryOrderSn").asText());
        vo.setExpressDeliverySn(item.path("expressDeliverySn").asText());
        vo.setExpressCompany(item.path("expressCompany").asText());
        vo.setExpressCompanyId(item.path("expressCompanyId").asLong());
        vo.setSupplierId(item.path("supplierId").asLong());
        vo.setSubPurchaseOrderSn(item.path("subPurchaseOrderSn").asText());
        vo.setUrgencyType(item.path("urgencyType").asInt());
        vo.setDeliverPackageNum(item.path("deliverPackageNum").asInt());
        vo.setReceivePackageNum(item.path("receivePackageNum").asInt());
        vo.setPredictTotalPackageWeight(item.path("predictTotalPackageWeight").asInt());
        vo.setStatus(item.path("status").asInt());
        vo.setDeliverTime(item.path("deliverTime").asLong());
        vo.setReceiveTime(item.path("receiveTime").isNull() ? null : item.path("receiveTime").asLong());
        vo.setSubWarehouseName(item.path("subWarehouseName").asText());
        vo.setPurchaseTime(item.path("purchaseTime").asLong());
        vo.setExpectPickUpGoodsTime(item.path("expectPickUpGoodsTime").isNull() ? null : item.path("expectPickUpGoodsTime").asLong());
        vo.setProductSkcId(item.path("productSkcId").asText());
        vo.setExpressBatchSn(item.path("expressBatchSn").asText());

        // 新增图片字段赋值
        if (item.has("subPurchaseOrderBasicVO") && !item.get("subPurchaseOrderBasicVO").isNull()) {
            JsonNode subVO = item.get("subPurchaseOrderBasicVO");
            vo.setProductSkcPicture(subVO.path("productSkcPicture").asText());
            vo.setPurchaseQuantity(subVO.path("purchaseQuantity").asInt());

        }

        // 包裹列表
        List<TemuDeliveryOrderSimpleVO.PackageVO> packageList = new ArrayList<>();
        if (item.has("packageList") && item.get("packageList").isArray()) {
            for (JsonNode pkg : item.get("packageList")) {
                TemuDeliveryOrderSimpleVO.PackageVO pkgVO = new TemuDeliveryOrderSimpleVO.PackageVO();
                pkgVO.setSkcNum(pkg.path("skcNum").asInt());
                pkgVO.setPackageSn(pkg.path("packageSn").asText());
                packageList.add(pkgVO);
            }
        }
        vo.setPackageList(packageList);

        // 包裹详情
        List<TemuDeliveryOrderSimpleVO.PackageDetailVO> packageDetailList = new ArrayList<>();
        if (item.has("packageDetailList") && item.get("packageDetailList").isArray()) {
            for (JsonNode detail : item.get("packageDetailList")) {
                TemuDeliveryOrderSimpleVO.PackageDetailVO detailVO = new TemuDeliveryOrderSimpleVO.PackageDetailVO();
                detailVO.setProductSkuId(detail.path("productSkuId").asLong());
                detailVO.setSkuNum(detail.path("skuNum").asInt());
                packageDetailList.add(detailVO);
            }
        }
        vo.setPackageDetailList(packageDetailList);

        // 收货地址
        if (item.has("receiveAddressInfo") && !item.get("receiveAddressInfo").isNull()) {
            JsonNode addr = item.get("receiveAddressInfo");
            TemuDeliveryOrderSimpleVO.ReceiveAddressInfoVO addrVO = new TemuDeliveryOrderSimpleVO.ReceiveAddressInfoVO();
            addrVO.setReceiverName(addr.path("receiverName").asText());
            addrVO.setPhone(addr.path("phone").asText());
            addrVO.setProvinceName(addr.path("provinceName").asText());
            addrVO.setCityName(addr.path("cityName").asText());
            addrVO.setDistrictName(addr.path("districtName").asText());
            addrVO.setDetailAddress(addr.path("detailAddress").asText());
            vo.setReceiveAddressInfo(addrVO);
        }

        return vo;
    }

    /**
     * 查询物流面单信息
     * @param reqVO 查询参数
     * @return
     */
    @Override
    public List<TemuBoxMarkRespVO> queryBoxMark(TemuBoxMarkQueryReqVO reqVO) {
        try {
            // 1. 获取默认店铺信息
            TemuOpenapiShopDO shop = temuOpenapiShopMapper.selectByShopId("634418222478497");
            if (shop == null) {
                throw new RuntimeException("未找到默认店铺信息");
            }

            // 2. 初始化Temu开放API工具
            TemuOpenApiUtil openApiUtil = new TemuOpenApiUtil();
            openApiUtil.setAppKey(shop.getAppKey());
            openApiUtil.setAppSecret(shop.getAppSecret());
            openApiUtil.setAccessToken(shop.getToken());
            openApiUtil.setBaseUrl("https://openapi.kuajingmaihuo.com/openapi/router");

            // 3. 构建API请求参数
            Map<String, Object> params = new HashMap<>();
            params.put("type", "bg.logistics.boxmarkinfo.get");
            params.put("deliveryOrderSnList", reqVO.getDeliveryOrderSnList());

            // 4. 执行API请求
            String apiResult = openApiUtil.callApi(params);

            // 5. 解析API返回结果
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(apiResult);
            
            // 6. 检查API调用是否成功
            if (!rootNode.path("success").asBoolean()) {
                throw new RuntimeException("箱唛查询失败: " + rootNode.path("errorMsg").asText());
            }

            // 7. 转换结果
            List<TemuBoxMarkRespVO> resultList = new ArrayList<>();
            JsonNode resultNode = rootNode.path("result");
            if (resultNode.isArray()) {
                for (JsonNode item : resultNode) {
                    TemuBoxMarkRespVO vo = convertToBoxMarkVO(item);
                    resultList.add(vo);
                }
            }

            return resultList;
        } catch (Exception e) {
            throw new RuntimeException("箱唛查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将JsonNode转换为BoxMarkVO对象
     */
    private TemuBoxMarkRespVO convertToBoxMarkVO(JsonNode item) {
        TemuBoxMarkRespVO vo = new TemuBoxMarkRespVO();
        
        // 设置基本字段
        vo.setVolumeType(item.path("volumeType").asInt());
        vo.setSupplierId(item.path("supplierId").asInt());
        vo.setIsCustomProduct(item.path("isCustomProduct").asBoolean());
        vo.setDeliveryMethod(item.path("deliveryMethod").asInt());
        vo.setPackageIndex(item.path("packageIndex").asInt());
        vo.setExpressDeliverySn(item.path("expressDeliverySn").asText());
        vo.setProductName(item.path("productName").asText());
        vo.setSubWarehouseEnglishName(item.path("subWarehouseEnglishName").asText());
        vo.setIsClothCat(item.path("isClothCat").asBoolean());
        vo.setIsFirst(item.path("isFirst").asBoolean());
        vo.setPurchaseStockType(item.path("purchaseStockType").asInt());
        vo.setTotalPackageNum(item.path("totalPackageNum").asInt());
        vo.setExpressCompany(item.path("expressCompany").asText());
        vo.setProductSkcId(item.path("productSkcId").asInt());
        vo.setNonClothSkuExtCode(item.path("nonClothSkuExtCode").asText());
        vo.setDeliveryOrderSn(item.path("deliveryOrderSn").asText());
        vo.setSupplierName(item.path("supplierName").asText());
        vo.setSettlementType(item.path("settlementType").asInt());
        vo.setSkcExtCode(item.path("skcExtCode").asText());
        vo.setDeliverTime(item.path("deliverTime").asLong());
        vo.setSubWarehouseId(item.path("subWarehouseId").asInt());
        vo.setUrgencyType(item.path("urgencyType").asInt());
        vo.setProductSkcName(item.path("productSkcName").asText());
        vo.setPackageSn(item.path("packageSn").asText());
        vo.setExpressEnglishCompany(item.path("expressEnglishCompany").asText());
        vo.setPackageSkcNum(item.path("packageSkcNum").asInt());
        vo.setSubWarehouseName(item.path("subWarehouseName").asText());
        vo.setSubPurchaseOrderSn(item.path("subPurchaseOrderSn").asText());
        vo.setDriverName(item.path("driverName").asText());
        vo.setDriverPhone(item.path("driverPhone").asText());
        vo.setPurchaseTime(item.path("purchaseTime").asLong());
        vo.setStorageAttrName(item.path("storageAttrName").asText());
        vo.setDeliverSkcNum(item.path("deliverSkcNum").asInt());
        vo.setDeliveryStatus(item.path("deliveryStatus").asInt());

        // 处理productSkuIdList
        List<Integer> skuIdList = new ArrayList<>();
        JsonNode skuIdListNode = item.path("productSkuIdList");
        if (skuIdListNode.isArray()) {
            for (JsonNode skuId : skuIdListNode) {
                skuIdList.add(skuId.asInt());
            }
        }
        vo.setProductSkuIdList(skuIdList);

        // 处理greyKeyHitMap
        Map<String, Boolean> greyKeyHitMap = new HashMap<>();
        JsonNode greyKeyHitMapNode = item.path("greyKeyHitMap");
        if (greyKeyHitMapNode.isObject()) {
            greyKeyHitMapNode.fields().forEachRemaining(entry -> 
                greyKeyHitMap.put(entry.getKey(), entry.getValue().asBoolean())
            );
        }
        vo.setGreyKeyHitMap(greyKeyHitMap);

        // 处理nonClothMainSpecVOList
        vo.setNonClothMainSpecVOList(convertSpecVOList(item.path("nonClothMainSpecVOList")));

        // 处理nonClothSecondarySpecVOList
        vo.setNonClothSecondarySpecVOList(convertSpecVOList(item.path("nonClothSecondarySpecVOList")));

        return vo;
    }

    /**
     * 转换规格列表
     */
    private List<TemuBoxMarkRespVO.SpecVO> convertSpecVOList(JsonNode specListNode) {
        List<TemuBoxMarkRespVO.SpecVO> specList = new ArrayList<>();
        if (specListNode.isArray()) {
            for (JsonNode specNode : specListNode) {
                TemuBoxMarkRespVO.SpecVO specVO = new TemuBoxMarkRespVO.SpecVO();
                specVO.setSpecId(specNode.path("specId").asInt());
                specVO.setParentSpecName(specNode.path("parentSpecName").asText());
                specVO.setParentSpecId(specNode.path("parentSpecId").asInt());
                specVO.setSpecName(specNode.path("specName").asText());
                specList.add(specVO);
            }
        }
        return specList;
    }

    /**
     * 查询定制sku条码信息
     * @param reqVO 查询参数
     * @return
     */
    @Override
    public TemuCustomGoodsLabelRespVO queryCustomGoodsLabel(TemuCustomGoodsLabelQueryReqVO reqVO) {
        try {
            // 1. 获取默认店铺信息
            TemuOpenapiShopDO shop = temuOpenapiShopMapper.selectByShopId("634418222478497");
            if (shop == null) {
                throw new RuntimeException("未找到默认店铺信息");
            }

            // 2. 初始化Temu开放API工具
            TemuOpenApiUtil openApiUtil = new TemuOpenApiUtil();
            openApiUtil.setAppKey(shop.getAppKey());
            openApiUtil.setAppSecret(shop.getAppSecret());
            openApiUtil.setAccessToken(shop.getToken());
            openApiUtil.setBaseUrl("https://openapi.kuajingmaihuo.com/openapi/router");

            // 3. 构建API请求参数
            Map<String, Object> params = new HashMap<>();
            params.put("type", "bg.goods.custom.label.get");
            if (reqVO.getProductSkuIdList() != null) params.put("productSkuIdList", reqVO.getProductSkuIdList());
            if (reqVO.getProductSkcIdList() != null) params.put("productSkcIdList", reqVO.getProductSkcIdList());
            if (reqVO.getPersonalProductSkuIdList() != null) params.put("personalProductSkuIdList", reqVO.getPersonalProductSkuIdList());
            if (reqVO.getCreateTimeEnd() != null) params.put("createTimeEnd", reqVO.getCreateTimeEnd());
            if (reqVO.getPageSize() != null) params.put("pageSize", reqVO.getPageSize());
            if (reqVO.getPage() != null) params.put("page", reqVO.getPage());
            if (reqVO.getCreateTimeStart() != null) params.put("createTimeStart", reqVO.getCreateTimeStart());
            if (reqVO.getLabelCode() != null) params.put("labelCode", reqVO.getLabelCode());

            // 4. 执行API请求
            String apiResult = openApiUtil.callApi(params);

            // 5. 解析API返回结果
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(apiResult);
            
            // 6. 检查API调用是否成功
            if (!rootNode.path("success").asBoolean()) {
                throw new RuntimeException("定制品商品条码查询失败: " + rootNode.path("errorMsg").asText());
            }

            // 7. 转换结果
            TemuCustomGoodsLabelRespVO respVO = objectMapper.treeToValue(rootNode.path("result"), TemuCustomGoodsLabelRespVO.class);
            return respVO;

        } catch (Exception e) {
            throw new RuntimeException("定制品商品条码查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取物流面单打印信息
     * @param reqVO 查询参数
     * @return
     */
    @Override
    public TemuPrintDataKeyRespVO getBoxMarkPrintDataKey(TemuBoxMarkQueryReqVO reqVO) {
        try {
            // 1. 获取默认店铺信息
            TemuOpenapiShopDO shop = temuOpenapiShopMapper.selectByShopId("634418222478497");
            if (shop == null) {
                throw new RuntimeException("未找到默认店铺信息");
            }

            // 2. 初始化Temu开放API工具
            TemuOpenApiUtil openApiUtil = new TemuOpenApiUtil();
            openApiUtil.setAppKey(shop.getAppKey());
            openApiUtil.setAppSecret(shop.getAppSecret());
            openApiUtil.setAccessToken(shop.getToken());
            openApiUtil.setBaseUrl("https://openapi.kuajingmaihuo.com/openapi/router");

            // 3. 构建API请求参数
            Map<String, Object> params = new HashMap<>();
            params.put("type", "bg.logistics.boxmarkinfo.get");
            params.put("deliveryOrderSnList", reqVO.getDeliveryOrderSnList());
            params.put("return_data_key", true);

            // 4. 执行API请求
            String apiResult = openApiUtil.callApi(params);

            // 5. 解析API返回结果
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(apiResult);
            
            // 6. 检查API调用是否成功
            if (!rootNode.path("success").asBoolean()) {
                throw new RuntimeException("获取物流面单打印数据Key失败: " + rootNode.path("errorMsg").asText());
            }

            // 7. 转换结果
            TemuPrintDataKeyRespVO respVO = new TemuPrintDataKeyRespVO();
            respVO.setDataKey(rootNode.path("result").asText());
            return respVO;

        } catch (Exception e) {
            throw new RuntimeException("获取物流面单打印数据Key失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取定制sku条码打印信息
     * @param reqVO 查询参数
     * @return
     */
    @Override
    public TemuPrintDataKeyRespVO getCustomGoodsLabelPrintDataKey(TemuCustomGoodsLabelQueryReqVO reqVO) {
        try {
            // 1. 获取默认店铺信息
            TemuOpenapiShopDO shop = temuOpenapiShopMapper.selectByShopId("634418222478497");
            if (shop == null) {
                throw new RuntimeException("未找到默认店铺信息");
            }

            // 2. 初始化Temu开放API工具
            TemuOpenApiUtil openApiUtil = new TemuOpenApiUtil();
            openApiUtil.setAppKey(shop.getAppKey());
            openApiUtil.setAppSecret(shop.getAppSecret());
            openApiUtil.setAccessToken(shop.getToken());
            openApiUtil.setBaseUrl("https://openapi.kuajingmaihuo.com/openapi/router");

            // 3. 构建API请求参数
            Map<String, Object> params = new HashMap<>();
            params.put("type", "bg.goods.custom.label.get");
            if (reqVO.getProductSkuIdList() != null) params.put("productSkuIdList", reqVO.getProductSkuIdList());
            if (reqVO.getProductSkcIdList() != null) params.put("productSkcIdList", reqVO.getProductSkcIdList());
            if (reqVO.getPersonalProductSkuIdList() != null) params.put("personalProductSkuIdList", reqVO.getPersonalProductSkuIdList());
            if (reqVO.getCreateTimeEnd() != null) params.put("createTimeEnd", reqVO.getCreateTimeEnd());
            if (reqVO.getPageSize() != null) params.put("pageSize", reqVO.getPageSize());
            if (reqVO.getPage() != null) params.put("page", reqVO.getPage());
            if (reqVO.getCreateTimeStart() != null) params.put("createTimeStart", reqVO.getCreateTimeStart());
            if (reqVO.getLabelCode() != null) params.put("labelCode", reqVO.getLabelCode());
            params.put("return_data_key", true);

            // 4. 执行API请求
            String apiResult = openApiUtil.callApi(params);

            // 5. 解析API返回结果
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(apiResult);
            
            // 6. 检查API调用是否成功
            if (!rootNode.path("success").asBoolean()) {
                throw new RuntimeException("获取定制sku条码打印数据Key失败: " + rootNode.path("errorMsg").asText());
            }

            // 7. 转换结果
            TemuPrintDataKeyRespVO respVO = new TemuPrintDataKeyRespVO();
            respVO.setDataKey(rootNode.path("result").asText());
            return respVO;

        } catch (Exception e) {
            throw new RuntimeException("获取定制sku条码打印数据Key失败: " + e.getMessage(), e);
        }
    }

    /**
     * 校验temu平台和erp的物流信息是否一致
     * @param trackingNumbers 物流单号列表
     * @return
     */
    @Override
    public TemuOrderTrackingValidateRespVO validateTrackingNumber(List<String> trackingNumbers) {
        TemuOrderTrackingValidateRespVO respVO = new TemuOrderTrackingValidateRespVO();
        respVO.setSuccess(true);
        StringBuilder errorMsg = new StringBuilder();

        try {
            // 1. 获取Temu平台数据
            PageResult<TemuDeliveryOrderSimpleVO> temuResult = getTemuPlatformData(trackingNumbers);
            
            // 1.1 检查哪些物流单号在Temu平台查不到数据
            Set<String> temuTrackingNumbers = temuResult.getList().stream()
                    .map(TemuDeliveryOrderSimpleVO::getExpressDeliverySn)
                    .collect(Collectors.toSet());
            
            List<String> notFoundTrackingNumbers = trackingNumbers.stream()
                    .filter(trackingNumber -> !temuTrackingNumbers.contains(trackingNumber))
                    .collect(Collectors.toList());
            
            if (!notFoundTrackingNumbers.isEmpty()) {
                errorMsg.append("平台查询不到物流单号：")
                        .append(String.join("，", notFoundTrackingNumbers))
                        .append("；");
                respVO.setSuccess(false);
            }

            // 2. 获取本地物流信息
            List<TemuOrderShippingInfoDO> shippingInfos = getLocalShippingInfo(trackingNumbers);

            // 3. 收集物流信息的trackingNumber和orderNo映射关系
            Map<String, List<String>> temuTrackingToOrders = collectTemuTrackingOrders(temuResult);
            Map<String, List<String>> localTrackingToOrders = collectLocalTrackingOrders(shippingInfos);

            // 4. 校验每个在Temu平台能查到的物流单号
            for (String trackingNumber : trackingNumbers) {
                // 跳过在Temu平台查不到的物流单号
                if (notFoundTrackingNumbers.contains(trackingNumber)) {
                    continue;
                }
                validateSingleTrackingNumber(trackingNumber, temuResult, temuTrackingToOrders,
                        localTrackingToOrders, errorMsg, respVO);
            }

            if (!respVO.getSuccess()) {
                respVO.setErrorMessage(errorMsg.toString());
                // 发送企业微信告警
                sendWeChatAlert(errorMsg.toString());
            }

            // 设置返回结果
            respVO.setTrackingNumberToOrderNos(temuTrackingToOrders);
            respVO.setTrackingNumberToSkus(collectTemuTrackingSkus(temuResult));

        } catch (Exception e) {
            respVO.setSuccess(false);
            respVO.setErrorMessage("校验失败：" + e.getMessage());
            log.error("[validateTrackingNumber] 验证物流单号时发生错误，物流单号列表：{}", trackingNumbers, e);
            // 发送企业微信告警
            sendWeChatAlert("校验失败：" + e.getMessage());
        }

        return respVO;
    }

    /**
     * 获取Temu平台数据
     */
    private PageResult<TemuDeliveryOrderSimpleVO> getTemuPlatformData(List<String> trackingNumbers) {
        try {
            TemuDeliveryOrderQueryReqVO queryReqVO = new TemuDeliveryOrderQueryReqVO();
            queryReqVO.setExpressDeliverySnList(trackingNumbers);
            return queryTemuLogisticsPage(queryReqVO);
        } catch (RuntimeException e) {
            throw new RuntimeException(String.format(e.getMessage()), e);
        }
    }

    /**
     * 获取本地物流信息
     */
    private List<TemuOrderShippingInfoDO> getLocalShippingInfo(List<String> trackingNumbers) {
        return shippingInfoMapper.selectList(
                new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
                        .in(TemuOrderShippingInfoDO::getTrackingNumber, trackingNumbers));
    }

    /**
     * 收集Temu平台的物流单号与备货单号的映射关系
     */
    private Map<String, List<String>> collectTemuTrackingOrders(PageResult<TemuDeliveryOrderSimpleVO> temuResult) {
        Map<String, List<String>> temuTrackingToOrders = new HashMap<>();
        for (TemuDeliveryOrderSimpleVO delivery : temuResult.getList()) {
            temuTrackingToOrders.computeIfAbsent(delivery.getExpressDeliverySn(), k -> new ArrayList<>())
                    .add(delivery.getSubPurchaseOrderSn());
        }
        return temuTrackingToOrders;
    }

    /**
     * 收集本地的物流单号与备货单号的映射关系
     */
    private Map<String, List<String>> collectLocalTrackingOrders(List<TemuOrderShippingInfoDO> shippingInfos) {
        return shippingInfos.stream()
                .collect(Collectors.groupingBy(
                        TemuOrderShippingInfoDO::getTrackingNumber,
                        Collectors.mapping(TemuOrderShippingInfoDO::getOrderNo, Collectors.toList())
                ));
    }

    /**
     * 收集Temu平台的物流单号与定制SKU（官网数量）信息的映射关系
     */
    private Map<String, List<TemuOrderTrackingValidateRespVO.SkuInfo>> collectTemuTrackingSkus(
            PageResult<TemuDeliveryOrderSimpleVO> temuResult) {
        Map<String, List<TemuOrderTrackingValidateRespVO.SkuInfo>> temuTrackingToSkus = new HashMap<>();
        for (TemuDeliveryOrderSimpleVO delivery : temuResult.getList()) {
            List<TemuOrderTrackingValidateRespVO.SkuInfo> skuInfos = delivery.getPackageDetailList().stream()
                    .filter(detail -> detail.getSkuNum() > 0)  // 过滤掉数量为0的SKU
                    .map(detail -> {
                        TemuOrderTrackingValidateRespVO.SkuInfo skuInfo = new TemuOrderTrackingValidateRespVO.SkuInfo();
                        skuInfo.setProductSkuId(detail.getProductSkuId());
                        skuInfo.setSkuNum(detail.getSkuNum());
                        return skuInfo;
                    })
                    .collect(Collectors.toList());
            temuTrackingToSkus.put(delivery.getExpressDeliverySn(), skuInfos);
        }
        return temuTrackingToSkus;
    }

    /**
     * 校验单个物流单号
     */
    private void validateSingleTrackingNumber(String trackingNumber,
            PageResult<TemuDeliveryOrderSimpleVO> temuResult,
            Map<String, List<String>> temuTrackingToOrders,
            Map<String, List<String>> localTrackingToOrders,
            StringBuilder errorMsg,
            TemuOrderTrackingValidateRespVO respVO) {
        
        List<String> temuOrders = temuTrackingToOrders.getOrDefault(trackingNumber, Collections.emptyList());
        List<String> localOrderNos = localTrackingToOrders.getOrDefault(trackingNumber, Collections.emptyList());

        // 校验订单数量
        if (temuOrders.size() != localOrderNos.size()) {
            errorMsg.append(String.format("物流单号：%s-备货单数量不匹配：平台%d个，本地%d个；",
                    trackingNumber, temuOrders.size(), localOrderNos.size()));
            respVO.setSuccess(false);
            return;
        }

        // 校验订单内容
        if (!new HashSet<>(temuOrders).equals(new HashSet<>(localOrderNos))) {
            Set<String> temuOrderSet = new HashSet<>(temuOrders);
            Set<String> localOrderSet = new HashSet<>(localOrderNos);
            Set<String> temuOnly = new HashSet<>(temuOrderSet);
            temuOnly.removeAll(localOrderSet);
            Set<String> localOnly = new HashSet<>(localOrderSet);
            localOnly.removeAll(temuOrderSet);

            if (!temuOnly.isEmpty()) {
                errorMsg.append(String.format("物流单号：%s-本地缺少备货单：%s；",
                        trackingNumber, String.join("，", temuOnly)));
            }
            if (!localOnly.isEmpty()) {
                errorMsg.append(String.format("物流单号：%s-本地多出备货单：%s；",
                        trackingNumber, String.join("，", localOnly)));
            }
            respVO.setSuccess(false);
            return;
        }

        // 3. 校验定制sku信息
        if (respVO.getSuccess()) {
            validateSkuInfo(trackingNumber, temuResult, temuOrders, errorMsg, respVO);
        }
    }

    /**
     * 校验定制SKU信息
     */
    private void validateSkuInfo(String trackingNumber, PageResult<TemuDeliveryOrderSimpleVO> temuResult,
            List<String> temuOrders, StringBuilder errorMsg, TemuOrderTrackingValidateRespVO respVO) {
        // 获取Temu平台和本地的定制SKU信息
        Map<String, List<TemuDeliveryOrderSimpleVO.PackageDetailVO>> temuOrderSkus = getTemuOrderSkus(temuResult, trackingNumber);
        List<TemuOrderDO> orderList = getLocalOrders(temuOrders);
        Map<String, List<TemuOrderDO>> localOrderSkus = orderList.stream()
                .collect(Collectors.groupingBy(TemuOrderDO::getOrderNo));

        StringBuilder skuDiffMsg = new StringBuilder();
        boolean hasSkuError = false;

        // 按orderNo逐个比对
        for (String orderNo : temuOrders) {
            if (validateOrderSkus(trackingNumber, orderNo, temuOrderSkus.get(orderNo),
                    localOrderSkus.get(orderNo), skuDiffMsg)) {
                hasSkuError = true;
            }
        }

        if (hasSkuError) {
            errorMsg.append(skuDiffMsg);
            respVO.setSuccess(false);
        }
    }

    /**
     * 获取Temu平台备货单的定制SKU信息
     */
    private Map<String, List<TemuDeliveryOrderSimpleVO.PackageDetailVO>> getTemuOrderSkus(
            PageResult<TemuDeliveryOrderSimpleVO> temuResult, String trackingNumber) {
        Map<String, List<TemuDeliveryOrderSimpleVO.PackageDetailVO>> temuOrderSkus = new HashMap<>();
        for (TemuDeliveryOrderSimpleVO delivery : temuResult.getList()) {
            if (delivery.getExpressDeliverySn().equals(trackingNumber)) {
                // 过滤掉skuNum为0的数据
                List<TemuDeliveryOrderSimpleVO.PackageDetailVO> validSkus = delivery.getPackageDetailList().stream()
                        .filter(detail -> detail.getSkuNum() > 0)
                        .collect(Collectors.toList());
                temuOrderSkus.put(delivery.getSubPurchaseOrderSn(), validSkus);
            }
        }
        return temuOrderSkus;
    }

    /**
     * 获取本地备货单信息
     */
    private List<TemuOrderDO> getLocalOrders(List<String> orderNos) {
        return orderMapper.selectList(
                new LambdaQueryWrapperX<TemuOrderDO>()
                        .in(TemuOrderDO::getOrderNo, orderNos));
    }

    /**
     * 校验单个备货单的定制SKU信息
     * @return 是否有错误
     */
    private boolean validateOrderSkus(String trackingNumber, String orderNo,
                                    List<TemuDeliveryOrderSimpleVO.PackageDetailVO> temuSkuList,
                                    List<TemuOrderDO> localSkuList,
                                    StringBuilder errorMsg) {
        // 如果任一方的SKU列表为null，说明数据不一致
        if (temuSkuList == null || localSkuList == null) {
            if (temuSkuList == null && localSkuList != null) {
                String localSkuInfo = localSkuList.stream()
                        .map(order -> String.format("SKU%s", order.getCustomSku()))
                        .collect(Collectors.joining("，"));
                errorMsg.append(String.format("物流单号：%s-备货单：%s-平台无定制SKU，erp有%s；",
                        trackingNumber, orderNo, localSkuInfo));
            } else if (temuSkuList != null && localSkuList == null) {
                List<TemuDeliveryOrderSimpleVO.PackageDetailVO> validTemuSkus = temuSkuList.stream()
                        .filter(detail -> detail.getSkuNum() > 0)
                        .collect(Collectors.toList());
                String temuSkuInfo = validTemuSkus.stream()
                        .map(detail -> String.format("定制SKU%d", detail.getProductSkuId()))
                        .collect(Collectors.joining("，"));
                errorMsg.append(String.format("物流单号：%s-备货单：%s-平台有%s，erp无定制SKU；",
                        trackingNumber, orderNo, temuSkuInfo));
            }
            return true;
        }

        // 过滤掉数量为0的SKU
        temuSkuList = temuSkuList.stream()
                .filter(detail -> detail.getSkuNum() > 0)
                .collect(Collectors.toList());

        // 1. 首先校验定制SKU的总数量（包括重复的）
        if (temuSkuList.size() != localSkuList.size()) {
            errorMsg.append(String.format("物流单号：%s-备货单号：%s-定制SKU总数不匹配：Temu平台%d个，erp%d个；",
                    trackingNumber, orderNo, temuSkuList.size(), localSkuList.size()));
            return true;
        }

        // 2. 校验定制SKU的内容是否一致
        List<Long> temuSkuIds = temuSkuList.stream()
                .map(TemuDeliveryOrderSimpleVO.PackageDetailVO::getProductSkuId)
                .sorted()
                .collect(Collectors.toList());

        List<Long> localSkuIds = localSkuList.stream()
                .map(order -> Long.parseLong(order.getCustomSku()))
                .sorted()
                .collect(Collectors.toList());

        if (!temuSkuIds.equals(localSkuIds)) {
            // 找出差异
            Map<Long, Long> temuSkuCount = temuSkuIds.stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            Map<Long, Long> localSkuCount = localSkuIds.stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            
            // 收集本地缺少的SKU
            List<String> missingSKUs = new ArrayList<>();
            // 收集本地多出的SKU
            List<String> extraSKUs = new ArrayList<>();

            // 以Temu平台数据为准进行比对
            for (Map.Entry<Long, Long> entry : temuSkuCount.entrySet()) {
                Long skuId = entry.getKey();
                Long temuCount = entry.getValue();
                Long localCount = localSkuCount.getOrDefault(skuId, 0L);
                
                if (localCount < temuCount) {
                    missingSKUs.add(String.format("定制SKU：%d(缺少%d个)", skuId, temuCount - localCount));
                } else if (localCount > temuCount) {
                    extraSKUs.add(String.format("定制SKU：%d(多出%d个)", skuId, localCount - temuCount));
                }
            }

            // 查找本地多出的SKU（在本地有但在Temu平台没有的）
            for (Map.Entry<Long, Long> entry : localSkuCount.entrySet()) {
                if (!temuSkuCount.containsKey(entry.getKey())) {
                    extraSKUs.add(String.format("定制SKU：%d(多出%d个)", entry.getKey(), entry.getValue()));
                }
            }

            // 组装错误信息
            errorMsg.append(String.format("物流单号：%s-备货单号：%s-定制SKU不一致：", trackingNumber, orderNo));
            if (!missingSKUs.isEmpty()) {
                errorMsg.append(String.format("缺少：%s", String.join("，", missingSKUs)));
            }
            if (!extraSKUs.isEmpty()) {
                if (!missingSKUs.isEmpty()) {
                    errorMsg.append("；");
                }
                errorMsg.append(String.format("多出：%s", String.join("，", extraSKUs)));
            }
            errorMsg.append("；");
            return true;
        }

        // 3. 校验每个定制SKU的官网数量
        Map<Long, Integer> temuSkuQuantities = temuSkuList.stream()
                .collect(Collectors.groupingBy(
                        TemuDeliveryOrderSimpleVO.PackageDetailVO::getProductSkuId,
                        Collectors.summingInt(TemuDeliveryOrderSimpleVO.PackageDetailVO::getSkuNum)));

        Map<Long, Integer> localSkuQuantities = localSkuList.stream()
                .collect(Collectors.groupingBy(
                        order -> Long.parseLong(order.getCustomSku()),
                        Collectors.summingInt(TemuOrderDO::getOriginalQuantity)));

        boolean hasQuantityError = false;
        for (Map.Entry<Long, Integer> entry : temuSkuQuantities.entrySet()) {
            Long skuId = entry.getKey();
            Integer temuQuantity = entry.getValue();
            Integer localQuantity = localSkuQuantities.get(skuId);

            if (!temuQuantity.equals(localQuantity)) {
                hasQuantityError = true;
                if (temuQuantity > localQuantity) {
                    errorMsg.append(String.format("物流单号：%s-备货单号：%s-定制SKU：%d-官网数量缺少%d个；",
                            trackingNumber, orderNo, skuId, temuQuantity - localQuantity));
                } else {
                    errorMsg.append(String.format("物流单号：%s-备货单号：%s-定制SKU：%d-官网数量多出%d个；",
                            trackingNumber, orderNo, skuId, localQuantity - temuQuantity));
                }
            }
        }

        return hasQuantityError;
    }

    /**
     * 发送企业微信告警
     *
     * @param message 告警消息
     */
    private void sendWeChatAlert(String message) {
        try {
            // 1. 获取shopId为888888的店铺webhook地址
            TemuShopDO webhookShop = temuShopMapper.selectByShopId(888888L);
            if (webhookShop == null || webhookShop.getWebhook() == null) {
                log.error("[sendWeChatAlert] 未配置告警webhook地址");
                return;
            }

            // 2. 构建告警消息
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("⚠️ 物流校验告警！\n");

            // 3. 格式化消息内容
            String[] alerts = message.split(";");
            for (String alert : alerts) {
                if (StringUtils.hasText(alert)) {
                    // 将每条告警信息的不同部分用换行分隔
                    String[] parts = alert.trim().split("-");
                    for (String part : parts) {
                        if (StringUtils.hasText(part)) {
                            messageBuilder.append(part.trim()).append("\n");
                        }
                    }
                }
            }


            // 4. 发送企业微信告警
            weiXinProducer.sendMessage(webhookShop.getWebhook(), messageBuilder.toString());
            log.info("[sendWeChatAlert] 发送企业微信告警成功");
        } catch (Exception e) {
            log.error("[sendWeChatAlert] 发送企业微信告警失败", e);
        }
    }
}