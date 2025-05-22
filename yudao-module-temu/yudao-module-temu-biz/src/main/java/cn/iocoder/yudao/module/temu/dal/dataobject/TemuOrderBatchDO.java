package cn.iocoder.yudao.module.temu.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("temu_order_batch")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TemuOrderBatchDO extends BaseDO {
	@TableId
	private Long id;
	// 批次号
	private String batchNo;
	// 文件地址
	private String fileUrl;
	// 状态
	private Integer status;
	// 备注
	private String remark;
	// 是否派单
	private Integer isDispatchTask;
	// 批次所属类目id
	private String batchCategoryId;
}
