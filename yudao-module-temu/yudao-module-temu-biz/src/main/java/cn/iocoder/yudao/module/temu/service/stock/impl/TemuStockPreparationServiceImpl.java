package cn.iocoder.yudao.module.temu.service.stock.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.print.TemuPrintDataKeyRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.stock.TemuStockPreparationPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.stock.TemuStockPreparationVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOpenapiShopDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderApiDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOpenapiShopMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderApiMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopMapper;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategorySkuDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuProductCategorySkuMapper;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopOldTypeSkcDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopOldTypeSkcMapper;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.temu.mq.producer.weixin.WeiXinProducer;
import cn.iocoder.yudao.module.temu.service.stock.TemuStockPreparationService;
import cn.iocoder.yudao.module.temu.utils.openapi.TemuOpenApiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.hutool.core.map.MapUtil;
import cn.iocoder.yudao.module.temu.controller.admin.vo.goods.TemuCustomGoodsLabelQueryReqVO;
import cn.iocoder.yudao.module.temu.service.deliveryOrder.TemuDeliveryOrderConvertService;
import cn.iocoder.yudao.module.temu.service.oss.TemuOssService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
@Slf4j
public class TemuStockPreparationServiceImpl implements TemuStockPreparationService {

    @Resource
    private ConfigApi configApi;

    @Resource
    private TemuOpenapiShopMapper temuOpenapiShopMapper;

    @Resource
    private TemuOrderApiMapper temuOrderApiMapper;

    @Resource
    private TemuShopMapper temuShopMapper;

    @Resource
    private TemuProductCategorySkuMapper temuProductCategorySkuMapper;

    @Resource
    private WeiXinProducer weiXinProducer;

    @Resource
    private TemuDeliveryOrderConvertService convertService;

    @Resource
    private TemuOssService temuOssService;

    @Resource
    private TemuShopOldTypeSkcMapper temuShopOldTypeSkcMapper;

    private static final Long DEFAULT_SHOP_ID = 634418222478497L;
    private static final Long WEIXIN_SHOP_ID = 88888888L;

