package cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

@Schema(description = "管理后台 - 用户充值记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class UserRechargeRecordRespVO {

    @Schema(description = "用户ID", example = "123")
    @ExcelProperty("用户ID")
    private String userId;

    @Schema(description = "用户昵称", example = "张三")
    @ExcelProperty("用户昵称")
    private String nickname;

    @Schema(description = "充值时间")
    @ExcelProperty("充值时间")
    private LocalDateTime rechargeTime;

    @Schema(description = "充值金额", example = "100.00")
    @ExcelProperty("充值金额")
    private BigDecimal amount;

    @Schema(description = "操作IP地址", example = "192.168.1.1")
    @ExcelProperty("操作IP地址")
    private String ip;

    @Schema(description = "支付状态", example = "10")
    @ExcelProperty("支付状态")
    private Integer payStatus;

    @Schema(description = "支付状态名称", example = "支付成功")
    @ExcelProperty("支付状态名称")
    private String payStatusName;

    @Schema(description = "支付订单ID", example = "1001")
    @ExcelProperty("支付订单ID")
    private Long payOrderId;
} 