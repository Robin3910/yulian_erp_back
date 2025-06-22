package cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

@Schema(description = "管理后台 - 操作日志 Response VO")
@Data
@ExcelIgnoreUnannotated
public class TemuOperationLogRespVO {

    @Schema(description = "用户ID", example = "123")
    @ExcelProperty("用户ID")
    private String userId;

    @Schema(description = "用户名", example = "张三")
    @ExcelProperty("用户名")
    private String userName;

    @Schema(description = "操作时间")
    @ExcelProperty("操作时间")
    private LocalDateTime operationTime;

    @Schema(description = "操作模块 ", example = "用户管理")
    @ExcelProperty("操作模块 ")
    private String module;

    @Schema(description = "操作类型", example = "保存订单")
    @ExcelProperty("操作类型")
    private String operationType;

    @Schema(description = "操作 IP 地址", example = "113.46.66.33")
    @ExcelProperty("操作 IP 地址")
    private String ipAddress;

    @Schema(description = "类名", example = "AdminTemuOrderController")
    @ExcelProperty("类名")
    private String className;

    @Schema(description = "方法名", example = "list")
    @ExcelProperty("方法名")
    private String methodName;

    @Schema(description = "请求参数", example = "{\"orderNo\":\"123456\"}")
    @ExcelProperty("请求参数")
    private String requestParams;

    @Schema(description = "响应结果", example = "{\"code\":200,\"data\":{}}")
    @ExcelProperty("响应结果")
    private String responseResult;

}