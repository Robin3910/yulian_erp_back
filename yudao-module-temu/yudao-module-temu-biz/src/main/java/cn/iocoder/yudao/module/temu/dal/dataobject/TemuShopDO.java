package cn.iocoder.yudao.module.temu.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "temu_shop", autoResultMap = true)
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

    /**
     * 合规单类型url
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> oldTypeUrl;
 
} 