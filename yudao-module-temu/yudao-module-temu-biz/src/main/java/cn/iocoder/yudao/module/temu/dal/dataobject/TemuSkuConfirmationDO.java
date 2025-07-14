package cn.iocoder.yudao.module.temu.dal.dataobject;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * SKU确认 DO
 */
@TableName("temu_sku_confirmation")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TemuSkuConfirmationDO extends BaseDO {
    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 店铺ID
     */
    private String shopId;

    /**
     * SKU编号
     */
    private String sku;

    /**
     * 状态：0-未确认，1-已确认
     */
    private Integer status;
}