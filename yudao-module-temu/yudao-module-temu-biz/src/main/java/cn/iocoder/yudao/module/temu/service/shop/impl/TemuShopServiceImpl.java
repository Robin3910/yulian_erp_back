package cn.iocoder.yudao.module.temu.service.shop.impl;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.shop.TemuShopUpdateReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopMapper;
import cn.iocoder.yudao.module.temu.service.shop.TemuShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
@Slf4j
public class TemuShopServiceImpl implements TemuShopService {
    
    @Resource
    private TemuShopMapper temuShopMapper;
    
    @Override
    public Long createShop(TemuShopCreateReqVO createReqVO) {
        // 检查店铺ID是否已存在
        if (temuShopMapper.selectByShopId(createReqVO.getShopId()) != null) {
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
        // 校验存在
        if (temuShopMapper.selectById(updateReqVO.getId()) == null) {
            throw exception(new ErrorCode(1_002_001_001, "店铺不存在"));
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
        PageResult<TemuShopDO> pageResult = temuShopMapper.selectPage(
                pageReqVO.getShopId(), pageReqVO.getShopName());
        return BeanUtils.toBean(pageResult, TemuShopRespVO.class);
    }
    
    @Override
    public TemuShopDO getShopByShopId(Long shopId) {
        return temuShopMapper.selectByShopId(shopId);
    }
} 