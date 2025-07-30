package cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "TemuApi - 物流面单查询 Request VO")
@Data
public class TemuBoxMarkQueryReqVO {

    @Schema(description = "店铺ID", required = true)
    private String shopId;

    @Schema(description = "发货单号列表", required = true)
    @NotEmpty(message = "发货单号列表不能为空")
    private List<String> deliveryOrderSnList;

}