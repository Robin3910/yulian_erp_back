package cn.iocoder.yudao.module.temu.controller.admin.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "批量插入订单请求 VO")
public class TemuOrderBatchInsertReqVO {
    @Schema(description = "中包序号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> sortingSequenceList;

    @Schema(description = "发货人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long senderId;

    @Schema(description = "条件判断标志", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean conditionFlag;
} 