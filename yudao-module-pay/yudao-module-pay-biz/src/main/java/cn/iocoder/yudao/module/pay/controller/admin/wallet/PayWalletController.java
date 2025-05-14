package cn.iocoder.yudao.module.pay.controller.admin.wallet;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.pay.core.enums.channel.PayChannelEnum;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.pay.controller.admin.order.vo.PayOrderRespVO;
import cn.iocoder.yudao.module.pay.controller.admin.order.vo.PayOrderSubmitRespVO;
import cn.iocoder.yudao.module.pay.controller.admin.wallet.vo.wallet.PayWalletPageReqVO;
import cn.iocoder.yudao.module.pay.controller.admin.wallet.vo.wallet.PayWalletRespVO;
import cn.iocoder.yudao.module.pay.controller.admin.wallet.vo.wallet.PayWalletUpdateBalanceReqVO;
import cn.iocoder.yudao.module.pay.controller.admin.wallet.vo.wallet.PayWalletUserReqVO;
import cn.iocoder.yudao.module.pay.controller.app.order.vo.AppPayOrderSubmitReqVO;
import cn.iocoder.yudao.module.pay.controller.app.order.vo.AppPayOrderSubmitRespVO;
import cn.iocoder.yudao.module.pay.controller.app.wallet.vo.recharge.AppPayWalletRechargeCreateReqVO;
import cn.iocoder.yudao.module.pay.controller.app.wallet.vo.recharge.AppPayWalletRechargeCreateRespVO;
import cn.iocoder.yudao.module.pay.controller.app.wallet.vo.wallet.AppPayWalletRespVO;
import cn.iocoder.yudao.module.pay.convert.order.PayOrderConvert;
import cn.iocoder.yudao.module.pay.convert.wallet.PayWalletConvert;
import cn.iocoder.yudao.module.pay.convert.wallet.PayWalletRechargeConvert;
import cn.iocoder.yudao.module.pay.dal.dataobject.channel.PayChannelDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.order.PayOrderDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.wallet.PayWalletDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.wallet.PayWalletRechargeDO;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.pay.enums.wallet.PayWalletBizTypeEnum;
import cn.iocoder.yudao.module.pay.framework.pay.core.WalletPayClient;
import cn.iocoder.yudao.module.pay.service.channel.PayChannelService;
import cn.iocoder.yudao.module.pay.service.order.PayOrderService;
import cn.iocoder.yudao.module.pay.service.wallet.PayWalletRechargeService;
import cn.iocoder.yudao.module.pay.service.wallet.PayWalletService;
import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.enums.UserTypeEnum.MEMBER;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserType;
import static cn.iocoder.yudao.module.pay.enums.ErrorCodeConstants.WALLET_NOT_FOUND;

@Tag(name = "管理后台 - 用户钱包")
@RestController
@RequestMapping("/pay/wallet")
@Validated
@Slf4j
public class PayWalletController {
	
	@Resource
	private PayWalletService payWalletService;
    @Resource
    private PayWalletRechargeService walletRechargeService;
    
    @Resource
    private PayChannelService channelService;
    @Resource
    private PayOrderService payOrderService;
	@GetMapping("/get")
	@PreAuthorize("@ss.hasPermission('pay:wallet:query')")
	@Operation(summary = "获得用户钱包明细")
	public CommonResult<PayWalletRespVO> getWallet(PayWalletUserReqVO reqVO) {
		PayWalletDO wallet = payWalletService.getOrCreateWallet(reqVO.getUserId(), MEMBER.getValue());
		return success(PayWalletConvert.INSTANCE.convert02(wallet));
	}
	
	@GetMapping("/page")
	@Operation(summary = "获得会员钱包分页")
	@PreAuthorize("@ss.hasPermission('pay:wallet:query')")
	public CommonResult<PageResult<PayWalletRespVO>> getWalletPage(@Valid PayWalletPageReqVO pageVO) {
		PageResult<PayWalletDO> pageResult = payWalletService.getWalletPage(pageVO);
		return success(PayWalletConvert.INSTANCE.convertPage(pageResult));
	}
	
