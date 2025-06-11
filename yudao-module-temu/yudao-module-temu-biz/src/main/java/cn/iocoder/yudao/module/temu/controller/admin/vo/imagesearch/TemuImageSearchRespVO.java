package cn.iocoder.yudao.module.temu.controller.admin.vo.imagesearch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 图片搜索结果 Response VO")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemuImageSearchRespVO {

    @Schema(description = "定制sku", required = true, example = "51978533533490")
    private String productId;

    @Schema(description = "相似度得分", required = true, example = "0.98")
    private Float score;
}