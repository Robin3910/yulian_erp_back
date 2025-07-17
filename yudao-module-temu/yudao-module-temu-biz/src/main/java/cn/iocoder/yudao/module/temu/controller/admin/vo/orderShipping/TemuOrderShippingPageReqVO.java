package cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "Temu管理 - 待发货列表分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemuOrderShippingPageReqVO extends PageParam {
	
	@Schema(description = "订单ID", example = "1024")
	private Long orderId;
	
	@Schema(description = "店铺ID")
	private Long shopId;
	
	@Schema(description = "物流单号")
	private String trackingNumber;
	
	@Schema(description = "订单编号")
	private String orderNo;
	
	@Schema(description = "订单状态", example = "3")
	private Integer orderStatus;
	
	@Schema(description = "定制SKU")
	private String customSku;

//	@Schema(description = "定制SKU列表")
//	private List<String> customSkuList;

	@Schema(description = "类目ID列表")
	private List<String> categoryIds;

	@Schema(description = "是否加急")
	private Boolean isUrgent;

	@Schema(description = "定制文字模糊查询")
	private String customTextList;

	@Schema(description = "批次编号")
	private String batchNo;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@Schema(description = "创建时间", example = "2024-01-01", title = "查询的时间范围")
	private LocalDate[] createTime;

	@Schema(description = "物流单序号")
	private Integer dailySequence;

	@Schema(description = "发货状态（0：未发货；1：已发货）")
	private Integer shippingStatus;

    @Schema(description = "发货人ID")
    private Long senderId;

    @Schema(description = "是否找齐：1-是，0-否")
    private Integer isFoundAll;
	
	@Data
	@Schema(description = "管理后台 - Temu批量更新订单状态 Request VO")
	public static class BatchUpdateStatusReqVO {
		
		@Schema(description = "订单ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotEmpty(message = "订单ID列表不能为空")
		private List<Long> orderIds;
		
		@Schema(description = "订单状态", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "订单状态不能为空")
		private Integer orderStatus;
		
		@Schema(description = "物流单号")
		private String trackingNumber;
	}
	
}
