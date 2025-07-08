package cn.iocoder.yudao.module.temu.service.imageSearch.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageAddReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageSearchOrderRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageSearchReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch.TemuImageSearchBySnReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderShippingInfoDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderShippingMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopMapper;
import cn.iocoder.yudao.module.temu.service.imageSearch.TemuImageSearchService;
import cn.iocoder.yudao.module.temu.config.TemuImageSearchConfig;
import com.aliyun.imagesearch20201214.Client;
import com.aliyun.imagesearch20201214.models.AddImageAdvanceRequest;
import com.aliyun.imagesearch20201214.models.AddImageResponse;
import com.aliyun.imagesearch20201214.models.SearchImageByPicAdvanceRequest;
import com.aliyun.imagesearch20201214.models.SearchImageByPicResponse;
import com.aliyun.imagesearch20201214.models.SearchImageByPicResponseBody;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;

@Slf4j
@Service
public class TemuImageSearchServiceImpl implements TemuImageSearchService {

    @Resource
    private Client imageSearchClient;

    @Resource
    private TemuImageSearchConfig imageSearchConfig;

    @Resource
    private TemuOrderMapper temuOrderMapper;

    @Resource
    private ConfigApi configApi;

    @Resource
    private TemuOrderShippingMapper temuOrderShippingMapper;

    @Resource
    private TemuShopMapper temuShopMapper;

    // 上传图片到阿里云图库
    @Override
    public AddImageResponse addImage(TemuImageAddReqVO reqVO) {
        try {
            AddImageAdvanceRequest request = new AddImageAdvanceRequest();
            // 设置实例名称
            request.instanceName = imageSearchConfig.getInstanceName();
            // 设置定制sku
            request.productId = reqVO.getProductId();
            // 设置图片名称
            request.picName = reqVO.getPicName();

            // 设置主体识别
            request.crop = true;
            // 设置图片内容
            request.picContentObject = reqVO.getFile().getInputStream();

            // 执行添加请求
            RuntimeOptions runtimeOptions = new RuntimeOptions();
            AddImageResponse response = imageSearchClient.addImageAdvance(request, runtimeOptions);

            // 记录日志
            log.info("[addImage][商品({}) 图片({}) 添加成功][response: {}]",
                    reqVO.getProductId(), reqVO.getPicName(), response.getBody());
            return response;

        } catch (TeaException teaException) {
            log.error("[addImage][商品({}) 图片({}) 添加失败] teaException",
                    reqVO.getProductId(), reqVO.getPicName(), teaException);
            throw new RuntimeException(teaException);
        } catch (Exception e) {
            log.error("[addImage][商品({}) 图片({}) 添加失败]",
                    reqVO.getProductId(), reqVO.getPicName(), e);
            throw new RuntimeException(e);
        }
    }

