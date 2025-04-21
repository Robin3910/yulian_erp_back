package cn.iocoder.yudao.module.temu.dal.dataobject;

import cn.hutool.json.JSONObject;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品品类 DO
 */
@TableName(value = "temu_product_category", autoResultMap = true)
@KeySequence("temu_product_category_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode

public class TemuProductCategoryDO extends BaseDO {
	@Data
	public static class UnitPrice {
		private BigDecimal price;
		private Integer max;
	}
	
	/**
	 * 品类编号
	 */
	@TableId
	private Long id;
	/**
	 * 商品品类ID
	 */
	private Long categoryId;
	/**
	 * 商品名称
	 */
	private String categoryName;
	/**
	 * 长度(cm)
	 */
	private BigDecimal length;
	/**
	 * 宽度(cm)
	 */
	private BigDecimal width;
	/**
	 * 高度(cm)
	 */
	private BigDecimal height;
	/**
	 * 重量(g)
	 */
	private BigDecimal weight;
	/**
	 * 主图URL
	 */
	private String mainImageUrl;
	
	/**
	 * 定价类型
	 */
	private Integer ruleType;
	
	/**
	 * 定价规则(JSON格式)
	 */
	@TableField(typeHandler = JacksonTypeHandler.class)
	private Object unitPrice;
	
	
} 