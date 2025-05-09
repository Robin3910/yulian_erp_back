package cn.iocoder.yudao.module.temu.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Map;

@Data
@TableName(value = "temu_shop_old_type_skc", autoResultMap = true)
public class TemuShopOldTypeSkcDO extends BaseDO {

    @TableId
    private Long id;

    //店铺ID
    private Long shopId;

    //SKC编号
    private String skc;

    //合规单URL
    private String oldTypeUrl;

    //合规单图片URL
    private String oldTypeImageUrl;

    //合规单类型
    private String oldType;
}
