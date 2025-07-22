package cn.iocoder.yudao.module.temu.controller.admin.vo.goods;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "TemuApi - 定制品商品条码查询 Response VO")
@Data
public class TemuCustomGoodsLabelRespVO {

    @Schema(description = "分页查询结果")
    private PersonalLabelCodePageResult personalLabelCodePageResult;

    @Data
    public static class PersonalLabelCodePageResult {
        @Schema(description = "结果列表")
        private List<LabelCodeItem> data;

        @Schema(description = "总数")
        private Integer totalCount;
    }

    @Data
    public static class LabelCodeItem {
        @Schema(description = "sku规格多语言信息")
        private Map<String, List<SpecVO>> productSkuSpecI18nMap;

        @Schema(description = "sku信息")
        private ProductSkuDTO productSkuDTO;

        @Schema(description = "skc图片信息")
        private List<ProductSkcImage> productSkcImageList;

        @Schema(description = "skc信息")
        private ProductSkcDTO productSkcDTO;

        @Schema(description = "货品产地信息")
        private ProductOrigin productOrigin;

        @Schema(description = "标签条码基础信息")
        private ProductSkuLabelCodeDTO productSkuLabelCodeDTO;

        @Schema(description = "skc规格多语言信息")
        private Map<String, List<SpecVO>> productSkcSpecI18nMap;
    }

    @Data
    public static class SpecVO {
        @Schema(description = "规格id")
        private Integer specId;

        @Schema(description = "父规格名称")
        private String parentSpecName;

        @Schema(description = "父规格id")
        private Integer parentSpecId;

        @Schema(description = "规格名称")
        private String specName;
    }

    @Data
    public static class ProductSkuDTO {
        @Schema(description = "件数")
        private Integer numberOfPieces;

        @Schema(description = "货品skuId")
        private Long productSkuId;

        @Schema(description = "货品id")
        private Long productId;

        @Schema(description = "sku规格列表，含当前sku所有规格属性")
        private ProductSkuSpec productSkuSpec;

        @Schema(description = "货品skcId")
        private Long productSkcId;

        @Schema(description = "sku件数单位")
        private Integer pieceUnitCode;

        @Schema(description = "sku货号")
        private String extCode;

        @Schema(description = "sku分类")
        private Integer skuClassification;

        @Schema(description = "sku预览图")
        private String thumbUrl;
    }

    @Data
    public static class ProductSkuSpec {
        @Schema(description = "货品sku id")
        private Long productSkuId;

        @Schema(description = "货品id")
        private Long productId;

        @Schema(description = "规格信息")
        private List<SpecVO> specList;

        @Schema(description = "货品skcId")
        private Long productSkcId;
    }

    @Data
    public static class ProductSkcImage {
        @Schema(description = "图片URL")
        private String imageUrl;

        @Schema(description = "语言")
        private String language;

        @Schema(description = "图片类型")
        private Integer imageType;
    }

    @Data
    public static class ProductSkcDTO {
        @Schema(description = "主销售属性id列表")
        private List<Integer> specIdList;

        @Schema(description = "skc货号")
        private String extCode;

        @Schema(description = "货品Id")
        private Long productId;

        @Schema(description = "主销售属性详情")
        private ProductSkcSpec productSkcSpec;

        @Schema(description = "货品skcId")
        private Long productSkcId;
    }

    @Data
    public static class ProductSkcSpec {
        @Schema(description = "货品id")
        private Long productId;

        @Schema(description = "规格信息")
        private List<SpecVO> specList;

        @Schema(description = "货品skcId")
        private Long productSkcId;
    }

    @Data
    public static class ProductOrigin {
        @Schema(description = "一级区域简称 (二字简码)")
        private String region1ShortName;

        @Schema(description = "一级区域名称 (英文)")
        private String region1Name;
    }

    @Data
    public static class ProductSkuLabelCodeDTO {
        @Schema(description = "货品sku id")
        private Long productSkuId;

        @Schema(description = "货品id")
        private Long productId;

        @Schema(description = "创建时间戳 (毫秒)")
        private Long createTimeTs;

        @Schema(description = "货品skc id")
        private Long productSkcId;

        @Schema(description = "标签条码")
        private Long labelCode;
    }
}