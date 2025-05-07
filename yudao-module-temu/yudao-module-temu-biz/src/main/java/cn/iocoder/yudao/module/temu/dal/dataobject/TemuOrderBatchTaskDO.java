package cn.iocoder.yudao.module.temu.dal.dataobject;

import lombok.*;

import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 订单批次任务分配 DO
 *
 * @author 禹链科技
 */
@TableName("temu_order_batch_task")
@KeySequence("temu_order_batch_task_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemuOrderBatchTaskDO extends BaseDO {
	
	/**
	 * 主键
	 */
	@TableId
	private Long id;
	/**
	 * 批次订单id
	 */
	private Long batchOrderId;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 1 作图 2 生产
	 */
	private Integer type;
	/**
	 * 0已分配 1待处理  2已完成
	 */
	private Integer status;
	/**
	 * 后续关联任务类型
	 */
	private Integer nextTaskType;
	
}