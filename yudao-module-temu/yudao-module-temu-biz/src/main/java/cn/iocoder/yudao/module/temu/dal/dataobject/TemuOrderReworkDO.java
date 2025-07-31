package cn.iocoder.yudao.module.temu.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 订单返工 DO
 *
 * @author 芋道源码
 */
@TableName("temu_order_rework")
@KeySequence("temu_order_rework_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemuOrderReworkDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 订单编号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 商品标题
     */
    @TableField("product_title")
    private String productTitle;

    /**
     * 商品图片URL
     */
    @TableField("product_img_url")
    private String productImgUrl;

    /**
     * 商品属性
     */
    @TableField("product_properties")
    private String productProperties;

    /**
     * SKU编号
     */
    @TableField("sku")
    private String sku;

    /**
     * SKC编号
     */
    @TableField("skc")
    private String skc;

    /**
     * 定制SKU
     */
    @TableField("custom_sku")
    private String customSku;

    /**
     * 返工原因
     */
    @TableField("rework_reason")
    private String reworkReason;

    /**
     * 返工发起人
     */
    @TableField("rework_initiator_name")
    private String reworkInitiatorName;

    /**
     * 返工作图人
     */
    @TableField("rework_draw_user_name")
    private String reworkDrawUserName;

    /**
     * 返工作图人ID
     */
    @TableField("rework_draw_user_id")
    private Long reworkDrawUserId;

    /**
     * 上一次返工作图人
     */
    @TableField("last_draw_user_name")
    private String lastDrawUserName;

    /**
     * 上一次返工作图人ID
     */
    @TableField("last_draw_user_id")
    private Long lastDrawUserId;

    /**
     * 是否完成 0未完成 1已完成
     */
    @TableField("is_finished")
    private Integer isFinished;

    /**
     * 返工次数
     */
    @TableField("rework_count")
    private Integer reworkCount;

    /**
     * 定制图片列表URL
     */
    @TableField("custom_image_urls")
    private String customImageUrls;

    /**
     * 定制文字列表
     */
    @TableField("custom_text_list")
    private String customTextList;

    /**
     * 店铺ID
     */
    @TableField("shop_id")
    private Long shopId;

} 