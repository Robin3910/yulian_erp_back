package cn.iocoder.yudao.module.temu.dal.dataobject;

import lombok.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;

/**
 * Temu返单记录 DO
 *
 * @author wujunlin
 */
@TableName("temu_order_return")
@KeySequence("temu_order_return_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemuOrderReturnDO  {
    /**
     * 主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 原订单号
     */
    private String orderNo;

    /**
     * SKU
     */
    private String sku;

    /**
     * SKC
     */
    private String skc;

    /**
     * 定制SKU
     */
    private String customSku;
    
    /**
     * 返单时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 店铺ID
     */
    private Long shopId;
    

    /**
     * 商品标题
     */
    private String productTitle;

    /**
     * 商品属性
     */
    private String productProperties;
    
    /**
     * 商品图片
     */
    private String productImgUrl;

    /**
     * 作图人员
     */
    private String drawUserName;
    
    /**
     * 生产人员
     */
    private String produceUserName;
    
    /**
     * 发货人员
     */
    private String shipUserName;

    /**
     * 返单原因
     */
    private int repeatReason;

    /**
     * 店铺名称
     */
    private String aliasName;



}