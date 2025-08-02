package cn.iocoder.yudao.module.temu.service.deliveryOrder;

/**
 * Temu物流信息 Service 接口
 */
public interface ITemuOrderShippingApiService {

    /**
     * 同步指定店铺的物流信息到数据库
     */
    void syncShippingInfo();
    
}
