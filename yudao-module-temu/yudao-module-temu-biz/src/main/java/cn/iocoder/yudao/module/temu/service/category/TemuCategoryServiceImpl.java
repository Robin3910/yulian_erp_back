package cn.iocoder.yudao.module.temu.service.category;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuProductCategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.CATEGORY_NOT_EXISTS;

/**
 * 商品品类 Service 实现类
 */
@Service
@Validated
public class TemuCategoryServiceImpl implements TemuCategoryService {

    @Resource
    private TemuProductCategoryMapper temuCategoryMapper;

    @Override
    public TemuCategoryRespVO getCategory(Long id) {
        TemuProductCategoryDO category = temuCategoryMapper.selectById(id);
        if (category == null) {
            throw exception(CATEGORY_NOT_EXISTS);
        }
        return BeanUtils.toBean(category, TemuCategoryRespVO.class);
    }

    @Override
    public PageResult<TemuCategoryRespVO> getCategoryPage(TemuCategoryPageReqVO pageReqVO) {
        PageResult<TemuProductCategoryDO> pageResult = temuCategoryMapper.selectPageByParams(pageReqVO);
        return BeanUtils.toBean(pageResult, TemuCategoryRespVO.class);
    }

    @Override
    public Long createCategory(TemuCategoryCreateReqVO createReqVO) {
        // 校验商品品类ID是否已存在
        if (temuCategoryMapper.selectByCategoryId(createReqVO.getCategoryId()) != null) {
            throw exception(new ErrorCode(1_001_000_001, "商品品类ID已存在"));
        }
        
        // 插入
        TemuProductCategoryDO category = BeanUtils.toBean(createReqVO, TemuProductCategoryDO.class);
        temuCategoryMapper.insert(category);
        // 返回
        return category.getId();
    }
} 