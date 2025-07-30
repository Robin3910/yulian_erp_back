package cn.iocoder.yudao.module.temu.service.deliveryOrder.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderSimpleVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuBoxMarkQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuBoxMarkRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.goods.TemuCustomGoodsLabelQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.goods.TemuCustomGoodsLabelRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderTrackingValidateRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.print.TemuPrintDataKeyRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOpenapiShopDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderShippingInfoDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOpenapiShopMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderShippingMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopMapper;
import cn.iocoder.yudao.module.temu.service.deliveryOrder.TemuDeliveryOrderConvertService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.nashorn.internal.runtime.regexp.joni.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.temu.utils.openapi.TemuOpenApiUtil;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderQueryReqVO;
import org.springframework.util.StringUtils;
import cn.iocoder.yudao.module.temu.mq.producer.weixin.WeiXinProducer;
import lombok.Data;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemuDeliveryOrderConvertServiceImpl implements TemuDeliveryOrderConvertService {

    private final TemuOpenapiShopMapper temuOpenapiShopMapper;
    private final TemuOrderShippingMapper shippingInfoMapper;
    private final TemuOrderMapper orderMapper;
    private final TemuShopMapper temuShopMapper;
    private final WeiXinProducer weiXinProducer;
    private final ConfigApi configApi;

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
        vo.setShopId(item.path("supplierId").asLong());
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

    // ======================= 【主方法】物流校验相关 =======================
    /**
     * 校验temu平台和erp的物流信息是否一致
     * 主要校验逻辑：
     * 1. 按shopId分组处理物流单号
     * 2. 对每个shopId：
     *   2.1 调用Temu API获取平台数据
     *   2.2 查询本地数据库获取ERP数据
     *   2.3 对比平台和本地数据的差异
     * 3. 如有差异，发送企业微信告警
     * @param shopTrackingNumbers 按shopId分组的物流单号列表，key为shopId，value为该shopId下的物流单号列表
     * @return 校验结果，包含成功/失败状态、错误信息、物流单号与备货单号的映射等
     */
    @Override
    public TemuOrderTrackingValidateRespVO validateTrackingNumber(Map<String, Set<String>> shopTrackingNumbers) {
        TemuOrderTrackingValidateRespVO respVO = new TemuOrderTrackingValidateRespVO();
        respVO.setSuccess(true);
        StringBuilder errorMsg = new StringBuilder();

        try {
            // 存储所有平台返回的结果
            ValidationResults results = new ValidationResults();
            // 遍历每个shopId，分别校验其物流单号
            for (Map.Entry<String, Set<String>> entry : shopTrackingNumbers.entrySet()) {
                String shopId = entry.getKey();
                // 从配置中获取需要校验的shopId列表
                String configShopIds = configApi.getConfigValueByKey("temu.logistics.validate.shop.ids");
                Set<String> validateShopIds = new HashSet<>();
                if (StringUtils.hasText(configShopIds)) {
                    // 将配置的shopId字符串转换为Set集合（支持多个shopId，用逗号分隔）
                    validateShopIds.addAll(Arrays.asList(configShopIds.split(" ")));
                } else {
                    // 如果配置为空，保持原有逻辑，只校验默认店铺
                    validateShopIds.add("634418222478497");
                }
                // 判断当前shopId是否需要校验
                if (!validateShopIds.contains(shopId)) {
                    log.info("[validateTrackingNumber] 跳过未配置的店铺物流校验，shopId={}", shopId);
                    continue;
                }
                
                // 将物流单号分批处理，每批最多20个
                Set<String> trackingNumbers = entry.getValue();
                List<Set<String>> batches = new ArrayList<>();
                Iterator<String> iterator = trackingNumbers.iterator();
                Set<String> currentBatch = new HashSet<>();
                
                while (iterator.hasNext()) {
                    String trackingNumber = iterator.next();
                    currentBatch.add(trackingNumber);
                    
                    if (currentBatch.size() >= 20 || !iterator.hasNext()) {
                        batches.add(new HashSet<>(currentBatch));
                        currentBatch.clear();
                    }
                }
                
                // 处理每一批物流单号
                for (Set<String> batch : batches) {
                    validateShopTrackingNumbers(shopId, batch, results, errorMsg);
                    if (!results.isSuccess()) {
                        respVO.setSuccess(false);
                    }
                }
            }
            
            // 如果有错误信息，设置到响应中
            if (errorMsg.length() > 0) {
                respVO.setErrorMessage(errorMsg.toString());
            }
            // 设置物流单号与订单号的映射关系
            respVO.setTrackingNumberToOrderNos(results.getTemuTrackingToOrders());
            // 设置物流单号与SKU信息的映射关系
            respVO.setTrackingNumberToSkus(results.getTemuTrackingToSkus());
        } catch (Exception e) {
            handleGlobalException(e, shopTrackingNumbers, respVO);
        }
        return respVO;
    }

    /**
     * 校验单个店铺的物流单号集合
     * 核心步骤：
     * 1. 获取平台数据
     * 2. 检查平台查不到的物流单号
     * 3. 获取并校验本地数据
     * @param shopId 店铺ID
     * @param trackingNumbers 物流单号列表
     * @param results 校验结果收集器
     * @param errorMsg 错误信息收集器
     */
    private void validateShopTrackingNumbers(String shopId, Set<String> trackingNumbers, 
            ValidationResults results, StringBuilder errorMsg) {
        String shopName = getShopName(shopId);
        try {
            // 1. 获取平台数据
            PageResult<TemuDeliveryOrderSimpleVO> temuResult = getTemuPlatformData(shopId, trackingNumbers);

            // 2. 检查平台查不到的物流单号
            List<String> notFoundTrackingNumbers = checkNotFoundTrackingNumbers(
                    shopName, trackingNumbers, temuResult, errorMsg);
            if (!notFoundTrackingNumbers.isEmpty()) {
                results.setSuccess(false);
            }
            // 3. 获取本地数据并校验
            // 注意：即使有平台查不到的物流单号，我们仍然继续校验其他可以查到的物流单号
            validateLocalData(shopId, shopName, trackingNumbers, temuResult, results, errorMsg);
        } catch (RuntimeException e) {
            handleShopException(e, shopId, shopName, trackingNumbers, results, errorMsg);
        }
    }

    // 获取temuApi平台数据
    private PageResult<TemuDeliveryOrderSimpleVO> getTemuPlatformData(String shopId, Set<String> trackingNumbers) {
        TemuDeliveryOrderQueryReqVO queryReqVO = new TemuDeliveryOrderQueryReqVO();
        queryReqVO.setExpressDeliverySnList(trackingNumbers);
        queryReqVO.setShopId(shopId);
        // 设置足够大的页面大小，确保一次性获取所有数据
        queryReqVO.setPageSize(100);
        queryReqVO.setPageNo(1);
        return queryTemuLogisticsPage(queryReqVO);
    }

    // 校验本地数据
    private void validateLocalData(String shopId, String shopName, Set<String> trackingNumbers,
            PageResult<TemuDeliveryOrderSimpleVO> temuResult, ValidationResults results, StringBuilder errorMsg) {
        // 1. 获取本地物流信息
        List<TemuOrderShippingInfoDO> shippingInfos = shippingInfoMapper.selectList(
                new LambdaQueryWrapperX<TemuOrderShippingInfoDO>()
                        .in(TemuOrderShippingInfoDO::getTrackingNumber, trackingNumbers)
                        .eq(TemuOrderShippingInfoDO::getShopId, Long.parseLong(shopId)));
        // 2. 收集映射关系
        Map<String, List<String>> temuTrackingToOrders = collectTemuTrackingOrders(temuResult);
        Map<String, List<String>> localTrackingToOrders = collectLocalTrackingOrders(shippingInfos);
        // 3. 校验每个物流单号
        // 先获取平台能查到的物流单号
        Set<String> temuTrackingNumbers = temuResult.getList().stream()
                .map(TemuDeliveryOrderSimpleVO::getExpressDeliverySn)
                .collect(Collectors.toSet());
        for (String trackingNumber : trackingNumbers) {
            // 如果平台查不到该物流单号，跳过（因为已经在前面的checkNotFoundTrackingNumbers中处理过了）
            if (!temuTrackingNumbers.contains(trackingNumber)) {
                continue;
            }
            // 校验订单数量和内容
            if (validateSingleTrackingNumber(shopName, trackingNumber, 
                    temuTrackingToOrders, localTrackingToOrders, temuResult, errorMsg)) {
                results.setSuccess(false);
            }
        }
        // 4. 合并结果
        results.getTemuTrackingToOrders().putAll(temuTrackingToOrders);
        results.getTemuTrackingToSkus().putAll(collectTemuTrackingSkus(temuResult));
    }

    /**
     * 检查平台查不到的物流单号
     * 对于平台查不到的物流单号：
     * 1. 收集到notFoundTrackingNumbers列表
     * 2. 添加到错误信息
     * 3. 发送告警
     */
    private List<String> checkNotFoundTrackingNumbers(String shopName, Set<String> trackingNumbers,
            PageResult<TemuDeliveryOrderSimpleVO> temuResult, StringBuilder errorMsg) {
        Set<String> temuTrackingNumbers = temuResult.getList().stream()
                .map(TemuDeliveryOrderSimpleVO::getExpressDeliverySn)
                .collect(Collectors.toSet());

        List<String> notFoundTrackingNumbers = trackingNumbers.stream()
                .filter(trackingNumber -> !temuTrackingNumbers.contains(trackingNumber))
                .collect(Collectors.toList());

        if (!notFoundTrackingNumbers.isEmpty()) {
            String alertMsg = String.format("店铺：%s - 平台查询不到物流单号：%s",
                    shopName, String.join("，", notFoundTrackingNumbers));
            errorMsg.append(alertMsg).append("；");
            // 对每个未找到的物流单号发送告警
            for (String trackingNumber : notFoundTrackingNumbers) {
                sendWeChatAlert(String.format("店铺：%s\n物流单号：%s\n平台查询不到该物流单号",
                        shopName, trackingNumber));
            }
        }
        return notFoundTrackingNumbers;
    }
    /**
     * 校验单个物流单号
     * 校验内容：
     * 1. 校验备货单 总数是否匹配
     * 2. 校验备货单 内容是否一致
     * 3. 校验定制SKU 数量和内容是否匹配
     * @return 是否有错误
     */
    private boolean validateSingleTrackingNumber(String shopName, String trackingNumber,
            Map<String, List<String>> temuTrackingToOrders, Map<String, List<String>> localTrackingToOrders,
            PageResult<TemuDeliveryOrderSimpleVO> temuResult, StringBuilder errorMsg) {
        List<String> temuOrders = temuTrackingToOrders.get(trackingNumber);
        List<String> localOrders = localTrackingToOrders.get(trackingNumber);

        // 1. 校验备货单数量
        if (temuOrders == null || localOrders == null || temuOrders.size() != localOrders.size()) {
            String alertMsg = String.format("店铺：%s\n物流单号：%s\n备货单数量不匹配：平台%d个，本地%d个",
                    shopName, trackingNumber, 
                    temuOrders == null ? 0 : temuOrders.size(), 
                    localOrders == null ? 0 : localOrders.size());
            sendWeChatAlert(alertMsg);
            errorMsg.append(alertMsg).append("；");
            return true;
        }
        // 2. 校验备货单内容
        if (!new HashSet<>(temuOrders).equals(new HashSet<>(localOrders))) {
            Set<String> temuOnly = new HashSet<>(temuOrders);
            temuOnly.removeAll(new HashSet<>(localOrders));
            Set<String> localOnly = new HashSet<>(localOrders);
            localOnly.removeAll(new HashSet<>(temuOrders));

            StringBuilder alertMsg = new StringBuilder()
                    .append("店铺：").append(shopName).append("\n")
                    .append("物流单号：").append(trackingNumber).append("\n");

            if (!temuOnly.isEmpty()) {
                alertMsg.append("本地缺少备货单：").append(String.join("，", temuOnly)).append("\n");
            }
            if (!localOnly.isEmpty()) {
                alertMsg.append("本地多出备货单：").append(String.join("，", localOnly)).append("\n");
            }

            sendWeChatAlert(alertMsg.toString());
            errorMsg.append(alertMsg).append("；");
            return true;
        }

        // 3. 校验 定制SKU信息
        return validateSkuInfo(shopName, trackingNumber, temuOrders, temuResult, errorMsg);
    }
    /**
     * 校验SKU信息
     * 1. 获取平台定制SKU信息（过滤skuNum=0）
     * 2. 按备货单号分组统计定制SKU数量
     * 3. 比对每个备货单号下的定制SKU数量
     * 4. 如果数量一致，比对定制SKU内容
     */
    private boolean validateSkuInfo(String shopName, String trackingNumber, List<String> orderNos,
            PageResult<TemuDeliveryOrderSimpleVO> temuResult, StringBuilder errorMsg) {
        // 1. 获取平台定制SKU信息（已经过滤掉skuNum=0的数据）
        List<TemuDeliveryOrderSimpleVO> deliveryOrders = temuResult.getList().stream()
                .filter(order -> order.getExpressDeliverySn().equals(trackingNumber))
                .collect(Collectors.toList());

        // 2. 按备货单号分组统计平台定制SKU数量
        Map<String, Integer> temuOrderSkuCounts = new HashMap<>();  // key: 备货单号, value: 定制SKU数量
        Map<String, Set<Long>> temuOrderSkuIds = new HashMap<>();   // key: 备货单号, value: 定制SKU ID集合
        for (TemuDeliveryOrderSimpleVO order : deliveryOrders) {
            String orderNo = order.getSubPurchaseOrderSn();
            int skuCount = 0;
            Set<Long> skuIds = new HashSet<>();
            for (TemuDeliveryOrderSimpleVO.PackageDetailVO detail : order.getPackageDetailList()) {
                if (detail.getSkuNum() > 0) {
                    skuCount++;
                    skuIds.add(detail.getProductSkuId());
                }
            }
            temuOrderSkuCounts.put(orderNo, skuCount);
            temuOrderSkuIds.put(orderNo, skuIds);
        }

        // 3. 按备货单号分组获取本地定制SKU数量
        Map<String, List<TemuOrderDO>> localOrderGroups = orderMapper.selectList(
                new LambdaQueryWrapperX<TemuOrderDO>()
                        .in(TemuOrderDO::getOrderNo, orderNos))
                .stream()
                .collect(Collectors.groupingBy(TemuOrderDO::getOrderNo));

        // 4. 分别比对每个备货单号下的定制SKU数量
        boolean hasError = false;
        for (Map.Entry<String, Integer> entry : temuOrderSkuCounts.entrySet()) {
            String orderNo = entry.getKey();
            int temuSkuCount = entry.getValue();
            int localSkuCount = localOrderGroups.getOrDefault(orderNo, Collections.emptyList()).size();

            if (temuSkuCount != localSkuCount) {
                String alertMsg = String.format("店铺：%s\n物流单号：%s\n备货单号：%s\n定制SKU数量不匹配：平台%d个，本地%d个",
                        shopName, trackingNumber, orderNo, temuSkuCount, localSkuCount);
                sendWeChatAlert(alertMsg);
                errorMsg.append(alertMsg).append("；");
                hasError = true;
            } else {
                // 5. 如果数量一致，比对定制SKU内容
                Set<Long> temuSkuIds = temuOrderSkuIds.get(orderNo);
                Set<Long> localSkuIds = localOrderGroups.get(orderNo).stream()
                        .map(order -> Long.parseLong(order.getCustomSku()))
                        .collect(Collectors.toSet());

                // 找出平台有但本地没有的定制SKU
                Set<Long> temuOnly = new HashSet<>(temuSkuIds);
                temuOnly.removeAll(localSkuIds);
                // 找出本地有但平台没有的定制SKU
                Set<Long> localOnly = new HashSet<>(localSkuIds);
                localOnly.removeAll(temuSkuIds);

                if (!temuOnly.isEmpty() || !localOnly.isEmpty()) {
                    StringBuilder alertMsg = new StringBuilder()
                            .append("店铺：").append(shopName).append("\n")
                            .append("物流单号：").append(trackingNumber).append("\n")
                            .append("备货单号：").append(orderNo).append("\n");

                    if (!temuOnly.isEmpty()) {
                        alertMsg.append("本地缺少定制SKU：").append(String.join("，", 
                                temuOnly.stream().map(String::valueOf).collect(Collectors.toList()))).append("\n");
                    }
                    if (!localOnly.isEmpty()) {
                        alertMsg.append("本地多出定制SKU：").append(String.join("，", 
                                localOnly.stream().map(String::valueOf).collect(Collectors.toList()))).append("\n");
                    }

                    sendWeChatAlert(alertMsg.toString());
                    errorMsg.append(alertMsg).append("；");
                    hasError = true;
                }
            }
        }
        // 检查是否有本地有但平台没有的备货单号
        Set<String> extraLocalOrders = new HashSet<>(localOrderGroups.keySet());
        extraLocalOrders.removeAll(temuOrderSkuCounts.keySet());
        if (!extraLocalOrders.isEmpty()) {
            String alertMsg = String.format("店铺名称：%s\n物流单号：%s\n本地多出备货单号：%s", 
                    shopName, trackingNumber, String.join("，", extraLocalOrders));
            sendWeChatAlert(alertMsg);
            errorMsg.append(alertMsg).append("；");
            hasError = true;
        }

        return hasError;
    }
    // 收集Temu平台的物流单号与备货单号的映射关系
    private Map<String, List<String>> collectTemuTrackingOrders(PageResult<TemuDeliveryOrderSimpleVO> temuResult) {
        Map<String, List<String>> temuTrackingToOrders = new HashMap<>();
        for (TemuDeliveryOrderSimpleVO delivery : temuResult.getList()) {
            temuTrackingToOrders.computeIfAbsent(delivery.getExpressDeliverySn(), k -> new ArrayList<>())
                    .add(delivery.getSubPurchaseOrderSn());
        }
        return temuTrackingToOrders;
    }
    // 收集本地的物流单号与备货单号的映射关系
    private Map<String, List<String>> collectLocalTrackingOrders(List<TemuOrderShippingInfoDO> shippingInfos) {
        return shippingInfos.stream()
                .collect(Collectors.groupingBy(
                        TemuOrderShippingInfoDO::getTrackingNumber,
                        Collectors.mapping(TemuOrderShippingInfoDO::getOrderNo, Collectors.toList())
                ));
    }
    // 收集Temu平台的物流单号与定制SKU（官网数量）信息的映射关系
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
    // 获取店铺名称
    private String getShopName(String shopId) {
        try {
            TemuShopDO shop = temuShopMapper.selectByShopId(Long.parseLong(shopId));
            return shop != null ? shop.getShopName() : "店铺ID:" + shopId;
        } catch (Exception e) {
            return "店铺ID:" + shopId;
        }
    }
    // 处理店铺级别的异常 主要处理店铺不存在的情况，其他异常继续向上抛出
    private void handleShopException(RuntimeException e, String shopId, String shopName,
                                     Set<String> trackingNumbers, ValidationResults results, StringBuilder errorMsg) {
        if (e.getMessage().contains("未找到对应的店铺信息")) {
            String alertMsg = String.format("店铺：%s\n物流单号列表：%s\n错误信息：%s",
                    shopName, String.join("，", trackingNumbers), e.getMessage());
            sendWeChatAlert(alertMsg);
            errorMsg.append(alertMsg).append("；");
            results.setSuccess(false);
        } else {
            throw e; // 其他错误继续向上抛出
        }
    }
    // 处理全局异常
    private void handleGlobalException(Exception e, Map<String, Set<String>> shopTrackingNumbers,
            TemuOrderTrackingValidateRespVO respVO) {
        respVO.setSuccess(false);
        String errorMessage = e.getMessage();
        if (errorMessage.contains("未找到对应的店铺信息")) {
            String shopId = errorMessage.substring(errorMessage.indexOf("shopId=") + 7);
            String shopName = getShopName(shopId);
            Set<String> affectedTrackingNumbers = shopTrackingNumbers.get(shopId);
            errorMessage = String.format("店铺：%s\n物流单号列表：%s\n错误信息：%s",
                    shopName, 
                    affectedTrackingNumbers != null ? String.join("，", affectedTrackingNumbers) : "无",
                    errorMessage);
            // 只在店铺信息错误时发送告警
            sendWeChatAlert(errorMessage);
        }
        respVO.setErrorMessage("校验失败：" + errorMessage);
        log.error("[validateTrackingNumber] 验证物流单号时发生错误，物流单号列表：{}", shopTrackingNumbers, e);
    }
    // 校验结果的内部类
    @Data
    private static class ValidationResults {
        /** 是否校验成功 */
        private boolean success = true;
        /** 物流单号到备货单号的映射 */
        private final Map<String, List<String>> temuTrackingToOrders = new HashMap<>();
        /** 物流单号到SKU信息的映射 */
        private final Map<String, List<TemuOrderTrackingValidateRespVO.SkuInfo>> temuTrackingToSkus = new HashMap<>();
    }
    /**
     * 发送企业微信告警
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
                    String[] parts = alert.trim().split(",");
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