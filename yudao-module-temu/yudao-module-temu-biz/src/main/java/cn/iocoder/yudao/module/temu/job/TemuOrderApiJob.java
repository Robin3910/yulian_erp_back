package cn.iocoder.yudao.module.temu.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.temu.service.stock.TemuStockPreparationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class TemuOrderApiJob implements JobHandler {

    @Resource
    private TemuStockPreparationService temuStockPreparationService;

    @Override
    @TenantJob
    public String execute(String param) throws Exception {
        System.out.println("执行 TemuOrderApiJob");
        int orders = temuStockPreparationService.saveStockPreparation();
        return String.format("添加订单数量 %s 个", orders);
    }
}
