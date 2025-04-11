package cn.iocoder.yudao.module.temu.service.category;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryCreateReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.category.TemuCategoryRespVO;

import javax.validation.Valid;

/**
 * 商品品类 Service 接口
 */
public interface TemuCategoryService {

    /**
     * 获得商品品类
     *
     * @param id 编号
     * @return 商品品类
     */
    TemuCategoryRespVO getCategory(Long id);

    /**
     * 获得商品品类分页
     *
     * @param pageReqVO 分页查询
     * @return 商品品类分页
     */
    PageResult<TemuCategoryRespVO> getCategoryPage(TemuCategoryPageReqVO pageReqVO);

    /**
     * 创建商品品类
     *
     * @param createReqVO 创建信息
     * @return 商品品类编号
     */
    Long createCategory(TemuCategoryCreateReqVO createReqVO);
    
    /**
     * 更新商品品类
     *
     * @param updateReqVO 更新信息
     */
    void updateProductCategory(@Valid TemuCategoryCreateReqVO updateReqVO);
    
    /**
     * 删除商品品类
     *
     * @param id 编号
     */
    void deleteProductCategory(Long id);
} 