    @Override
    public PageResult<TemuStockPreparationVO> getStockPreparationPage(TemuStockPreparationPageReqVO reqVO) {
        // 1. 获取店铺信息
        Long shopId = DEFAULT_SHOP_ID;
        if (!CollectionUtils.isEmpty(reqVO.getSupplierIdList())) {
            shopId = reqVO.getSupplierIdList().get(0).longValue();
        }
        TemuOpenapiShopDO shop = temuOpenapiShopMapper.selectById(shopId);
        if (shop == null) {
            throw new RuntimeException(String.format("店铺信息不存在，shopId: %d", shopId));
        }

        // 2. 构建API工具类
        TemuOpenApiUtil openApiUtil = new TemuOpenApiUtil();
        openApiUtil.setAppKey(shop.getAppKey());
        openApiUtil.setAppSecret(shop.getAppSecret());
        openApiUtil.setAccessToken(shop.getToken());
        openApiUtil.setBaseUrl("https://openapi.kuajingmaihuo.com/openapi/router");

        // 3. 构建请求参数
        TreeMap<String, Object> params = new TreeMap<>();
        // 添加分页参数
        params.put("pageSize", reqVO.getPageSize());
        params.put("pageNo", reqVO.getPageNo());
        // 添加查询条件（如果不为空）
        if (reqVO.getPurchaseTimeTo() != null) {
            params.put("purchaseTimeTo", reqVO.getPurchaseTimeTo());
        }
        if (reqVO.getSupplierIdList() != null && !reqVO.getSupplierIdList().isEmpty()) {
            params.put("supplierIdList", reqVO.getSupplierIdList());
        }
        if (reqVO.getProductSkcIdList() != null && !reqVO.getProductSkcIdList().isEmpty()) {
            params.put("productSkcIdList", reqVO.getProductSkcIdList());
        }
        if (reqVO.getSubPurchaseOrderSnList() != null && !reqVO.getSubPurchaseOrderSnList().isEmpty()) {
            params.put("subPurchaseOrderSnList", reqVO.getSubPurchaseOrderSnList());
        }
        if (reqVO.getPurchaseTimeFrom() != null) {
            params.put("purchaseTimeFrom", reqVO.getPurchaseTimeFrom());
        }
        if (reqVO.getStatusList() != null && !reqVO.getStatusList().isEmpty()) {
            params.put("statusList", reqVO.getStatusList());
        }

        try {
            // 4. 调用API获取结果
            String apiResult = openApiUtil.getShipOrderList(params);
            
            // 5. 解析API返回结果为VO对象
            JSONObject jsonResult = JSONUtil.parseObj(apiResult);
            if (!jsonResult.getBool("success", false)) {
                throw new RuntimeException("API调用失败：" + jsonResult.getStr("errorMsg"));
            }
            
            JSONObject result = jsonResult.getJSONObject("result");
            Long total = result.getLong("total");
            List<TemuStockPreparationVO> list = new ArrayList<>();
            
            // 解析订单列表
            result.getJSONArray("subOrderForSupplierList").forEach(item -> {
                JSONObject order = (JSONObject) item;
                TemuStockPreparationVO vo = new TemuStockPreparationVO();
                
                // 设置订单基本信息
                vo.setSubPurchaseOrderSn(order.getStr("subPurchaseOrderSn"));
                vo.setProductName(order.getStr("productName"));
                vo.setProductSkcId(order.getStr("productSkcId"));
                vo.setCategory(order.getStr("category"));
                vo.setSupplierId(order.getStr("supplierId"));
                vo.setSupplierName(order.getStr("supplierName"));
                vo.setPurchaseTime(order.getStr("purchaseTime"));
                vo.setStatus(order.getInt("status"));
                
                // 解析SKU详情列表
                List<TemuStockPreparationVO.SkuDetail> skuDetails = new ArrayList<>();
                JSONArray skuList = order.getJSONArray("skuQuantityDetailList");
                skuList.forEach(skuItem -> {
                    JSONObject skuDetail = (JSONObject) skuItem;
                    TemuStockPreparationVO.SkuDetail detail = new TemuStockPreparationVO.SkuDetail();
                    
                    detail.setClassName(skuDetail.getStr("className"));
                    detail.setThumbUrlList(skuDetail.getJSONArray("thumbUrlList").toList(String.class));
                    detail.setProductSkuId(skuDetail.getStr("productSkuId"));
                    detail.setFulfilmentProductSkuId(skuDetail.getStr("fulfilmentProductSkuId"));
                    detail.setPurchaseQuantity(skuDetail.getInt("purchaseQuantity"));
                    
                    skuDetails.add(detail);
                });
                vo.setSkuQuantityDetailList(skuDetails);
                
                list.add(vo);
            });
            
            // 6. 返回分页结果
            return new PageResult<>(list, total, reqVO.getPageNo(), reqVO.getPageSize());
        } catch (Exception e) {
            log.error("[getStockPreparationPage][查询备货单列表异常]", e);
            throw new RuntimeException("查询备货单列表失败", e);
        }
    }

