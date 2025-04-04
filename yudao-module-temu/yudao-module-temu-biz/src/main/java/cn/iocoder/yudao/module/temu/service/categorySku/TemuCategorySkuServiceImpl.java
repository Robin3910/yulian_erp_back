package cn.iocoder.yudao.module.temu.service.categorySku;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.temu.controller.admin.vo.TemuCategorySkuCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.TemuCategorySkuPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.TemuCategorySkuRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.TemuCategorySkuUpdateReqVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategorySkuDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuProductCategorySkuMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.temu.enums.ErrorCodeConstants.CATEGORY_SKU_NOT_EXISTS;

/**
 * 商品品类SKU关系 Service 实现类
 */
@Service
@Validated
public class TemuCategorySkuServiceImpl implements TemuCategorySkuService {

    @Resource
    private TemuProductCategorySkuMapper categorySkuMapper;

    @Override
    public Long createCategorySku(TemuCategorySkuCreateReqVO createReqVO) {
        // 校验是否已经存在
        if (categorySkuMapper.selectByCategoryIdAndSku(createReqVO.getCategoryId(), createReqVO.getSku()) != null) {
            throw exception(new ErrorCode(1_001_001_000, "商品品类SKU关系已存在"));
        }

        // 插入
        TemuProductCategorySkuDO categorySku = BeanUtils.toBean(createReqVO, TemuProductCategorySkuDO.class);
        categorySkuMapper.insert(categorySku);
        // 返回
        return categorySku.getId();
    }

    @Override
    public void updateCategorySku(TemuCategorySkuUpdateReqVO updateReqVO) {
        // 校验存在
        validateCategorySkuExists(updateReqVO.getId());
        
        // 校验是否已经存在相同的记录
        TemuProductCategorySkuDO existingSku = categorySkuMapper.selectByCategoryIdAndSku(
                updateReqVO.getCategoryId(), updateReqVO.getSku());
        if (existingSku != null && !existingSku.getId().equals(updateReqVO.getId())) {
            throw exception(new ErrorCode(1_001_001_001, "商品品类SKU关系已存在"));
        }

        // 更新
        TemuProductCategorySkuDO updateObj = BeanUtils.toBean(updateReqVO, TemuProductCategorySkuDO.class);
        categorySkuMapper.updateById(updateObj);
    }

    @Override
    public void deleteCategorySku(Long id) {
        // 校验存在
        validateCategorySkuExists(id);
        // 删除
        categorySkuMapper.deleteById(id);
    }

    @Override
    public void deleteCategorySkus(Collection<Long> ids) {
        // 批量删除
        categorySkuMapper.deleteBatchIds(ids);
    }

    private void validateCategorySkuExists(Long id) {
        if (categorySkuMapper.selectById(id) == null) {
            throw exception(CATEGORY_SKU_NOT_EXISTS);
        }
    }

    @Override
    public TemuCategorySkuRespVO getCategorySku(Long id) {
        TemuProductCategorySkuDO categorySku = categorySkuMapper.selectById(id);
        return BeanUtils.toBean(categorySku, TemuCategorySkuRespVO.class);
    }

    @Override
    public PageResult<TemuCategorySkuRespVO> getCategorySkuPage(TemuCategorySkuPageReqVO pageReqVO) {
        PageResult<TemuProductCategorySkuDO> pageResult = categorySkuMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, TemuCategorySkuRespVO.class);
    }

    @Override
    public TemuCategorySkuRespVO getCategorySkuByShopIdAndSku(Long shopId, String sku) {
        TemuProductCategorySkuDO categorySku = categorySkuMapper.selectByShopIdAndSku(shopId, sku);
        return BeanUtils.toBean(categorySku, TemuCategorySkuRespVO.class);
    }
} 