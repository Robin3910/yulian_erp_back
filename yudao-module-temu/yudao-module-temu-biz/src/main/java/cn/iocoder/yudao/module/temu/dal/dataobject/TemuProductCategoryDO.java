package cn.iocoder.yudao.module.temu.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品品类 DO
 */
@TableName("temu_product_category")
@KeySequence("temu_product_category_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode
public class TemuProductCategoryDO extends BaseDO {

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

} 