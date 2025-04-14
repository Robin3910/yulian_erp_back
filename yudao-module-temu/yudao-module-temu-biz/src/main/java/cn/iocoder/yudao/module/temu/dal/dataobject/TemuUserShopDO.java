package cn.iocoder.yudao.module.temu.dal.dataobject;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 用户与店铺关系绑定 DO
 *
 * @author 禹链科技
 */
@TableName("temu_user_shop")
@KeySequence("temu_user_shop_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemuUserShopDO extends BaseDO {
	
	/**
	 * 主键
	 */
	@TableId
	private Long id;
	/**
	 * 用户ID
	 */
	private Long userId;
	/**
	 * 店铺ID
	 */
	private Long shopId;
}