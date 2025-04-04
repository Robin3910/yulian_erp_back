package cn.iocoder.yudao.module.temu.controller.admin.dal.dataobject;

import lombok.*;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 商品品类 DO
 *
 * @author 芋道源码
 */
@TableName("temu_product_category")
@KeySequence("temu_product_category_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemuProductCateGoryDO extends BaseDO {
	
	/**
	 * 主键ID
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
	
}