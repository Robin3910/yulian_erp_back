package cn.iocoder.yudao.module.temu.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("temu_order_batch_category")
public class TemuOrderBatchCategoryMpDO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchCategoryId;

    private Long categoryId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String creator;

    private String updater;

    @TableLogic(value = "0", delval = "1")
    private Boolean deleted;

    private Long tenantId;
}
