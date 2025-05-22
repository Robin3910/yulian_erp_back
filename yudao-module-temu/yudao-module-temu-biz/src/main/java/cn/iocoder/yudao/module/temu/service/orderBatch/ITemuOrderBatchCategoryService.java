package cn.iocoder.yudao.module.temu.service.orderBatch;

import java.util.List;
import java.util.Map;

/**
 * Temu订单批次分类 Service 接口
 */
public interface ITemuOrderBatchCategoryService {

    /**
     * 根据categoryOrderMap查询对应的batchCategoryId，并重新组织数据结构
     *
     * @param categoryOrderMap 分类ID和订单ID列表的映射
     * @return batchCategoryId和订单ID列表的映射
     */
    Map<String, List<Long>> getBatchCategoryOrderMap(Map<String, List<Long>> categoryOrderMap);
}
