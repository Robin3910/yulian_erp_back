package cn.iocoder.yudao.module.temu.dal.dataobject;

import lombok.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 操作日志 DO
 *
 * @author 禹链科技
 */
@TableName("temu_operation_log")
@KeySequence("temu_operation_log_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemuOperationLogDO extends BaseDO {

    /**
     * 主键，自增
     */
    @TableId
    private Long id;
    /**
     * 用户 ID
     */
    private String userId;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
    /**
     * 操作模块 
     */
    private String module;
    /**
     * 操作类型
     */
    private String operationType;
    /**
     * 请求参数
     */
    private String requestParams;
    /**
     * 响应结果
     */
    private String responseResult;
    /**
     * 操作 IP 地址
     */
    private String ipAddress;
    /**
     * 类名
     */
    private String className;
    /**
     * 方法名
     */
    private String methodName;

}