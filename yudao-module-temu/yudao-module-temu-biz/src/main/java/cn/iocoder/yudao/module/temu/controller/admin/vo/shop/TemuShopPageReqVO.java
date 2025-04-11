package cn.iocoder.yudao.module.temu.controller.admin.vo.shop;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 店铺分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemuShopPageReqVO extends PageParam {
    
    @Schema(description = "店铺ID", example = "2048")
    private Long shopId;
    
    @Schema(description = "店铺名称", example = "测试店铺")
    private String shopName;
    
    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] createTime;
} 