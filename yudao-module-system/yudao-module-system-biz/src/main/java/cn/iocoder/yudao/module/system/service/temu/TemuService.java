package cn.iocoder.yudao.module.system.service.temu;

import cn.iocoder.yudao.module.system.controller.admin.temu.vo.TemuSaveReqVO;

public interface TemuService {

    /**
     * 保存Temu数据
     * 如果数据已存在则更新，不存在则新增
     *
     * @param saveReqVO 保存的数据
     */
    void saveTemuData(TemuSaveReqVO saveReqVO);
} 