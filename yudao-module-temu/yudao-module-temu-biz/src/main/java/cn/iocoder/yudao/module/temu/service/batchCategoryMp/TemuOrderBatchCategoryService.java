package cn.iocoder.yudao.module.temu.service.batchCategoryMp;



import cn.iocoder.yudao.module.temu.controller.admin.vo.batchCategoryMp.TemuOrderBatchCategoryRespVO;
import java.util.List;

public interface TemuOrderBatchCategoryService {
    /**
     * 批量逻辑删除
     */
    boolean deleteBatch(List<Long> ids);

    /**
     * 按 batchCategoryId 查询
     */
    List<TemuOrderBatchCategoryRespVO> listByBatchCategoryId(String batchCategoryId);

    /**
     * 按 categoryId 查询
     */
    List<TemuOrderBatchCategoryRespVO> listByCategoryId(Long categoryId);

    /**
     * 根据 categoryId 修改 batchCategoryId
     */
    boolean updateBatchCategoryId(String batchCategoryId, Long categoryId);
}
