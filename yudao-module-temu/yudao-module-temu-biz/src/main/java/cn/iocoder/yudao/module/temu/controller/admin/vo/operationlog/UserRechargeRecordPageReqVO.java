package cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 用户充值记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserRechargeRecordPageReqVO extends PageParam {

    @Schema(description = "用户ID", example = "123")
    private String userId;

    @Schema(description = "用户昵称", example = "张三")
    private String nickname;

    @Schema(description = "充值时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] rechargeTime;

    @Schema(description = "支付状态", example = "10")
    private Integer payStatus;

    @Schema(description = "操作IP地址", example = "192.168.1.1")
    private String ip;
} 