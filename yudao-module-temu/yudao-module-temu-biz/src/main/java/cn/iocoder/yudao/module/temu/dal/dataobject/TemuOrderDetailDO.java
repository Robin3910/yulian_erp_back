package cn.iocoder.yudao.module.temu.dal.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TemuOrderDetailDO extends TemuOrderDO {
	private String shopName;
	private Object categoryPriceRule;
	private Integer categoryRuleType;
	private String  productCategoryName;
	private BigDecimal defaultPrice;

	// 新增：每日序号
	private Integer dailySequence;
}