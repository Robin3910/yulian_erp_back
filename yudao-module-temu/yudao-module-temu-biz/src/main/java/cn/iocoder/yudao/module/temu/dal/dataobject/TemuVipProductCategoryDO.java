package cn.iocoder.yudao.module.temu.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@TableName(value = "temu_product_category_vip", autoResultMap = true)
@Data
@EqualsAndHashCode
public class TemuVipProductCategoryDO extends BaseDO {
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

    // 合规单类型
    private String oldType;
}
