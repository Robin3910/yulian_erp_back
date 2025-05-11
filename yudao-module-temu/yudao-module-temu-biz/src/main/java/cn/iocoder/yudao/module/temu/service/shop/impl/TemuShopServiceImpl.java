package cn.iocoder.yudao.module.temu.service.shop.impl;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopUpdateReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuUserShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuUserShopMapper;
import cn.iocoder.yudao.module.temu.service.shop.TemuShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;

@Service
@Slf4j
public class TemuShopServiceImpl implements TemuShopService {
	
	@Resource
	private TemuShopMapper temuShopMapper;
	@Resource
	private TemuUserShopMapper temuUserShopMapper;
	
	@Override
	public Long createShop(TemuShopCreateReqVO createReqVO) {
		// 检查店铺ID是否已存在
		TemuShopDO temuShopDO = temuShopMapper.selectByShopId(createReqVO.getShopId());
		if (temuShopDO != null) {
			throw exception(new ErrorCode(1_002_001_000, "店铺ID已存在"));
		}
		// 插入
		TemuShopDO shop = BeanUtils.toBean(createReqVO, TemuShopDO.class);
		temuShopMapper.insert(shop);
		
		// 返回
		return shop.getId();
	}
	
	@Override
	public void updateShop(TemuShopUpdateReqVO updateReqVO) {
		TemuShopDO temuShopDO = temuShopMapper.selectByShopId(updateReqVO.getShopId());
		
		// 校验存在
		if (temuShopDO != null && !Objects.equals(temuShopDO.getId(), updateReqVO.getId()) && !temuShopDO.getDeleted()) {
			throw exception(new ErrorCode(1_002_001_001, "店铺ID重复"));
		}
		// 更新
		TemuShopDO updateObj = BeanUtils.toBean(updateReqVO, TemuShopDO.class);
		temuShopMapper.updateById(updateObj);
	}
	
	@Override
	public void deleteShop(Long id) {
		// 校验存在
		if (temuShopMapper.selectById(id) == null) {
			throw exception(new ErrorCode(1_002_001_001, "店铺不存在"));
		}
		
		// 删除
		temuShopMapper.deleteById(id);
	}
	
	@Override
	public TemuShopRespVO getShop(Long id) {
		TemuShopDO shop = temuShopMapper.selectById(id);
		return BeanUtils.toBean(shop, TemuShopRespVO.class);
	}
	
	@Override
	public PageResult<TemuShopRespVO> getShopPage(TemuShopPageReqVO pageReqVO) {
		PageResult<TemuShopDO> pageResult = temuShopMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<TemuShopDO>()
				.eqIfPresent(TemuShopDO::getShopId, pageReqVO.getShopId())
				.likeIfPresent(TemuShopDO::getShopName, pageReqVO.getShopName())
				.betweenIfPresent(TemuShopDO::getCreateTime, pageReqVO.getCreateTime())
				.orderByDesc(TemuShopDO::getId));
		
		PageResult<TemuShopRespVO> result = BeanUtils.toBean(pageResult, TemuShopRespVO.class);
		result.setPageNo(pageReqVO.getPageNo());
		result.setPageSize(pageReqVO.getPageSize());
		return result;
	}
	
	@Override
	public PageResult<TemuShopRespVO> getShopPageForUser(TemuShopPageReqVO pageReqVO) {
		//根据用户查询关联的店铺ID
		Long userId = pageReqVO.getUserId();
		List<TemuUserShopDO> temuUserShopDOS = temuUserShopMapper.selectList(new LambdaQueryWrapperX<TemuUserShopDO>()
				.eq(TemuUserShopDO::getUserId, userId));
		if (temuUserShopDOS.isEmpty()) {
			return PageResult.empty();
		}
		List<Long> shopIds = temuUserShopDOS.stream()
				.map(TemuUserShopDO::getShopId)
				.collect(Collectors.toList());
		PageResult<TemuShopDO> pageResult = temuShopMapper.selectPage(pageReqVO, new LambdaQueryWrapperX<TemuShopDO>()
				.eqIfPresent(TemuShopDO::getShopId, pageReqVO.getShopId())
				.likeIfPresent(TemuShopDO::getShopName, pageReqVO.getShopName())
				.betweenIfPresent(TemuShopDO::getCreateTime, pageReqVO.getCreateTime())
				.in(TemuShopDO::getShopId, shopIds)
				.orderByDesc(TemuShopDO::getId));
		
		PageResult<TemuShopRespVO> result = BeanUtils.toBean(pageResult, TemuShopRespVO.class);
		result.setPageNo(pageReqVO.getPageNo());
		result.setPageSize(pageReqVO.getPageSize());
		return result;
	}
	
	
	@Override
	public TemuShopDO getShopByShopId(Long shopId) {
		return temuShopMapper.selectByShopId(shopId);
	}
} 