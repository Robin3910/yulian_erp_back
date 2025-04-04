package cn.iocoder.yudao.module.temu.controller.admin.controller.vo.client;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;

@Data
public class TemuOrderRequestVO extends PageParam {
	// 店铺id
	private String shopId;
	private String sku;
	private String skc;
	private String customSku;
	private String orderStatus;
	private String categoryId;
}