    @Override
    public int saveStockPreparation() {
        long startTime = System.currentTimeMillis();
        log.info("[saveStockPreparation][开始执行备货单同步]");

        // 1. 构建API工具类
        TemuOpenapiShopDO shop = temuOpenapiShopMapper.selectById(DEFAULT_SHOP_ID);
        if (shop == null) {
            throw new RuntimeException(String.format("店铺信息不存在，shopId: %d", DEFAULT_SHOP_ID));
        }

        TemuOpenApiUtil openApiUtil = new TemuOpenApiUtil();
        openApiUtil.setAppKey(shop.getAppKey());
        openApiUtil.setAppSecret(shop.getAppSecret());
        openApiUtil.setAccessToken(shop.getToken());
        openApiUtil.setBaseUrl("https://openapi.kuajingmaihuo.com/openapi/router");

        // 2. 构建请求参数
        TreeMap<String, Object> params = new TreeMap<>();
        // 添加近一天的时间参数（毫秒级时间戳）
        
        long now = System.currentTimeMillis();
        long oneDayAgo = now - 1L * 24 * 60 * 60 * 1000;
        params.put("purchaseTimeFrom", oneDayAgo);
        params.put("purchaseTimeTo", now);
        
        // 添加状态条件，除了8-作废以外的所有状态
        List<Integer> statusList = new ArrayList<>();
        statusList.add(0);
        statusList.add(1);
        statusList.add(2);
        statusList.add(3);
        statusList.add(4);
        statusList.add(5);
        statusList.add(6);
        statusList.add(7);
        statusList.add(9);
        params.put("statusList", statusList);

        int pageNo = 1;
        int pageSize = 50; // 修改为10条数据用于测试
        Long total = null;

        // 从配置中获取是否开启可动态配置批次生成时间段
        String isSortingSequence = configApi.getConfigValueByKey("temu_is_sortingSequence");
        log.info("中包序号功能是否开启: {}", isSortingSequence);
        boolean flag = false; // 默认值
        if (StrUtil.isNotEmpty(isSortingSequence)) {
            try {
                flag = Boolean.parseBoolean(isSortingSequence);
            } catch (Exception e) {
                log.warn("中包序号功能是否开启，使用默认值");
            }
        }

        // 用于收集所有订单数据，以便生成序号
        List<Map<String, Object>> allOrders = new ArrayList<>();

        while (true) {
            long pageStartTime = System.currentTimeMillis();
            params.put("pageNo", pageNo);
            params.put("pageSize", pageSize);
        try {
            // 3. 调用API保存数据
            String apiResult = openApiUtil.getShipOrderList(params);
            // 4. 解析API返回结果
            JSONObject jsonResult = JSONUtil.parseObj(apiResult);
            if (!jsonResult.getBool("success", false)) {
                throw new RuntimeException("API调用失败：" + jsonResult.getStr("errorMsg"));
            }
                JSONObject result = jsonResult.getJSONObject("result");
                if (total == null) {
                    total = result.getLong("total", 0L);
                }
                JSONArray subOrderList = result.getJSONArray("subOrderForSupplierList");
                if (subOrderList != null && !subOrderList.isEmpty()) {
                    List<TemuOrderApiDO> orderApiDOList = new ArrayList<>();
                    // 将API返回的订单数据转换为Map格式，用于生成序号
                    for (Object item : subOrderList) {
                        JSONObject order = (JSONObject) item;
                        Map<String, Object> orderMap = new HashMap<>();
                        orderMap.put("orderId", order.getStr("subPurchaseOrderSn"));
                        
                        // 处理skus信息
                        JSONArray skuList = order.getJSONArray("skuQuantityDetailList");
                        if (skuList != null && !skuList.isEmpty()) {
                            for (Object skuItem : skuList) {
                                JSONObject skuDetail = (JSONObject) skuItem;
                                Map<String, Object> skusMap = new HashMap<>();
                                skusMap.put("skuId", skuDetail.getStr("productSkuId"));
                                orderMap.put("skus", skusMap);
                                
                                // 添加到总订单列表
                                allOrders.add(new HashMap<>(orderMap));
                            }
                        }
                    }

                    if(flag) {
                        // 生成序号
                        generateSortingSequenceBatch(allOrders);
                        // 验证序号一致性
                        validateSortingSequenceConsistency(allOrders);
                    }

                    // 1. 收集所有的SKC
                    Set<String> skcSet = subOrderList.stream()
                            .map(item -> ((JSONObject) item).getStr("productSkcId"))
                            .filter(skc -> !skc.isEmpty())
                            .collect(Collectors.toSet());

                    // 2. 批量查询合规单URL
                    Map<String, TemuShopOldTypeSkcDO> skcToOldTypeMap = new HashMap<>();
                    if (!skcSet.isEmpty()) {
                        LambdaQueryWrapper<TemuShopOldTypeSkcDO> queryWrapper = new LambdaQueryWrapper<TemuShopOldTypeSkcDO>()
                                .eq(TemuShopOldTypeSkcDO::getShopId, DEFAULT_SHOP_ID)
                                .in(TemuShopOldTypeSkcDO::getSkc, skcSet);
                        List<TemuShopOldTypeSkcDO> oldTypeSkcList = temuShopOldTypeSkcMapper.selectList(queryWrapper);
                        skcToOldTypeMap = oldTypeSkcList.stream()
                                .collect(Collectors.toMap(TemuShopOldTypeSkcDO::getSkc, oldType -> oldType));
                    }

                    // 处理订单数据
                    for (Object item : subOrderList) {
                        JSONObject order = (JSONObject) item;
                        String subPurchaseOrderSn = order.getStr("subPurchaseOrderSn");
                        String productName = order.getStr("productName");
                        String productSkcId = order.getStr("productSkcId");
                        String category = order.getStr("category");
                        String supplierId = order.getStr("supplierId");
                        String supplierName = order.getStr("supplierName");
                        String purchaseTime = order.getStr("purchaseTime");
                        Integer status = order.getInt("status");
                        JSONArray skuList = order.getJSONArray("skuQuantityDetailList");
                        if (skuList != null) {
                            for (Object skuItem : skuList) {
                                JSONObject skuDetail = (JSONObject) skuItem;
                                TemuOrderApiDO orderApiDO = new TemuOrderApiDO();
                                // 赋值
                                orderApiDO.setOrderNo(subPurchaseOrderSn);
                                orderApiDO.setProductTitle(productName);
                                orderApiDO.setSkc(productSkcId);

                                // 设置订单状态，参考saveOrders的处理方式
                                String statusStr = order.getStr("status");
                                // TODO: 后续需要完善其他状态的处理逻辑
                                if ("1".equals(statusStr)) {
                                    orderApiDO.setOrderStatus(0); // 待发货状态设为0
                                } else {
                                    orderApiDO.setOrderStatus(99); // 其他状态统一设为99
                                }

                                // 设置SKU相关信息
                                String sku = skuDetail.getStr("productSkuId");
                                orderApiDO.setSku(sku);
                                String customSku = skuDetail.getStr("fulfilmentProductSkuId");
                                orderApiDO.setCustomSku(customSku);

                                // 设置合规单URL
                                if (!productSkcId.isEmpty()) {
                                    TemuShopOldTypeSkcDO oldTypeSkcDO = skcToOldTypeMap.get(productSkcId);
                                    if (oldTypeSkcDO != null) {
                                        orderApiDO.setComplianceUrl(oldTypeSkcDO.getOldTypeUrl());
                                        orderApiDO.setComplianceImageUrl(oldTypeSkcDO.getOldTypeImageUrl());
                                    }
                                }

                                // 获取商品条码URL
//                                if (StrUtil.isNotBlank(customSku)) {
//                                    try {
//                                        TemuCustomGoodsLabelQueryReqVO labelReqVO = new TemuCustomGoodsLabelQueryReqVO();
//                                        List<Long> customSkuList = new ArrayList<>();
//                                        customSkuList.add(Long.parseLong(customSku));
//                                        labelReqVO.setPersonalProductSkuIdList(customSkuList);
//                                        TemuPrintDataKeyRespVO labelRespVO = convertService.getCustomGoodsLabelPrintDataKey(labelReqVO);
//                                        if (labelRespVO != null && StrUtil.isNotBlank(labelRespVO.getPrintUrl())) {
//                                            // 直接保存打印URL，不进行下载或转换
//                                            orderApiDO.setGoodsSn(labelRespVO.getPrintUrl());
//                                            log.info("[saveStockPreparation][获取商品条码URL成功] customSku:{}, url:{}",
//                                                customSku, labelRespVO.getPrintUrl());
//                                        }
//                                    } catch (Exception e) {
//                                        log.error("[saveStockPreparation][获取商品条码URL异常] customSku:{}", customSku, e);
//                                    }
//                                }

                                // 设置类目信息，参考saveOrders的处理方式
                                if (StrUtil.isNotBlank(sku)) {
                                    HashMap<String, Object> queryMap = MapUtil.of("sku", sku);
                                    queryMap.put("shop_id", supplierId);
                                    List<TemuProductCategorySkuDO> categorySkuList = temuProductCategorySkuMapper.selectByMap(queryMap);
                                    if (categorySkuList != null && !categorySkuList.isEmpty()) {
                                        TemuProductCategorySkuDO categorySku = categorySkuList.get(0);
                                        orderApiDO.setCategoryId(String.valueOf(categorySku.getCategoryId()));
                                        orderApiDO.setCategoryName(categorySku.getCategoryName());
                                    }
                                }

                                orderApiDO.setShopId(supplierId);
                                orderApiDO.setOriginalQuantity(skuDetail.getInt("purchaseQuantity")); // 保存官网原始数量
                                orderApiDO.setProductProperties(skuDetail.getStr("className"));
                                orderApiDO.setProductImgUrl(skuDetail.getJSONArray("thumbUrlList") != null ? skuDetail.getJSONArray("thumbUrlList").toString() : null);
                                
                                // TODO ：需要从API获取实际的销售价格
                                orderApiDO.setSalePrice(BigDecimal.ZERO);
                                
                                // TODO ：后续需要设置实际的制作数量
                                orderApiDO.setQuantity(orderApiDO.getOriginalQuantity());
                                
                                // 订单创建时间
                                if (purchaseTime != null) {
                                    try {
                                        orderApiDO.setBookingTime(LocalDateTime.ofEpochSecond(Long.parseLong(purchaseTime) / 1000, 0, java.time.ZoneOffset.ofHours(8)));
                                    } catch (Exception ignore) {}
                                }

                                // 设置sorting_sequence
                                if(flag) {
                                    // 从allOrders中找到对应的序号
                                    final String finalSku = sku; // 创建final变量供Lambda使用
                                    Optional<Map<String, Object>> matchingOrder = allOrders.stream()
                                        .filter(o -> orderApiDO.getOrderNo().equals(o.get("orderId")) && 
                                            finalSku.equals(((Map)o.get("skus")).get("skuId")))
                                        .findFirst();
                                    
                                    matchingOrder.ifPresent(o -> {
                                        String sortingSequence = (String) o.get("sorting_sequence");
                                        if (sortingSequence != null) {
                                            orderApiDO.setSortingSequence(sortingSequence);
                                        }
                                    });
                                }
                                
                                // 根据customSku查询是否存在记录
                                TemuOrderApiDO existingOrder = temuOrderApiMapper.selectByCustomSku(orderApiDO.getCustomSku());
                                if (existingOrder != null) {
                                    // 比较下单时间
                                    if (orderApiDO.getBookingTime() != null && existingOrder.getBookingTime() != null) {
                                        if (orderApiDO.getBookingTime().isAfter(existingOrder.getBookingTime())) {
                                            // 如果当前订单时间更晚，说明是返单，新增记录
                                            // 先查询当前订单是否已经被标记为返单
                                            TemuOrderApiDO currentOrderInDb = temuOrderApiMapper.selectByCustomSkuAndOrderNo(
                                                orderApiDO.getCustomSku(), orderApiDO.getOrderNo());
                                            boolean alreadyReturn = currentOrderInDb != null && 
                                                currentOrderInDb.getIsReturnOrder() != null && 
                                                currentOrderInDb.getIsReturnOrder() == 1;

                                            orderApiDO.setIsReturnOrder(1);
                                            orderApiDOList.add(orderApiDO); // 添加到批量插入列表
                                            
                                            log.info("[saveStockPreparation][发现返单情况] 定制SKU:{}, 原订单号:{}, 新订单号:{}, 原订单日期:{}, 新订单日期:{}",
                                                orderApiDO.getCustomSku(), existingOrder.getOrderNo(), orderApiDO.getOrderNo(),
                                                existingOrder.getBookingTime(), orderApiDO.getBookingTime());
                                            
                                            // 如果未被标记过返单，则发送企业微信通知
                                            if (!alreadyReturn) {
                                                TemuShopDO weixinShop = temuShopMapper.selectByShopId(WEIXIN_SHOP_ID);
                                                if (weixinShop != null && StrUtil.isNotEmpty(weixinShop.getWebhook())) {
                                                    String message = String.format("警告：发现返单情况，请检查订单：\n定制SKU: %s\n原订单号: %s\n新订单号: %s\n原订单日期: %s\n新订单日期: %s\n店铺: %s", 
                                                        orderApiDO.getCustomSku(), existingOrder.getOrderNo(), orderApiDO.getOrderNo(),
                                                        existingOrder.getBookingTime(), orderApiDO.getBookingTime(), 
                                                        existingOrder.getShopId());
                                                    weiXinProducer.sendMessage(weixinShop.getWebhook(), message);
                                                }
                                            }
                                        } else if (orderApiDO.getBookingTime().isEqual(existingOrder.getBookingTime())) {
                                            // 如果时间相同，比较备货单号
                                            if (!existingOrder.getOrderNo().equals(orderApiDO.getOrderNo())) {
                                                // 备货单号不同，更新记录
                                                orderApiDO.setId(existingOrder.getId());
                                                temuOrderApiMapper.updateById(orderApiDO);
                                            }
                                            // 备货单号相同，不做任何操作
                                        }
                                        // 如果当前订单时间更早，不做任何操作
                                    }
                                } else {
                                    // 不存在记录，添加到批量插入列表
                                    orderApiDOList.add(orderApiDO);
                                }
                            }
                        }
                    }
                    
                    // 批量插入
                    if (!orderApiDOList.isEmpty()) {
                        for (TemuOrderApiDO orderApiDO : orderApiDOList) {
                            temuOrderApiMapper.insert(orderApiDO);
                        }
                    }
                }
                if (subOrderList == null || subOrderList.isEmpty() || subOrderList.size() < pageSize) {
                    long pageEndTime = System.currentTimeMillis();
                    log.info("[saveStockPreparation][第{}页处理完成][耗时: {}ms][处理数据量: {}]", 
                        pageNo, pageEndTime - pageStartTime, 
                        subOrderList != null ? subOrderList.size() : 0);
                    break;
                }
                long pageEndTime = System.currentTimeMillis();
                log.info("[saveStockPreparation][第{}页处理完成][耗时: {}ms][处理数据量: {}]", 
                    pageNo, pageEndTime - pageStartTime, subOrderList.size());
                pageNo++;
        } catch (Exception e) {
            log.error("[saveStockPreparation][保存备货单异常]", e);
            throw new RuntimeException("保存备货单失败", e);
        }
        }
        long endTime = System.currentTimeMillis();
        log.info("[saveStockPreparation][备货单同步完成][总耗时: {}ms][总数据量: {}]", endTime - startTime, total);
        return total == null ? 0 : total.intValue();
    }

