package cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - Temu店铺合规单查询 Response VO")
@Data
public class TemuShopOldTypeRespVO {

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "skc编号")
    private String skc;

    @Schema(description = "合规单URL")
    private String oldTypeUrl;

    @Schema(description = "合规单类型")
    private String oldType;
}
