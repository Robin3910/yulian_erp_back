package cn.iocoder.yudao.module.temu.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("temu_order_placement_record_vip")
public class TemuVipOrderPlacementRecordDO {
    @TableId
    private Long id;
    private String orderNo;
    private Long shopId;
    private String shopName;
    private String productTitle;
    private String productProperties;
    private Long categoryId;
    private String categoryName;
    private Integer originalQuantity;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String sku;
    private String skc;
    private String customSku;
    private LocalDateTime operationTime;
    private Boolean isReturnOrder;
    private Long operatorId;
    private String operator;
}
