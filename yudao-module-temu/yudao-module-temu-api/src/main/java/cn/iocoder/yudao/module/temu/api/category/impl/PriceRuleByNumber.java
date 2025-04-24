package cn.iocoder.yudao.module.temu.api.category.impl;

import cn.iocoder.yudao.module.temu.api.category.IPriceRule;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 按数量计算价格
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceRuleByNumber implements IPriceRule {
	
	private BigDecimal defaultPrice;
	private List<Rule> unitPrice;
	private String ruleType="按数量计算价格";
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",shape = JsonFormat.Shape.STRING,timezone = "GMT+8")
	private Date time= new Date();
	
	@Override
	public BigDecimal calcUnitPrice(Integer quantity) {
		//   按照从小到大的顺序排序
		unitPrice.sort((o1, o2) -> o1.getMax() - o2.getMax());
		for (Rule rule : unitPrice) {
			if (quantity <= rule.getMax()) {
				return rule.getPrice();
			}
		}
		return defaultPrice;
		
	}
	
	@Data
	public static class Rule {
		private int max;
		private BigDecimal price;
	}
}
