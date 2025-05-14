package cn.iocoder.yudao.module.temu.dal.dataobject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemuOrderBatchTaskUserInfoDO extends TemuOrderBatchTaskDO {
	private String nickName;
}
