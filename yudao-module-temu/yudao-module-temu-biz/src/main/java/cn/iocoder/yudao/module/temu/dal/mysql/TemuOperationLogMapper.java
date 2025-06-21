package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.temu.controller.admin.vo.operationlog.TemuOperationLogPageReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOperationLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 *
 * @author 禹链科技
 */
@Mapper
public interface TemuOperationLogMapper extends BaseMapperX<TemuOperationLogDO> {

    default PageResult<TemuOperationLogDO> selectPage(TemuOperationLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TemuOperationLogDO>()
                .likeIfPresent(TemuOperationLogDO::getUserName, reqVO.getUserName())
                .betweenIfPresent(TemuOperationLogDO::getOperationTime, reqVO.getOperationTime())
                .likeIfPresent(TemuOperationLogDO::getModule, reqVO.getModule())
                .likeIfPresent(TemuOperationLogDO::getIpAddress, reqVO.getIpAddress())
                .likeIfPresent(TemuOperationLogDO::getClassName, reqVO.getClassName())
                .likeIfPresent(TemuOperationLogDO::getMethodName, reqVO.getMethodName())
                .orderByDesc(TemuOperationLogDO::getId));
    }

}