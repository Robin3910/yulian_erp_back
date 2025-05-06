package cn.iocoder.yudao.module.temu.controller.admin.vo.shopBatch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - Temu店铺合规单删除 Request VO")
@Data
public class TemuShopOldTypeDeleteReqVO {

    @Schema(description = "店铺ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    @Schema(description = "合规单类型")
    private String oldType;

    @Schema(description = "SKC编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "SKC编号列表不能为空")
    private List<String> skcList;
} 