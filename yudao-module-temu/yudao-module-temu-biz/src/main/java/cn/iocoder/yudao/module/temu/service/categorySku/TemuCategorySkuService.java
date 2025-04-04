package cn.iocoder.yudao.module.temu.service.categorySku;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.TemuCategorySkuCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.TemuCategorySkuPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.TemuCategorySkuRespVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.TemuCategorySkuUpdateReqVO;

import java.util.Collection;
import java.util.List;

/**
 * 商品品类SKU关系 Service 接口
 */
public interface TemuCategorySkuService {

    /**
     * 创建商品品类SKU关系
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCategorySku(TemuCategorySkuCreateReqVO createReqVO);

    /**
     * 更新商品品类SKU关系
     *
     * @param updateReqVO 更新信息
     */
    void updateCategorySku(TemuCategorySkuUpdateReqVO updateReqVO);

    /**
     * 删除商品品类SKU关系
     *
     * @param id 编号
     */
    void deleteCategorySku(Long id);

    /**
     * 批量删除商品品类SKU关系
     *
     * @param ids 编号集合
     */
    void deleteCategorySkus(Collection<Long> ids);

    /**
     * 获得商品品类SKU关系
     *
     * @param id 编号
     * @return 商品品类SKU关系
     */
    TemuCategorySkuRespVO getCategorySku(Long id);

    /**
     * 获得商品品类SKU关系分页
     *
     * @param pageReqVO 分页查询
     * @return 商品品类SKU关系分页
     */
    PageResult<TemuCategorySkuRespVO> getCategorySkuPage(TemuCategorySkuPageReqVO pageReqVO);

    /**
     * 根据店铺ID和SKU获得商品品类SKU关系
     *
     * @param shopId 店铺ID
     * @param sku SKU
     * @return 商品品类SKU关系
     */
    TemuCategorySkuRespVO getCategorySkuByShopIdAndSku(Long shopId, String sku);
} 