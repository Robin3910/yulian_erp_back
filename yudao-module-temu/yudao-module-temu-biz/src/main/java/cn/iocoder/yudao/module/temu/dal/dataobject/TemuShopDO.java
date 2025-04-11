package cn.iocoder.yudao.module.temu.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("temu_shop")
public class TemuShopDO extends BaseDO {
    
    /**
     * 主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 店铺ID
     */
    private Long shopId;
    
    /**
     * 店铺名称
     */
    private String shopName;
    
    /**
     * 信息通知机器人webhook地址
     */
    private String webhook;
 
} 