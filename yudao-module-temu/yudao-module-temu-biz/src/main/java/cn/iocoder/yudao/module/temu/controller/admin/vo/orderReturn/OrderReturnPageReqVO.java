/*
 * @Author: 徐佳德 1404577549@qq.com
 * @Date: 2025-07-24 14:52:10
 * @LastEditors: 徐佳德 1404577549@qq.com
 * @LastEditTime: 2025-07-28 12:16:40
 * @FilePath: \yulian_erp_back\yudao-module-temu\yudao-module-temu-biz\src\main\java\cn\iocoder\yudao\module\temu\controller\admin\vo\orderReturn\OrderReturnPageReqVO.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Data
public class OrderReturnPageReqVO extends PageParam {
    @Schema(description = "订单编号")
    private String orderNo;
    
    @Schema(description = "SKU")
    private String sku;
    
    @Schema(description = "SKC")
    private String skc;
    
    @Schema(description = "定制SKU")
    private String customSku;
    
    @Schema(description = "返单原因")
    private String repeatReason;
    
    @Schema(description = "开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;
    
    @Schema(description = "结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    @Schema(description = "分页偏移量")
    private Integer offset;
} 