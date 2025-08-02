package cn.iocoder.yudao.module.temu.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.temu.service.deliveryOrder.ITemuOrderShippingApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class TemuOrderShippingApiJob implements JobHandler {

    @Resource
    private ITemuOrderShippingApiService shippingApiService;

    @Override
    @TenantJob
    public String execute(String param) throws Exception {
        System.out.println("执行 TemuOrderApiJob");
        shippingApiService.syncShippingInfo();
        return String.format("1");
    }
}
