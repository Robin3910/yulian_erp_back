package cn.iocoder.yudao.module.system.dal.dataobject.temu;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_temu")
@Data
@EqualsAndHashCode(callSuper = true)
public class TemuDO extends BaseDO {

    @TableId
    private Long id;
    
    private String content;
} 