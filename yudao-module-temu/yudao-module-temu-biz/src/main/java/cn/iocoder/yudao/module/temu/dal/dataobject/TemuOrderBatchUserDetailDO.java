package cn.iocoder.yudao.module.temu.dal.dataobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TemuOrderBatchUserDetailDO extends TemuOrderBatchDO {
	private Long taskUserId;
	private Integer taskType;
	private Long taskId;
	private Integer taskStatus;
	private List<TemuOrderDetailDO> orderList;
	private List<TemuOrderBatchTaskUserInfoDO> userList;
}
