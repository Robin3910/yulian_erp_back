package cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 操作日志新增/修改 Request VO")
@Data
public class TemuOperationLogSaveReqVO {

    @Schema(description = "用户 ID", example = "666")
    private String userId;

    @Schema(description = "用户名", example = "张三")
    private String userName;

    @Schema(description = "操作时间")
    private LocalDateTime operationTime;

    @Schema(description = "操作模块 ", example = "用户管理")
    private String module;

    @Schema(description = "操作类型", example = "保存订单")
    private String operationType;

    @Schema(description = "请求参数")
    private String requestParams;

    @Schema(description = "响应结果")
    private String responseResult;

    @Schema(description = "操作 IP 地址", example = "113.46.66.33")
    private String ipAddress;

    @Schema(description = "类名", example = "AdminTemuOrderController")
    private String className;

    @Schema(description = "方法名", example = "list")
    private String methodName;

}