    // 图片搜索阿里云图库
    @Override
    public List<TemuImageSearchOrderRespVO> searchImageByPicWithHighestScore(TemuImageSearchReqVO reqVO) {
        try {
            // 创建新的搜索请求对象（避免修改原始请求参数）
            TemuImageSearchReqVO searchReqVO = new TemuImageSearchReqVO();

            // 复制原始请求参数
            searchReqVO.setFile(reqVO.getFile()); // 设置搜索图片文件（Base64/URL格式）
            searchReqVO.setCategoryId(reqVO.getCategoryId()); // 设置图像库的类目ID（限定搜索范围）
            searchReqVO.setCrop(reqVO.getCrop()); // 是否开启主体识别（true/false）
            searchReqVO.setRegion(reqVO.getRegion()); // 指定图片主体区域坐标（格式：x1,x2,y1,y2）

            // 从配置中获取返回结果数量
            String numStr = configApi.getConfigValueByKey("temu.image_search.result_num");
            log.info("图片搜索返回结果数量配置值: {}", numStr);
            int num = 10; // 默认值
            if (StrUtil.isNotEmpty(numStr)) {
                try {
                    num = Integer.parseInt(numStr);
                    if (num <= 0) {
                        num = 10;
                    }
                } catch (NumberFormatException e) {
                    log.warn("图片搜索返回结果数量配置格式错误，使用默认值");
                }
            }
            searchReqVO.setNum(num);

            // 调用基础搜索方法（执行阿里云图像搜索API）
            SearchImageByPicResponse response = searchImageByPic(searchReqVO);

            // 获取搜索结果列表（阿里云返回的相似商品数据）
            List<SearchImageByPicResponseBody.SearchImageByPicResponseBodyAuctions> auctions = response.getBody()
                    .getAuctions();

            // 处理空结果情况
            if (auctions == null || auctions.isEmpty()) {
                return new ArrayList<>();
            }

            // 获取所有结果的productId 对应订单的定制sku
            List<String> customSkus = auctions.stream()
                    .map(SearchImageByPicResponseBody.SearchImageByPicResponseBodyAuctions::getProductId)
                    .collect(Collectors.toList());

            // 如果没有找到匹配的图片，返回空列表
            if (customSkus.isEmpty()) {
                return new ArrayList<>();
            }

            // 直接使用TemuOrderMapper 查询 所有对应的订单信息
            List<TemuOrderDO> orderList = temuOrderMapper.selectListByCustomSku(customSkus);
            
            // 预先查询 所有店铺信息
            List<Long> shopIds = orderList.stream().map(TemuOrderDO::getShopId).collect(Collectors.toList());
            Map<Long, TemuShopDO> shopMap = temuShopMapper.selectByShopIds(shopIds).stream()
                    .collect(Collectors.toMap(TemuShopDO::getShopId, shop -> shop));
            
            // 预先查询 所有物流信息
            List<String> orderNos = orderList.stream().map(TemuOrderDO::getOrderNo).collect(Collectors.toList());
            Map<String, TemuOrderShippingInfoDO> shippingMap = temuOrderShippingMapper.selectListByOrderNos(orderNos).stream()
                    .collect(Collectors.toMap(TemuOrderShippingInfoDO::getOrderNo, shipping -> shipping));
            
            // 创建返回结果列表
            List<TemuImageSearchOrderRespVO> resultList = new ArrayList<>();
            
            // 遍历订单列表，匹配对应的productId和score
            for (TemuOrderDO order : orderList) {
                TemuImageSearchOrderRespVO respVO = new TemuImageSearchOrderRespVO();
                // 复制订单信息
                BeanUtils.copyProperties(order, respVO);
                
                // 查找对应的auction记录
                for (SearchImageByPicResponseBody.SearchImageByPicResponseBodyAuctions auction : auctions) {
                    if (auction.getProductId().equals(order.getCustomSku())) {
                        respVO.setProductId(auction.getProductId());
                        respVO.setScore(auction.getScore());
                        break;
                    }
                }

                // 设置店铺相关字段
                TemuShopDO shop = shopMap.get(order.getShopId());
                if (shop != null) {
                    respVO.setShopName(shop.getShopName());
                    respVO.setAliasName(shop.getAliasName());
                }

                // 设置物流相关字段
                TemuOrderShippingInfoDO shipping = shippingMap.get(order.getOrderNo());
                if (shipping != null) {
                    respVO.setTrackingNumber(shipping.getTrackingNumber());
                    respVO.setExpressImageUrl(shipping.getExpressImageUrl());
                    respVO.setExpressOutsideImageUrl(shipping.getExpressOutsideImageUrl());
                    respVO.setExpressSkuImageUrl(shipping.getExpressSkuImageUrl());
                    respVO.setDailySequence(shipping.getDailySequence());
                    respVO.setShippingTime(shipping.getCreateTime());
                    respVO.setSortingSequence(shipping.getSortingSequence());
                }

                resultList.add(respVO);
                log.info("[searchImageByPicWithHighestScore][搜索结果：{}]", respVO);
            }
            
            return resultList;
            
        } catch (Exception e) {
            log.error("[searchImageByPicWithHighestScore][类目({}) 搜索失败]",
                    reqVO.getCategoryId(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行阿里云图像搜索（以图搜图）核心方法
     * 
     * @param reqVO 搜索请求参数封装对象
     * @return 阿里云API返回的搜索结果响应体
     */
    @Override
    public SearchImageByPicResponse searchImageByPic(TemuImageSearchReqVO reqVO) {
        try {
            // 初始化阿里云图像搜索高级请求对象
            SearchImageByPicAdvanceRequest request = new SearchImageByPicAdvanceRequest();
            // 配置实例名称（从配置中心获取）
            request.instanceName = imageSearchConfig.getInstanceName();

            // 设置搜索类目ID（可选）
            if (reqVO.getCategoryId() != null) {
                request.categoryId = reqVO.getCategoryId();
            }

            // 设置返回结果数量
            request.num = reqVO.getNum();

            // 启用主体识别功能（默认开启）
            // 说明：自动裁剪背景，聚焦图片主体提升搜索准确率
            request.crop = reqVO.getCrop() != null ? reqVO.getCrop() : true;

            // 设置主体区域（可选）
            if (reqVO.getRegion() != null) {
                request.region = reqVO.getRegion();
            }

            // 设置图片二进制流（通过文件输入流传输）
            // 注意：使用InputStream避免Base64编码的内存开销
            request.picContentObject = reqVO.getFile().getInputStream();

            // 发起API请求（含运行时配置）
            RuntimeOptions runtimeOptions = new RuntimeOptions();
            SearchImageByPicResponse response = imageSearchClient.searchImageByPicAdvance(request, runtimeOptions);

            // 记录日志
            log.info("[searchImageByPic][类目({}) 搜索成功][response: {}]",
                    reqVO.getCategoryId(), response.getBody());
            return response;

        } catch (TeaException teaException) {
            log.error("[searchImageByPic][类目({}) 搜索失败] teaException",
                    reqVO.getCategoryId(), teaException);
            throw new RuntimeException(teaException);
        } catch (Exception e) {
            log.error("[searchImageByPic][类目({}) 搜索失败]",
                    reqVO.getCategoryId(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据条码编号或定制SKU查询订单
     * @param reqVO 搜索请求（包含goodsSnNo或customSku）
     * @return 订单详情列表
     */
    @Override
    public List<TemuImageSearchOrderRespVO> searchOrderBySnOrSku(TemuImageSearchBySnReqVO reqVO) {
        log.info("[searchOrderBySnOrSku] 开始根据条码编号或定制SKU查询订单, 参数: {}", reqVO);
        
        // 参数校验
        if (!reqVO.isValid()) {
            log.warn("[searchOrderBySnOrSku] 参数无效，goodsSnNo和customSku至少需要提供一个");
            return new ArrayList<>();
        }
        
        try {
            List<TemuOrderDO> orderList;
            
            if (StrUtil.isNotEmpty(reqVO.getGoodsSnNo())) {
                // 如果提供了商品编号，优先使用商品编号查询
                log.info("[searchOrderBySnOrSku] 使用条码编号查询: {}", reqVO.getGoodsSnNo());
                orderList = temuOrderMapper.selectListBygoodsSnNo(Arrays.asList(reqVO.getGoodsSnNo()));
            } else {
                // 如果提供了自定义SKU，使用定制SKU查询
                log.info("[searchOrderBySnOrSku] 使用定制SKU查询: {}", reqVO.getCustomSku());
                orderList = temuOrderMapper.selectListByCustomSku(Arrays.asList(reqVO.getCustomSku()));
            }
            
            log.info("[searchOrderBySnOrSku] 查询到订单数量: {}", orderList.size());
            
            if (orderList.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 预先查询所有店铺信息
            List<Long> shopIds = orderList.stream()
                    .map(TemuOrderDO::getShopId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, TemuShopDO> shopMap = temuShopMapper.selectByShopIds(shopIds).stream()
                    .collect(Collectors.toMap(TemuShopDO::getShopId, shop -> shop));
            
            // 预先查询所有物流信息
            List<String> orderNos = orderList.stream()
                    .map(TemuOrderDO::getOrderNo)
                    .collect(Collectors.toList());
            Map<String, TemuOrderShippingInfoDO> shippingMap = temuOrderShippingMapper.selectListByOrderNos(orderNos).stream()
                    .collect(Collectors.toMap(TemuOrderShippingInfoDO::getOrderNo, shipping -> shipping));
            
            // 创建返回结果列表
            List<TemuImageSearchOrderRespVO> resultList = new ArrayList<>();
            
            // 遍历订单列表，构建返回结果
            for (TemuOrderDO order : orderList) {
                TemuImageSearchOrderRespVO respVO = new TemuImageSearchOrderRespVO();
                // 复制订单信息
                BeanUtils.copyProperties(order, respVO);
                
                // 设置店铺相关字段
                TemuShopDO shop = shopMap.get(order.getShopId());
                if (shop != null) {
                    respVO.setShopName(shop.getShopName());
                    respVO.setAliasName(shop.getAliasName());
                }
                
                // 设置物流相关字段
                TemuOrderShippingInfoDO shipping = shippingMap.get(order.getOrderNo());
                if (shipping != null) {
                    respVO.setTrackingNumber(shipping.getTrackingNumber());
                    respVO.setExpressImageUrl(shipping.getExpressImageUrl());
                    respVO.setExpressOutsideImageUrl(shipping.getExpressOutsideImageUrl());
                    respVO.setExpressSkuImageUrl(shipping.getExpressSkuImageUrl());
                    respVO.setDailySequence(shipping.getDailySequence());
                    respVO.setShippingTime(shipping.getCreateTime());
                    respVO.setSortingSequence(shipping.getSortingSequence());
                }
                
                resultList.add(respVO);
                log.info("[searchOrderBySnOrSku][查询结果：订单号={}, 商品条码={}, 定制SKU={}]",
                        order.getOrderNo(), order.getSku(), order.getCustomSku());
            }
            
            // 批量更新 isCompleteProducerTask 字段为 1
            if (orderList != null && !orderList.isEmpty()) {
                List<Long> orderIds = orderList.stream()
                    .map(TemuOrderDO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                if (!orderIds.isEmpty()) {
                    temuOrderMapper.updateIsCompleteProducerTaskBatch(orderIds);
                    log.info("已更新生产状态,orderIds为{}",orderIds);
                }
            }
            
            log.info("[searchOrderBySnOrSku] 查询完成，返回结果数量: {}", resultList.size());
            return resultList;
            
        } catch (Exception e) {
            log.error("[searchOrderBySnOrSku] 查询失败", e);
            throw new RuntimeException("根据条码编号或定制SKU查询订单失败", e);
        }
    }

}