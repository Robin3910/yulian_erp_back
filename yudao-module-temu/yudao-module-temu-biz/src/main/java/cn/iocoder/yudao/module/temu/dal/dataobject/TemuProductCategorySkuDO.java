package cn.iocoder.yudao.module.temu.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 商品品类SKU关系 DO
 */
@TableName("temu_product_category_sku")
@KeySequence("temu_product_category_sku_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode
public class TemuProductCategorySkuDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    
    /**
     * 商品品类ID
     */
    private Long categoryId;

    /**
     * 商品品类名称
     */
    private String categoryName;
    
    /**
     * SKU
     */
    private String sku;
    
    /**
     * 店铺ID
     */
    private Long shopId;
    
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 