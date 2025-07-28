/*
 * @Author: 徐佳德 1404577549@qq.com
 * @Date: 2025-07-24 14:52:38
 * @LastEditors: 徐佳德 1404577549@qq.com
 * @LastEditTime: 2025-07-28 11:03:15
 * @FilePath: \yulian_erp_back\yudao-module-temu\yudao-module-temu-biz\src\main\java\cn\iocoder\yudao\module\temu\controller\admin\vo\orderReturn\OrderReturnPageRespVO.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package cn.iocoder.yudao.module.temu.controller.admin.vo.orderReturn;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderReturnPageRespVO {
    @Schema(description = "返单记录ID")
    private Long id;
    @Schema(description = "原订单号")
    private String orderNo;
    @Schema(description = "SKU")
    private String sku;
    @Schema(description = "SKC")
    private String skc;
    @Schema(description = "定制SKU")
    private String customSku;
    @Schema(description = "返单时间")
    private LocalDateTime createdAt;
    @Schema(description = "店铺ID")
    private Long shopId;
    @Schema(description = "店铺别名")
    private String aliasName;
    @Schema(description = "商品标题")
    private String productTitle;
    @Schema(description = "商品属性")
    private String productProperties;
    @Schema(description = "商品图片")
    private String productImgUrl;
    @Schema(description = "作图人员")
    private String drawUserName;
    @Schema(description = "生产人员")
    private String produceUserName;
    @Schema(description = "发货人员")
    private String shipUserName;
    @Schema(description = "返单原因")
    private int repeatReason;
} 