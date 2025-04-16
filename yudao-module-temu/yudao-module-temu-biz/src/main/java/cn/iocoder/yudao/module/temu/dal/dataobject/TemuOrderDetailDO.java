package cn.iocoder.yudao.module.temu.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TemuOrderDetailDO extends TemuOrderDO {
	private String shopName;
	private List<TemuProductCategoryDO.UnitPrice> categoryPriceRule;
	private BigDecimal defaultPrice;
}