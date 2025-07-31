package cn.iocoder.yudao.module.temu.service.bigCustomer;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuVipProductCategoryDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuVipProductCategoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.CATEGORY_NOT_EXISTS;

//大客户的商品类
@Slf4j
@Service
@Validated
public class TemuVipCategoryServiceImpl implements  TemuVipCategoryService{
    @Resource
    private TemuVipProductCategoryMapper temuVipProductCategoryMapper;

    @Override
    public TemuCategoryRespVO getCategory(Long id) {
        TemuVipProductCategoryDO category = temuVipProductCategoryMapper.selectById(id);
        log.warn("getCategory:{}", category);
        if (category == null) {
            throw exception(CATEGORY_NOT_EXISTS);
        }
        return BeanUtils.toBean(category, TemuCategoryRespVO.class);
    }

    @Override
    public PageResult<TemuCategoryRespVO> getCategoryPage(TemuCategoryPageReqVO pageReqVO) {
        PageResult<TemuVipProductCategoryDO> pageResult = temuVipProductCategoryMapper.selectPageByParams(pageReqVO);
        return BeanUtils.toBean(pageResult, TemuCategoryRespVO.class);
    }

    @Override
    @Transactional
    public Long createCategory(TemuCategoryCreateReqVO createReqVO) {
        // 插入
        TemuVipProductCategoryDO category = BeanUtils.toBean(createReqVO, TemuVipProductCategoryDO.class);
        temuVipProductCategoryMapper.insert(category);
        category.setCategoryId(category.getId());
        temuVipProductCategoryMapper.updateById(category);
        // 返回
        return category.getId();
    }

    @Override
    public void updateProductCategory(TemuCategoryCreateReqVO updateReqVO) {
        // 校验存在
        validateProductCategoryExists(updateReqVO.getId());
        // 更新
        TemuVipProductCategoryDO updateObj = BeanUtils.toBean(updateReqVO, TemuVipProductCategoryDO.class);
        temuVipProductCategoryMapper.updateById(updateObj);
    }

    @Override
    public void deleteProductCategory(Long id) {
        // 校验存在
        validateProductCategoryExists(id);
        // 删除
        temuVipProductCategoryMapper.deleteById(id);
    }

    private void validateProductCategoryExists(Long id) {
        if (temuVipProductCategoryMapper.selectById(id) == null) {
            throw exception(CATEGORY_NOT_EXISTS);
        }
    }
}
