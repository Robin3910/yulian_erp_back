package cn.iocoder.yudao.module.temu.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;

import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;

import cn.iocoder.yudao.module.temu.dal.dataobject.TemuUserShopDO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

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
	 * @param shopId   店铺ID
	 * @param shopName 店铺名称
	 * @return 分页结果
	 */
	default PageResult<TemuShopDO> selectPage(Long shopId, String shopName, LocalDateTime[] createTime) {
		// 使用pageParam代表分页参数对象
		return selectPage(new PageParam(), new LambdaQueryWrapperX<TemuShopDO>()
				.eqIfPresent(TemuShopDO::getShopId, shopId)
				.likeIfPresent(TemuShopDO::getShopName, shopName)
				.betweenIfPresent(TemuShopDO::getCreateTime, createTime)
				.orderByDesc(TemuShopDO::getId));
	}
	
	/**
	 * 分页查询TemuShopDO数据
	 *
	 * 该方法用于查询TemuShopDO的分页数据。默认情况下，分页参数设置为不分页（即查询所有数据）。
	 * 通过LambdaQueryWrapperX构建查询条件，并调用selectPage方法进行分页查询。
	 *
	 * @return PageResult<TemuShopDO> 返回分页查询结果，包含查询到的TemuShopDO数据列表及分页信息
	 */
	default PageResult<TemuShopDO> selectPage() {
	    // 初始化分页参数对象，并设置不分页（pageSize为-1）
	    PageParam pageParam = new PageParam();
	    pageParam.setPageSize(-1);
	    // 使用LambdaQueryWrapperX构建查询条件
	    LambdaQueryWrapperX<TemuShopDO> temuShopDOLambdaQueryWrapperX = new LambdaQueryWrapperX<>();
	
	    // 调用selectPage方法进行分页查询
	    return selectPage(pageParam, temuShopDOLambdaQueryWrapperX);
	}
	default PageResult<TemuShopDO> selectPage(long userId) {
		PageParam pageParam = new PageParam();
		pageParam.setPageSize(-1);
		MPJLambdaWrapper<TemuShopDO> temuShopDOMPJLambdaWrapper = new MPJLambdaWrapper<>();
		temuShopDOMPJLambdaWrapper.selectAll(TemuShopDO.class)
				.innerJoin(TemuUserShopDO.class, TemuUserShopDO::getShopId, TemuShopDO::getShopId)
				.eq(TemuUserShopDO::getUserId, userId);
		return selectJoinPage(pageParam, TemuShopDO.class,temuShopDOMPJLambdaWrapper);
	}

    /**
     * 根据店铺ID列表查询店铺信息
     *
     * @param shopIds 店铺ID列表
     * @return 店铺信息列表
     */
    default List<TemuShopDO> selectByShopIds(Collection<Long> shopIds) {
        return selectList(new LambdaQueryWrapperX<TemuShopDO>()
                .in(TemuShopDO::getShopId, shopIds));
    }
	
} 