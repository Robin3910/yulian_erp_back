package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.*;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.service.order.ITemuOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import cn.iocoder.yudao.module.temu.controller.admin.vo.order.TemuOrderBatchInsertReqVO;

@Tag(name = "Temu管理 - 订单管理")
@RestController
@RequestMapping("/temu/order")
@Validated
public class AdminTemuOrderController {
	@Resource
	private ITemuOrderService temuOrderService;
	
	@GetMapping("/page")
	@Operation(summary = "获取订单管理信息")
	public CommonResult<?> list(TemuOrderRequestVO temuOrderRequestVO) {
		Long userId = SecurityFrameworkUtils.getLoginUserId();
		return success(temuOrderService.list(temuOrderRequestVO, userId));
	}
	
	//统计订单总金额
	@GetMapping("/statistics")
	@Operation(summary = "统计订单总金额")
	public CommonResult<TemuOrderStatisticsRespVO> statistics(TemuOrderRequestVO temuOrderRequestVO) {
		Long userId = SecurityFrameworkUtils.getLoginUserId();
		return success(temuOrderService.statistics( temuOrderRequestVO, userId));
	}
	
	@GetMapping("/admin-page")
	@Operation(summary = "获取订单管理信息")
	public CommonResult<?> adminList(TemuOrderRequestVO temuOrderRequestVO) {
		return success(temuOrderService.list(temuOrderRequestVO));
	}
	//统计订单总金额
	@GetMapping("/admin-statistics")
	@Operation(summary = "统计订单总金额")
	public CommonResult<TemuOrderStatisticsRespVO> adminStatistics(TemuOrderRequestVO temuOrderRequestVO) {
		return success(temuOrderService.statistics( temuOrderRequestVO));
	}
	
	//批量修改订单状态
	@PostMapping("/beatch_update_status")
	@Operation(summary = "批量修改订单状态")
	public CommonResult<Boolean> beatchUpdateStatus(@RequestBody List<TemuOrderDO> requestVO) {
		return success(temuOrderService.beatchUpdateStatus(requestVO));
	}
	
	@PostMapping("/save")
	@Operation(summary = "保存订单数据")
	public CommonResult<Integer> saveOrders(@RequestBody TemuOrderSaveRequestVO requestVO) {
		System.out.println("接收到的订单请求数据: " + requestVO);
		// 记录请求内容到日志
		String requestJson = JSONUtil.toJsonStr(requestVO);
		
		// 获取订单列表
		List<Map<String, Object>> ordersList = requestVO.getOrders().getOrders();
		
		// 保存订单
		int savedCount = temuOrderService.saveOrders(
				requestVO.getShopId(),
				requestVO.getShopName(),
				ordersList,
				requestJson
		);
		
		return success(savedCount);
	}
	
	@PostMapping("/update-category")
	@Operation(summary = "获取订单管理信息")
	public CommonResult<?> updateCategory(@Valid @RequestBody TemuOrderUpdateCategoryReqVo requestVO) {
		
		return success(temuOrderService.updateCategory(requestVO));
	}
	
	//	代下单状态批量下单
	@PostMapping("/batch-save-order")
	@Operation(summary = "批量下单")
	public CommonResult<Integer> batchSave(@Valid @RequestBody List<TemuOrderBatchOrderReqVO> requestVO) {
		return success(temuOrderService.batchSaveOrder(requestVO));
	}
	
	//根据订单ID获取合规单信息和商品码
	@GetMapping("/get-order-extra-info/{orderId}")
	@Operation(summary = "根据订单ID获取合规单信息和商品码")
	public CommonResult<TemuOrderExtraInfoRespVO> getOrderExtraInfo(@PathVariable("orderId") String orderId) {
		return success(temuOrderService.getOrderExtraInfo(orderId));
	}
	//保存订单备注
	@PutMapping("/update-order-remark")
	@Operation(summary = "保存订单备注")
	public CommonResult<Boolean> saveOrderRemark(@Valid @RequestBody TemuOrderSaveOrderRemarkReqVO requestVO) {
		return success(temuOrderService.saveOrderRemark(requestVO));
	}
	
	/**
	 * 更新订单定制图片
	 * 
	 * @param reqVO 请求VO
	 * @return 更新结果
	 */
	@PutMapping("/update-custom-images")
	@Operation(summary = "更新订单定制图片")
	@PermitAll  //允许用户修改定制图片
	public CommonResult<Boolean> updateOrderCustomImages(@Valid @RequestBody TemuOrderUpdateCustomImagesReqVO reqVO) {
		return success(temuOrderService.updateOrderCustomImages(reqVO.getOrderId(), reqVO.getCustomImageUrls()));
	}
	/**
	 * 批量更新订单状态
	 * @param reqVOList 请求VO列表
	 * @return 更新结果
	 */
	@PostMapping("/update-order-status")
	@Operation(summary = "批量更新订单状态")
	public CommonResult<Boolean> updateOrderStatus(@Valid @RequestBody List<TemuOrderDO> reqVOList) {
		return success(temuOrderService.updateOrderStatus(reqVOList));
	}

    @GetMapping("/order-sku-page")
    @Operation(summary = "分页查询唯一(orderNo, sku)组合的订单")
    public CommonResult<OrderSkuPageRespVO> orderSkuPage(
            TemuOrderRequestVO temuOrderRequestVO,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return success(temuOrderService.orderSkuPage(temuOrderRequestVO, pageNo, pageSize));
    }

    @PostMapping("/toggle-is-found-all")
    @Operation(summary = "切换订单是否找齐状态")
    public CommonResult<Boolean> toggleIsFoundAll(@RequestParam("orderId") Long orderId,
                                                  @RequestParam(value = "isFoundAll", required = false) Integer isFoundAll) {
        return success(temuOrderService.toggleIsFoundAll(orderId, isFoundAll));
    }

    /**
     * 批量插入订单（根据sorting_sequence、发货人id、条件判断）
     */
    @PostMapping("/batch-insert")
    @Operation(summary = "批量插入订单（根据sorting_sequence、发货人id、条件判断）")
    public CommonResult<Boolean> batchInsertOrder(@RequestBody TemuOrderBatchInsertReqVO reqVO) {
        return success(temuOrderService.batchUpdateSenderIdBySortingSequence(
            reqVO.getSortingSequenceList(), reqVO.getSenderId(), reqVO.getConditionFlag()
        ));
    }
}