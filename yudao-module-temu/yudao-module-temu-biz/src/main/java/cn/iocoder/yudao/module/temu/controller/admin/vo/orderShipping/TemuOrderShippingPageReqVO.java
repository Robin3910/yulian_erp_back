package cn.iocoder.yudao.module.temu.controller.admin.vo.orderShipping;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Schema(description = "Temu管理 - 待发货列表分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemuOrderShippingPageReqVO extends PageParam {

    private Long shopId;

    private String trackingNumber;

    private String orderNo;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate[] createTime;





}
