package cn.iocoder.yudao.module.temu.controller.admin.vo.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Schema(description = "Temu订单保存请求VO")
@Data
public class TemuOrderSaveRequestVO {

    @Schema(description = "店铺ID", required = true)
    @NotEmpty(message = "店铺ID不能为空")
    private String shopId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "订单列表", required = true)
    @NotNull(message = "订单列表不能为空")
    private OrdersWrapper orders;

    @Data
    public static class OrdersWrapper {
        @Schema(description = "订单数组")
        private List<Map<String, Object>> orders;
    }
} 