    /**
     * 批量生成sorting_sequence（基于传入的ordersList数据，不查询数据库）
     */
    private void generateSortingSequenceBatch(List<Map<String, Object>> ordersList) {
        if (CollUtil.isEmpty(ordersList)) {
            return;
        }

        // 1. 按订单编号分组
        Map<String, List<Map<String, Object>>> orderNoGroupMap = ordersList.stream()
                .collect(Collectors.groupingBy(orderMap -> 
                    convertToString(orderMap.get("orderId"))));

        // 2. 为每个订单编号组生成sorting_sequence
        for (Map.Entry<String, List<Map<String, Object>>> entry : orderNoGroupMap.entrySet()) {
            String orderNo = entry.getKey();
            List<Map<String, Object>> sameOrderNoList = entry.getValue();

            if (StrUtil.isBlank(orderNo)) {
                continue;
            }

            try {
                // 3. 获取订单编号后6位作为基础编号
                String baseSequence;
                if (orderNo.length() >= 6) {
                    baseSequence = orderNo.substring(orderNo.length() - 6);
                } else {
                    baseSequence = String.format("%06d", Integer.parseInt(orderNo));
                }

                // 4. 按SKU分组，相同SKU使用相同的sorting_sequence
                Map<String, List<Map<String, Object>>> skuGroupMap = sameOrderNoList.stream()
                        .collect(Collectors.groupingBy(orderMap -> {
                            Map<String, Object> skusMap = (Map<String, Object>) orderMap.get("skus");
                            if (skusMap != null) {
                                return convertToString(skusMap.get("skuId"));
                            }
                            return "";
                        }));

                // 5. 为每个SKU组分配sorting_sequence
                List<String> allSkus = new ArrayList<>(skuGroupMap.keySet());
                allSkus.sort((sku1, sku2) -> {
                    int hash1 = Math.abs(sku1.hashCode());
                    int hash2 = Math.abs(sku2.hashCode());
                    if (hash1 != hash2) {
                        return Integer.compare(hash1, hash2);
                    }
                    return sku1.compareTo(sku2);
                });

                for (int i = 0; i < allSkus.size(); i++) {
                    String sku = allSkus.get(i);
                    List<Map<String, Object>> sameSkuList = skuGroupMap.get(sku);
                    
                    String sortingSequence;
                    if (i == 0) {
                        sortingSequence = baseSequence;
                    } else {
                        sortingSequence = baseSequence + "_" + String.format("%02d", i + 1);
                    }

                    sameSkuList.forEach(orderMap -> orderMap.put("sorting_sequence", sortingSequence));
                }
            } catch (Exception e) {
                log.error("生成sorting_sequence时发生异常: orderNo={}", orderNo, e);
            }
        }
    }

