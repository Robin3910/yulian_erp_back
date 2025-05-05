package cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - Temu店铺合规单查询 Request VO")
@Data
public class TemuShopOldTypeReqVO {

    @Schema(description = "店铺ID", required = true, example = "1024")
    private Long shopId;

    @Schema(description = "SKC编号", required = true, example = "SKC001")
    private String skc;

    @Schema(description = "合规单类型", example = "1")
    private String oldType;
}