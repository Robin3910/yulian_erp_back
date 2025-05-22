package cn.iocoder.yudao.module.temu.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("temu_order_batch_category")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TemuOrderBatchCategoryDO extends BaseDO {
    @TableId
    private Long id;
    // 批次所属类目id
    private String batchCategoryId;
    // 类目ID
    private String categoryId;

}
