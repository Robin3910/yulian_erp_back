package cn.iocoder.yudao.module.temu.controller.admin.controller;

import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.temu.controller.admin.vo.client.TemuOrderBeatchUpdateOrderStatusVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.client.TemuOrderRequestVO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.client.TemuOrderSaveRequestVO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuOrderDO;
import cn.iocoder.yudao.module.temu.service.order.ITemuOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Temu管理 - 订单管理")
@RestController
@RequestMapping("/temu/order")
@Validated
public class AdminTemuOrderController {
	@Resource private ITemuOrderService temuOrderService;
	
	@GetMapping("/page")
	@Operation(summary = "获取订单管理信息")
	@PermitAll
	public CommonResult<?> list(TemuOrderRequestVO temuOrderRequestVO) {
		return CommonResult.success(temuOrderService.list(temuOrderRequestVO));
	}
	//批量修改订单状态
	@PostMapping("/beatch_update_status")
	@Operation(summary = "批量修改订单状态")
	public CommonResult<Boolean> beatchUpdateStatus(@RequestBody List<TemuOrderDO> requestVO) {
		return success(temuOrderService.beatchUpdateStatus(requestVO));
	}
	
	@PostMapping("/save")
	@Operation(summary = "保存订单数据")
	@PermitAll
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
}