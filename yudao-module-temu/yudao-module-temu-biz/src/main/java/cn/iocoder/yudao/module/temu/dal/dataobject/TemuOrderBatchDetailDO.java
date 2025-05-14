package cn.iocoder.yudao.module.temu.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TemuOrderBatchDetailDO extends TemuOrderBatchDO {
	private List<TemuOrderDetailDO> orderList;
	private List<TemuOrderBatchTaskUserInfoDO> userList;
}