    /**
     * 验证生成的序号一致性
     */
    private void validateSortingSequenceConsistency(List<Map<String, Object>> ordersList) {
        if (CollUtil.isEmpty(ordersList)) {
            return;
        }
        
        Map<String, List<Map<String, Object>>> orderNoGroupMap = ordersList.stream()
                .collect(Collectors.groupingBy(orderMap -> 
                    convertToString(orderMap.get("orderId"))));

        for (Map.Entry<String, List<Map<String, Object>>> entry : orderNoGroupMap.entrySet()) {
            String orderNo = entry.getKey();
            List<Map<String, Object>> sameOrderNoList = entry.getValue();
            
            Map<String, String> skuToSequenceMap = new HashMap<>();
            boolean hasInconsistency = false;
            
            for (Map<String, Object> orderMap : sameOrderNoList) {
                String sku = "";
                Map<String, Object> skusMap = (Map<String, Object>) orderMap.get("skus");
                if (skusMap != null) {
                    sku = convertToString(skusMap.get("skuId"));
                }
                
                String sortingSequence = convertToString(orderMap.get("sorting_sequence"));
                
                if (StrUtil.isNotBlank(sku) && StrUtil.isNotBlank(sortingSequence)) {
                    String existingSequence = skuToSequenceMap.get(sku);
                    if (existingSequence != null && !existingSequence.equals(sortingSequence)) {
                        log.warn("SKU {} 在同一订单 {} 中存在不同的序号: {} vs {}", 
                            sku, orderNo, existingSequence, sortingSequence);
                        hasInconsistency = true;
                    } else {
                        skuToSequenceMap.put(sku, sortingSequence);
                    }
                }
            }
            
            if (hasInconsistency) {
                log.warn("订单 {} 的SKU-序号映射存在不一致: {}", orderNo, skuToSequenceMap);
            }
        }
    }

    private String convertToString(Object obj) {
        return obj == null ? "" : obj.toString();
    }
} 