package cn.iocoder.yudao.module.temu.service.stock.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.stock.TemuStockPreparationPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.stock.TemuStockPreparationVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOpenapiShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOpenapiShopMapper;
import cn.iocoder.yudao.module.temu.service.stock.TemuStockPreparationService;
import cn.iocoder.yudao.module.temu.utils.openapi.TemuOpenApiUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Service
@Slf4j
public class TemuStockPreparationServiceImpl implements TemuStockPreparationService {

    @Resource
    private TemuOpenapiShopMapper temuOpenapiShopMapper;

    private static final Long DEFAULT_SHOP_ID = 634418222478497L;

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
        // log.info("[getStockPreparationPage][时间参数] from:{}, to:{}", reqVO.getPurchaseTimeFrom(), reqVO.getPurchaseTimeTo());
        if (reqVO.getPurchaseTimeFrom() != null) {
            params.put("purchaseTimeFrom", reqVO.getPurchaseTimeFrom());
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
                JSONObject skuDetail = order.getJSONArray("skuQuantityDetailList").getJSONObject(0);
                
                TemuStockPreparationVO vo = new TemuStockPreparationVO();
                vo.setClassName(skuDetail.getStr("className"));
                vo.setThumbUrlList(skuDetail.getJSONArray("thumbUrlList").toList(String.class));
                vo.setProductSkuId(skuDetail.getStr("productSkuId"));
                vo.setPurchaseTime(order.getStr("purchaseTime"));
                vo.setSupplierId(order.getStr("supplierId"));
                vo.setSubPurchaseOrderSn(order.getStr("subPurchaseOrderSn"));
                vo.setSupplierName(order.getStr("supplierName"));
                vo.setPurchaseQuantity(skuDetail.getInt("purchaseQuantity"));
                vo.setProductName(order.getStr("productName"));
                vo.setProductSkcId(order.getStr("productSkcId"));
                vo.setCategory(order.getStr("category"));
                vo.setFulfilmentProductSkuId(skuDetail.getStr("fulfilmentProductSkuId"));
                
                list.add(vo);
            });
            
            // 6. 返回分页结果
            return new PageResult<>(list, total, reqVO.getPageNo(), reqVO.getPageSize());
        } catch (Exception e) {
            log.error("[getStockPreparationPage][查询备货单列表异常]", e);
            throw new RuntimeException("查询备货单列表失败", e);
        }
    }
} 