package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TemuShopMapper extends BaseMapperX<TemuShopDO> {
    
    /**
     * 根据店铺ID查询店铺
     *
     * @param shopId 店铺ID
     * @return 店铺
     */
    default TemuShopDO selectByShopId(Long shopId) {
        return selectOne(TemuShopDO::getShopId, shopId);
    }
    
    /**
     * 分页查询店铺
     *
     * @param shopId 店铺ID
     * @param shopName 店铺名称
     * @return 分页结果
     */
    default PageResult<TemuShopDO> selectPage(Long shopId, String shopName) {
        // 使用pageParam代表分页参数对象
        return selectPage(new PageParam(), new LambdaQueryWrapperX<TemuShopDO>()
                .eqIfPresent(TemuShopDO::getShopId, shopId)
                .likeIfPresent(TemuShopDO::getShopName, shopName)
                .orderByDesc(TemuShopDO::getId));
    }
} 