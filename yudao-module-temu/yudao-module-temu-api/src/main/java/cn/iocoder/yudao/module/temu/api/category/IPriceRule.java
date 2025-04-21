package cn.iocoder.yudao.module.temu.api.category;

import lombok.Data;

import java.math.BigDecimal;

public interface IPriceRule {
	
	BigDecimal calcUnitPrice(Integer quantity);
	
}
