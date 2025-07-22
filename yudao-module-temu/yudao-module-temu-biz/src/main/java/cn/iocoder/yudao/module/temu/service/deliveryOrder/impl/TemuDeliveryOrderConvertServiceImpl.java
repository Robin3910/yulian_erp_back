package cn.iocoder.yudao.module.temu.service.deliveryOrder.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderSimpleVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuBoxMarkQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuBoxMarkRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.goods.TemuCustomGoodsLabelQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.goods.TemuCustomGoodsLabelRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.print.TemuPrintDataKeyRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOpenapiShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOpenapiShopMapper;
import cn.iocoder.yudao.module.temu.service.deliveryOrder.TemuDeliveryOrderConvertService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import cn.iocoder.yudao.module.temu.utils.openapi.TemuOpenApiUtil;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderQueryReqVO;

@Service
@RequiredArgsConstructor
public class TemuDeliveryOrderConvertServiceImpl implements TemuDeliveryOrderConvertService {

    private final TemuOpenapiShopMapper temuOpenapiShopMapper;

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
        vo.setDeliveryOrderSn(item.path("" +
                "").asText());
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
}