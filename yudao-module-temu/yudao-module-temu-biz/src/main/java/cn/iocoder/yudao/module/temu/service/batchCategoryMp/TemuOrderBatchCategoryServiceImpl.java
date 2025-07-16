package cn.iocoder.yudao.module.temu.service.batchCategoryMp;

import cn.iocoder.yudao.module.temu.controller.admin.vo.batchCategoryMp.TemuOrderBatchCategoryRespVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderBatchCategoryMpDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuOrderBatchCategoryMpMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Temu订单批次类目 业务实现类
 * 实现批量删除、按条件查询、批量更新等功能，基于 MyBatis-Plus
 */
@Service
public class TemuOrderBatchCategoryServiceImpl extends ServiceImpl<TemuOrderBatchCategoryMpMapper, TemuOrderBatchCategoryMpDO> implements TemuOrderBatchCategoryService {
    /**
     * 批量逻辑删除（根据主键ID集合）
     * @param ids 主键ID集合
     * @return 是否删除成功
     */
    @Override
    public boolean deleteBatch(List<Long> ids) {
            return this.removeByIds(ids);
    }

    /**
     * 根据 batchCategoryId 查询批次类目列表
     * @param batchCategoryId 批次所属类目id，可为空
     * @return 查询结果VO列表
     */
    @Override
    public List<TemuOrderBatchCategoryRespVO> listByBatchCategoryId(String batchCategoryId) {
        LambdaQueryWrapper<TemuOrderBatchCategoryMpDO> wrapper = new LambdaQueryWrapper<>();
        if (batchCategoryId != null && !batchCategoryId.isEmpty()) {
            wrapper.eq(TemuOrderBatchCategoryMpDO::getBatchCategoryId, batchCategoryId);
        }
        List<TemuOrderBatchCategoryMpDO> list = this.list(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 根据 categoryId 查询批次类目列表
     * @param categoryId 商品品类ID，可为空
     * @return 查询结果VO列表
     */
    @Override
    public List<TemuOrderBatchCategoryRespVO> listByCategoryId(Long categoryId) {
        LambdaQueryWrapper<TemuOrderBatchCategoryMpDO> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(TemuOrderBatchCategoryMpDO::getCategoryId, categoryId);
        }
        List<TemuOrderBatchCategoryMpDO> list = this.list(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 根据 categoryId 修改 batchCategoryId
     * @param batchCategoryId 新的批次所属类目id
     * @param categoryId 商品品类ID
     * @return 是否更新成功
     */
    @Override
    public boolean updateBatchCategoryId(String batchCategoryId, Long categoryId) {
        LambdaUpdateWrapper<TemuOrderBatchCategoryMpDO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TemuOrderBatchCategoryMpDO::getCategoryId, categoryId)
                .set(TemuOrderBatchCategoryMpDO::getBatchCategoryId, batchCategoryId);
        return this.update(wrapper);
    }

    /**
     * DO对象转VO对象
     * @param doObj 数据库实体对象
     * @return 返回VO对象
     */
    private TemuOrderBatchCategoryRespVO convertToVO(TemuOrderBatchCategoryMpDO doObj) {
        TemuOrderBatchCategoryRespVO vo = new TemuOrderBatchCategoryRespVO();
        BeanUtils.copyProperties(doObj, vo);
        return vo;
    }
}
