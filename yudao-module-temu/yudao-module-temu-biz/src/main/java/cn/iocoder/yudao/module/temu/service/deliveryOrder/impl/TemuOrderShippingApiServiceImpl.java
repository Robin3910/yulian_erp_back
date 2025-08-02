package cn.iocoder.yudao.module.temu.service.deliveryOrder.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderQueryReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderSimpleVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping.TemuOrderShippingRespVO;
import cn.iocoder.yudao.module.temu.service.deliveryOrder.ITemuOrderShippingApiService;
import cn.iocoder.yudao.module.temu.service.deliveryOrder.TemuDeliveryOrderConvertService;
import cn.iocoder.yudao.module.temu.service.order.ITemuOrderShippingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Temu物流信息 Service 实现类
 */
@Service
@Slf4j
public class TemuOrderShippingApiServiceImpl implements ITemuOrderShippingApiService {

    @Resource
    private TemuDeliveryOrderConvertService deliveryOrderConvertService;

    @Resource
    private ITemuOrderShippingService temuOrderShippingService;

    @Resource
    private ConfigApi configApi;

    private List<String> getAuthorizedShopIds() {
        String shopIdsStr = configApi.getConfigValueByKey("temu.authorized.shop.ids");
        log.info("[getAuthorizedShopIds] 从配置中获取已授权店铺列表: {}", shopIdsStr);
        if (shopIdsStr == null || shopIdsStr.trim().isEmpty()) {
            log.warn("[getAuthorizedShopIds] 未配置已授权店铺列表，使用默认值");
            // 默认值
            return Arrays.asList("634418218920700");
        }
        // 将配置字符串分割成列表（配置格式：店铺ID用逗号分隔）
        List<String> shopIds = Arrays.asList(shopIdsStr.split(" "));
        // 去除空格
        shopIds = shopIds.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        log.info("[getAuthorizedShopIds] 已授权店铺列表: {}", shopIds);
        return shopIds;
    }

    @Override
    public void syncShippingInfo() {
            List<String> authorizedShopIds = getAuthorizedShopIds();
            log.info("[syncShippingInfo] 开始同步所有已授权店铺的物流信息，店铺数量：{}", authorizedShopIds.size());
            for (String authorizedShopId : authorizedShopIds) {
                try {
                    log.info("[syncShippingInfo] 开始同步店铺 {} 的物流信息", authorizedShopId);
                    syncShippingInfoForShop(authorizedShopId);
                    log.info("[syncShippingInfo] 店铺 {} 的物流信息同步完成", authorizedShopId);

                    // 每个店铺同步完成后休眠5秒，避免对数据库造成过大压力
                    try {
                        log.info("[syncShippingInfo] 休眠3秒后继续同步下一个店铺");
                        Thread.sleep(8000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("[syncShippingInfo] 休眠被中断", e);
                    }
                } catch (Exception e) {
                    log.error("[syncShippingInfo] 店铺 {} 的物流信息同步失败", authorizedShopId, e);
                    // 即使同步失败，也等待5秒后再同步下一个店铺
                    try {
                        log.info("[syncShippingInfo] 同步失败，休眠5秒后继续同步下一个店铺");
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("[syncShippingInfo] 休眠被中断", ie);
                    }
                }
            }
            log.info("[syncShippingInfo] 所有店铺的物流信息同步完成");
            return;

    }

    private void syncShippingInfoForShop(String shopId) {
        try {
            int pageNo = 1;
            final int pageSize = 50; // 将每页数量从100降到50，减轻数据库压力
            long total = 0;
            int totalSaved = 0;

            // 用于收集所有分页数据
            List<TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO> allSaveRequestVOs = new ArrayList<>();

            do {
                // 1. 构建查询参数
                TemuDeliveryOrderQueryReqVO reqVO = new TemuDeliveryOrderQueryReqVO();
                reqVO.setShopId(shopId);
                reqVO.setStatus(1);
                reqVO.setPageNo(pageNo);
                reqVO.setPageSize(pageSize);

                // 2. 调用API获取物流信息
                PageResult<TemuDeliveryOrderSimpleVO> pageResult = deliveryOrderConvertService.queryTemuLogisticsPage(reqVO);
                List<TemuDeliveryOrderSimpleVO> list = pageResult.getList();
                total = pageResult.getTotal(); // 获取总记录数

                if (list == null || list.isEmpty()) {
                    break;
                }

                // 3. 准备批量保存的数据
                for (TemuDeliveryOrderSimpleVO vo : list) {
                    // 创建保存请求VO
                    TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO saveRequestVO = new TemuOrderShippingRespVO.TemuOrderShippingSaveRequestVO();
                    saveRequestVO.setOrderNo(vo.getSubPurchaseOrderSn());
                    saveRequestVO.setShippingTime(LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(vo.getDeliverTime()),
                            ZoneId.systemDefault()
                    ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    saveRequestVO.setTrackingNumber(vo.getExpressDeliverySn());
                    saveRequestVO.setShopId(Long.valueOf(shopId));

                    allSaveRequestVOs.add(saveRequestVO);
                }

                log.info("[syncShippingInfo][shopId: {}] 第{}页获取物流信息成功，本页数量: {}, 累计获取数量: {}, 总数据量: {}",
                        shopId, pageNo, list.size(), allSaveRequestVOs.size(), total);

                pageNo++; // 下一页
            } while (allSaveRequestVOs.size() < total); // 当已获取数量小于总数时继续循环

            // 4. 所有数据收集完成后，一次性批量保存到数据库
            if (!allSaveRequestVOs.isEmpty()) {
                totalSaved = temuOrderShippingService.batchSaveOrderShippingV2(allSaveRequestVOs);
                log.info("[syncShippingInfo][shopId: {}] 同步完成，总共保存: {} 条记录", shopId, totalSaved);
            }

            // 每个店铺同步完成后休眠3秒，避免对数据库造成过大压力
            try {
                log.info("[syncShippingInfo] 休眠5秒后继续同步下一个店铺");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[syncShippingInfo] 休眠被中断", e);
            }
        } catch (Exception e) {
            log.error("[syncShippingInfo][shopId: {}] 同步物流信息失败", shopId, e);
            throw new RuntimeException("同步物流信息失败", e);
        }
    }
}