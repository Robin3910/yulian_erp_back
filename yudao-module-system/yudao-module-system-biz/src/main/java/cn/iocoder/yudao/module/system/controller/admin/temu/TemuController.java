package cn.iocoder.yudao.module.system.controller.admin.temu;

import cn.hutool.core.lang.Assert;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.infra.api.websocket.WebSocketSenderApi;
import cn.iocoder.yudao.module.system.controller.admin.notice.vo.NoticePageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.notice.vo.NoticeRespVO;
import cn.iocoder.yudao.module.system.controller.admin.notice.vo.NoticeSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.notice.NoticeDO;
import cn.iocoder.yudao.module.system.service.notice.NoticeService;
import cn.iocoder.yudao.module.system.service.temu.TemuService;
import cn.iocoder.yudao.module.system.controller.admin.temu.vo.TemuSaveReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Slf4j
@Tag(name = "Temu管理页面")
@RestController
@RequestMapping("/system/temu")
@Validated
public class TemuController {

    @Resource
    private TemuService temuService;

    @PostMapping("/save")
    @Operation(summary = "保存Temu数据")
    @PreAuthorize("@ss.hasPermission('system:temu:save')")
    public CommonResult<Boolean> saveTemuData(@Valid @RequestBody TemuSaveReqVO saveReqVO) {
        log.info("[saveTemuData][收到Temu数据，店铺ID({})，订单数量({})]", 
                saveReqVO.getShopId(), saveReqVO.getOrders().size());
        temuService.saveTemuData(saveReqVO);
        return success(true);
    }

}
