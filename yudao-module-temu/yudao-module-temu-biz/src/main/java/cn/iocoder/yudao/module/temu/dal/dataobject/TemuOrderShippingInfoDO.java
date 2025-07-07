package cn.iocoder.yudao.module.temu.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待发货列表 DO
 */
@Data
@TableName("temu_order_shipping_info")
public class TemuOrderShippingInfoDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 订单编号
     */
    @TableField("order_no")
    private String orderNo;
    /**
     * 物流单号
     */
    @TableField("tracking_number")
    private String trackingNumber;
    /**
     * 快递面单图片URL
     */
    @TableField("express_image_url")
    private String expressImageUrl;
    /**
     * 快递面单外单图片URL
     */
    @TableField("express_outside_image_url")
    private String expressOutsideImageUrl;
    /**
     * 快递面单SKU图片URL
     */
    @TableField("express_sku_image_url")
    private String expressSkuImageUrl;
    /**
     * 店铺ID
     */
    @TableField("shop_id")
    private Long shopId;
    /**
     * 物流订单发货状态（0未发货，1已发货）
     */
    @TableField("shipping_status")
    private Integer shippingStatus;
    /**
     * 是否加急
     */
    @TableField("is_urgent")
    private Boolean isUrgent;
    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
    /**
     * 每日序号 - 每天从1开始
     */
    @TableField("daily_sequence")
    private Integer dailySequence;
    /**
     * 发货操作人ID
     */
    @TableField("shipped_operator_id")
    private Long shippedOperatorId;
    /**
     * 分拣序号，用于标识订单分拣的顺序
     */
    @TableField("sorting_sequence")
    private Integer sortingSequence;
}