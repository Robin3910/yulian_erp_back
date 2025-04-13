package cn.iocoder.yudao.module.system.dal.dataobject.temu;


import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("temu_shop")
public class AdminTemuShopDO extends BaseDO {
    
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