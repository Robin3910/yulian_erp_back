package cn.iocoder.yudao.module.temu.service.batchCategoryMp;



import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.batchCategoryMp.TemuOrderBatchCategoryPageReqVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.batchCategoryMp.TemuOrderBatchCategoryRespVO;
import java.util.List;

public interface TemuOrderBatchCategoryService {
    /**
     * 批量逻辑删除
     */
    boolean deleteBatch(List<Long> ids);

    /**
     * 通用多条件分页查询
     */
    PageResult<TemuOrderBatchCategoryRespVO> page(TemuOrderBatchCategoryPageReqVO reqVO);

    /**
     * 根据 categoryId 修改 batchCategoryId
     */
    boolean updateBatchCategoryId(String batchCategoryId, Long categoryId);


}
