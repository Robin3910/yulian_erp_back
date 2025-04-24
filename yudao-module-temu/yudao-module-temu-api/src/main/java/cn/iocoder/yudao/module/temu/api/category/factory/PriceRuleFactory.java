package cn.iocoder.yudao.module.temu.api.category.factory;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.temu.api.category.IPriceRule;
import cn.iocoder.yudao.module.temu.api.category.impl.PriceRuleByLayout;
import cn.iocoder.yudao.module.temu.api.category.impl.PriceRuleByNumber;
import lombok.Getter;


public class PriceRuleFactory {
	private enum Rule {
		NUMBER(1, PriceRuleByNumber.class), LAYOUT(2, PriceRuleByLayout.class);
		@Getter
		private final Integer type;
		@Getter
		private final Class<? extends IPriceRule> bean;
		
		Rule(Integer type, Class<? extends IPriceRule> bean) {
			this.type = type;
			this.bean = bean;
		}
	}
	
	public static IPriceRule createPriceRule(Integer type, Object RulePrice) {
		for (Rule rule : Rule.values()) {
			if (rule.getType().equals(type)) {
				return BeanUtils.toBean(RulePrice, rule.getBean());
			}
		}
		throw new RuntimeException("找不到对应的配置");
	}
}