	@PutMapping("/update-balance")
	@Operation(summary = "更新会员用户余额")
	@PreAuthorize("@ss.hasPermission('pay:wallet:update-balance')")
	public CommonResult<Boolean> updateWalletBalance(@Valid @RequestBody PayWalletUpdateBalanceReqVO updateReqVO) {
		// 获得用户钱包
		PayWalletDO wallet = payWalletService.getOrCreateWallet(updateReqVO.getUserId(), MEMBER.getValue());
		if (wallet == null) {
			log.error("[updateWalletBalance]，updateReqVO({}) 用户钱包不存在.", updateReqVO);
			throw exception(WALLET_NOT_FOUND);
		}
		
		// 更新钱包余额
		payWalletService.addWalletBalance(wallet.getId(), String.valueOf(updateReqVO.getUserId()),
				PayWalletBizTypeEnum.UPDATE_BALANCE, updateReqVO.getBalance());
		return success(true);
	}
    
    @GetMapping("/get-enable-code-list")
    @Operation(summary = "获得指定应用的开启的支付渠道编码列表")
    @Parameter(name = "appId", description = "应用编号", required = true, example = "1")
    public CommonResult<Set<String>> getEnableChannelCodeList(@RequestParam("appId") Long appId) {
        List<PayChannelDO> channels = channelService.getEnableChannelList(appId);
        return success(convertSet(channels, PayChannelDO::getCode));
    }
	
	@PostMapping("/create-order")
    @Operation(summary = "创建钱包充值记录（发起充值）")
    public CommonResult<AppPayWalletRechargeCreateRespVO> createWalletRecharge(
            @Valid @RequestBody AppPayWalletRechargeCreateReqVO reqVO) {
        PayWalletRechargeDO walletRecharge = walletRechargeService.createWalletRecharge(
                getLoginUserId(), getLoginUserType(), getClientIP(), reqVO);
        return success(PayWalletRechargeConvert.INSTANCE.convert(walletRecharge));
    }
	
	@PostMapping("/submit-pay-order")
	@Operation(summary = "支付钱包充值订单")
    public CommonResult<AppPayOrderSubmitRespVO> submitPayOrder(@RequestBody AppPayOrderSubmitReqVO reqVO) {
        // 1. 钱包支付事，需要额外传 user_id 和 user_type
        if (Objects.equals(reqVO.getChannelCode(), PayChannelEnum.WALLET.getCode())) {
            Map<String, String> channelExtras = reqVO.getChannelExtras() == null ?
                    Maps.newHashMapWithExpectedSize(2) : reqVO.getChannelExtras();
            channelExtras.put(WalletPayClient.USER_ID_KEY, String.valueOf(getLoginUserId()));
            channelExtras.put(WalletPayClient.USER_TYPE_KEY, String.valueOf(getLoginUserType()));
            reqVO.setChannelExtras(channelExtras);
        }
        
        // 2. 提交支付
        PayOrderSubmitRespVO respVO = payOrderService.submitOrder(reqVO, getClientIP());
        return success(PayOrderConvert.INSTANCE.convert3(respVO));
    }
	
	@GetMapping("/get-pay-order-info")
	@Operation(summary = "获得支付订单信息")
	@Parameters({
			@Parameter(name = "id", description = "编号", required = true, example = "1024"),
			@Parameter(name = "sync", description = "是否同步", example = "true")
	})
	public CommonResult<PayOrderRespVO> getOrder(@RequestParam("id") Long id,
	                                             @RequestParam(value = "sync", required = false) Boolean sync) {
		PayOrderDO order = payOrderService.getOrder(id);
		// sync 仅在等待支付
		if (Boolean.TRUE.equals(sync) && PayOrderStatusEnum.isWaiting(order.getStatus())) {
			payOrderService.syncOrderQuietly(order.getId());
			// 重新查询，因为同步后，可能会有变化
			order = payOrderService.getOrder(id);
		}
		return success(BeanUtils.toBean(order, PayOrderRespVO.class));
	}
	@GetMapping("/get-my-wallet")
	@Operation(summary = "获取钱包")
	public CommonResult<AppPayWalletRespVO> getPayWallet() {
		PayWalletDO wallet = payWalletService.getOrCreateWallet(SecurityFrameworkUtils.getLoginUserId(), UserTypeEnum.ADMIN.getValue());
		return success(PayWalletConvert.INSTANCE.convert(wallet));
	}
}
