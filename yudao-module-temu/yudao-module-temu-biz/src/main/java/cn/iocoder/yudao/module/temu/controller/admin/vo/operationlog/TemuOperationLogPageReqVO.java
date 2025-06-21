package cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 操作日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TemuOperationLogPageReqVO extends PageParam {

    @Schema(description = "用户名", example = "张三")
    private String userName;

    @Schema(description = "操作时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] operationTime;

    @Schema(description = "操作模块 ", example = "用户管理")
    private String module;

    @Schema(description = "操作 IP 地址", example = "113.46.66.33")
    private String ipAddress;

    @Schema(description = "类名", example = "AdminTemuOrderController")
    private String className;

    @Schema(description = "方法名", example = "list")
    private String methodName;

}