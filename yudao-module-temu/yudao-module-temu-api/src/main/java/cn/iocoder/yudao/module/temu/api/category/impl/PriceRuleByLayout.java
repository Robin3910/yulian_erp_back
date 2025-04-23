package cn.iocoder.yudao.module.temu.api.category.impl;

import cn.iocoder.yudao.module.temu.api.category.IPriceRule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 按版面计算价格
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceRuleByLayout implements IPriceRule {
	//单品价格
	private BigDecimal singlePrice;
	//单个版面 默认数量
	private int singleLayoutCount;
	//版面默认价格
	private BigDecimal defaultPrice;
	private List<Rule> unitPrice;
	private String ruleType = "按版面计算价格";
	private String remark;
	
	@Data
	public static class Rule {
		private int max;
		private BigDecimal price;
	}
	
	@Override
	public BigDecimal calcUnitPrice(Integer quantity) {
		//最小价格
		BigDecimal minPrice;
		//假设单品总价格最小
		BigDecimal singleTotalPrice = singlePrice.multiply(new BigDecimal(quantity));
		log.warn("单品数量{},单品价格{}\n",quantity,singleTotalPrice);
		minPrice=singleTotalPrice;
		//获取单品最大需要几个版面
		BigDecimal maxLayoutCount = new BigDecimal(quantity).divide(new BigDecimal(singleLayoutCount), RoundingMode.UP);
		log.warn("最大需要{}个版面\n",maxLayoutCount);
		for (int i = 1; i <= maxLayoutCount.intValue(); i++) {
			BigDecimal currentTotalPrice;
			BigDecimal remainPrice;
			// 当前整版的价格
			BigDecimal layoutTotalPrice = calcLayoutUnitPrice(i).multiply(new BigDecimal(i));
			log.warn("{}个整版价格是{}\n",new BigDecimal(i),layoutTotalPrice);
			
			//检查是是否存在剩余的单个产品 没有就是0  有就加上单个产品*价格
			BigDecimal remainder = new BigDecimal(quantity).subtract(new BigDecimal(i).multiply(new BigDecimal(singleLayoutCount)));
			if (remainder.floatValue() > 0) {
				remainPrice = singlePrice.multiply(remainder);
				log.warn("存在剩余产品数量是{},总的价格是{}\n",remainder,remainPrice);
			} else {
				remainPrice = new BigDecimal(0);
				log.warn("存在剩余产品数量是{},总的价格是{}\n",remainder,remainPrice);
			}
			currentTotalPrice= layoutTotalPrice.add(remainPrice);
			log.warn("计算合并后的价格是{}\n",currentTotalPrice);
			log.warn("比较的大小结果{}",currentTotalPrice.compareTo(minPrice));
	        if (currentTotalPrice.compareTo(minPrice)<=0){
				minPrice=currentTotalPrice;
	        }
		}
		log.warn("最后总价是{}数量是{}单价是{}",minPrice,new BigDecimal(quantity),minPrice.divide(new BigDecimal(quantity), 2, RoundingMode.HALF_UP));
		return minPrice.divide(new BigDecimal(quantity), 6, RoundingMode.HALF_UP);
	}
	
	private BigDecimal calcLayoutUnitPrice(Integer quantity) {
		unitPrice.sort((o1, o2) -> o1.getMax() - o2.getMax());
		for (Rule rule : unitPrice) {
			if (quantity <= rule.getMax()) {
				return rule.getPrice();
			}
		}
		return defaultPrice;
	}
	
	
}
