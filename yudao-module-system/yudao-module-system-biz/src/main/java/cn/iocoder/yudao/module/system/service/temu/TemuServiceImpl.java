package cn.iocoder.yudao.module.system.service.temu;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.temu.vo.TemuSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.temu.TemuDO;
import cn.iocoder.yudao.module.system.dal.mysql.temu.TemuMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

@Service
@Validated
public class TemuServiceImpl implements TemuService {

    @Resource
    private TemuMapper temuMapper;

    @Override
    public void saveTemuData(TemuSaveReqVO saveReqVO) {
        // 转换请求VO为DO对象
        TemuDO temuDO = BeanUtils.toBean(saveReqVO, TemuDO.class);
        
        // 判断是否存在
        if (temuDO.getId() != null) {
            TemuDO dbTemu = temuMapper.selectById(temuDO.getId());
            if (dbTemu != null) {
                // 更新已存在的数据
                temuMapper.updateById(temuDO);
                return;
            }
        }
        
        // 插入新数据
        temuMapper.insert(temuDO);
    }
} 