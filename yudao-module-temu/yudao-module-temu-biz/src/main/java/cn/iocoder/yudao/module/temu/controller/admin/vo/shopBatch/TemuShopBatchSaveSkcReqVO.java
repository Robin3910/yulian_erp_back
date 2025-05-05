package cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Temu批量保存合规单SKC 请求VO")
@Data
public class TemuShopBatchSaveSkcReqVO {

    @Schema(description = "店铺ID")
    private String shopId;

    @Schema(description = "skc编号")
    private String skc;

    @Schema(description = "合规单URL")
    private String oldTypeUrl;

    @Schema(description = "合规单类型")
    private String oldType;

}

