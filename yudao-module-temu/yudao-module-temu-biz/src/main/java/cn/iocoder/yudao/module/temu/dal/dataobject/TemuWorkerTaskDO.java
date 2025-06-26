package cn.iocoder.yudao.module.temu.dal.dataobject;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 工作人员任务记录 DO
 *
 * @author wujunlin
 */
@TableName("temu_worker_task")
@KeySequence("temu_worker_task_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemuWorkerTaskDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 工作人员ID，关联system_users表id
     */
    private Long workerId;
    /**
     * 工作人员姓名，关联system_users表nickname
     */
    private String workerName;
    /**
     * 任务类型 1:作图 2:生产 3:发货
     */
    private Byte taskType;
    /**
     * 任务状态 0:待处理 1:已完成 2:已取消
     */
    private Byte taskStatus;
    /**
     * 关联订单ID，对应temu_order表id
     */
    private Long orderId;
    /**
     * 订单编号，对应temu_order表order_no
     */
    private String orderNo;
    /**
     * 批次订单ID，对应temu_order_batch_task表batch_order_id
     */
    private Long batchOrderId;
    /**
     * 定制SKU，对应temu_order表custom_sku
     */
    private String customSku;
    /**
     * 处理SKU数量，对应temu_order表quantity
     */
    private Integer skuQuantity;
    /**
     * 任务完成时间
     */
    private LocalDateTime taskCompleteTime;
    /**
     * 店铺ID，对应temu_order表shop_id
     */
    private Long shopId;

}