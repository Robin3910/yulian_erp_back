package cn.iocoder.yudao.module.temu.utils.openapi;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.temu.api.openapi.dto.OrderInfoDTO;
import cn.iocoder.yudao.module.temu.controller.admin.vo.deliveryOrder.TemuDeliveryOrderQueryReqVO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemuOpenApiUtil {
	private String baseUrl;
	private String appKey;
	private String appSecret;
	private String accessToken;
	
	/**
	 * 判断给定对象是否为基本类型的包装类
	 *
	 * @param obj 需要判断的对象
	 * @return 如果对象是基本类型的包装类则返回true，否则返回false
	 */
	public static boolean isPrimitiveWrapper(Object obj) {
		// 检查对象是否为null，null不是基本类型或其包装类
		if (obj == null) {
			return false;
		}
		// 检查对象是否为基本类型包装类之一
		return obj instanceof Byte ||
				obj instanceof Short ||
				obj instanceof Integer ||
				obj instanceof Long ||
				obj instanceof Float ||
				obj instanceof Double ||
				obj instanceof Boolean ||
				obj instanceof Character;
	}
	
	//生成签名
	private String createSign(TreeMap<String, Object> params) {
		//用key按ASCII码升序排序，
		log.info("排序的结果是{}", params);
		StringBuilder stringBuilder = new StringBuilder();
		for (String key : params.keySet()) {
			stringBuilder.append(key);
			Object o = params.get(key);
			if (o != null) {
				if (isPrimitiveWrapper(o)) {
					stringBuilder.append(o);
				} else {
					stringBuilder.append(JSONUtil.toJsonStr(o));
				}
			}
			
		}
		stringBuilder.append(appSecret);
		log.info("签名拼接结果\n{}", appSecret + stringBuilder);
		//生成大写的MD5
		return DigestUtil.md5Hex(appSecret + stringBuilder).toUpperCase();
	}
	
	//获取公共参数公共请求参数
	private TreeMap<String, Object> createCommonRequest() {
		TreeMap<String, Object> map = new TreeMap<>();
		map.put("app_key", appKey);
		map.put("timestamp", System.currentTimeMillis() / 1000);
		map.put("access_token", accessToken);
		map.put("data_type", "json");
		return map;
	}
	
	//生成请求参数
	private TreeMap<String, Object> createRequest(TreeMap<String, Object> params) {
		// 合并请求参数
		TreeMap<String, Object> requestData = createCommonRequest();
		requestData.putAll(params);
		// 生成签名
		String sign = createSign(requestData);
		requestData.put("sign", sign);
		return requestData;
	}
	
	public String request(TreeMap<String, Object> params) {
		//创建请求参数
		TreeMap<String, Object> requestData = createRequest(params);
		log.info("参数拼接的结果\n{}", JSONUtil.toJsonStr(requestData));
		//发送请求
		String body = HttpRequest.post(baseUrl).body(JSONUtil.toJsonStr(requestData)).execute().body();
		log.info("请求的内容是\n:{}", body);
		return body;
	}

	public List<OrderInfoDTO.SubOrderForSupplier> getFullOrderList(TreeMap<String, Object> params) {
		ArrayList<OrderInfoDTO.SubOrderForSupplier> subOrderForSupplierList = new ArrayList<>();
		params.put("type", "bg.purchaseorderv2.get");
		params.put("isCustomGoods", true);
		Integer pageSize = 500;
		Integer pageNo = 1;
		Integer total;
		params.put("pageSize", pageSize);
		while (true) {
			params.put("pageNo", pageNo);
			//响应结果
			OrderInfoDTO orderInfoDTO = BeanUtil.toBean(JSONUtil.parseObj(request(params)), OrderInfoDTO.class);
			if (!orderInfoDTO.isSuccess()) {
				throw ServiceExceptionUtil.exception(new ErrorCode(orderInfoDTO.getErrorCode(), orderInfoDTO.getErrorMsg()));
			}
			if (orderInfoDTO.getResult().getSubOrderForSupplierList() == null || orderInfoDTO.getResult().getSubOrderForSupplierList().isEmpty()) {
				break;
			}
			//统计总数
			total = orderInfoDTO.getResult().getTotal();
			//获取当前页面的数据
			subOrderForSupplierList.addAll(orderInfoDTO.getResult().getSubOrderForSupplierList());
			//判断是否存在下一页
			if (pageNo * pageSize < total) {
				pageNo++;
				continue;
			}
			break;
		}
		
		return subOrderForSupplierList;
	}
	
	/**
	 * 获取发货订单列表
	 *
	 * @param params 请求参数
	 * @return 发货订单列表
	 */
	public String getShipOrderList(TreeMap<String, Object> params) {
		params.put("type", "bg.purchaseorderv2.get");
//		Integer pageSize = 2;
//		Integer pageNo = 1;
//		params.put("pageSize", pageSize);
//		params.put("pageNo", pageNo);
//		// 创建快递单号列表
//		List<String> expressDeliverySnList = new ArrayList<>();
//		expressDeliverySnList.add("SF3190241519922");
//		params.put("expressDeliverySnList", expressDeliverySnList);
		// 发送请求并返回结果
		return request(params);
	}
	//请求（失败并重试）
	public String requestWithRetry(TreeMap<String, Object> params, int maxRetry, long retryIntervalMillis) {
		TreeMap<String, Object> requestData = createRequest(params);
		String lastError = null;
		for (int i = 0; i < maxRetry; i++) {
			try {
				log.info("第{}次请求API，参数：{}", i + 1, JSONUtil.toJsonStr(requestData));
				String body = HttpRequest.post(baseUrl)
						.body(JSONUtil.toJsonStr(requestData))
						.timeout(10000) // 可设置超时时间
						.execute()
						.body();
				// 业务判断
				cn.hutool.json.JSONObject json = JSONUtil.parseObj(body);
				if (json.getBool("success", false)) {
					return body;
				} else {
					lastError = json.getStr("errorMsg");
					log.warn("第{}次API请求失败，错误信息：{}", i + 1, lastError);
				}
			} catch (Exception e) {
				lastError = e.getMessage();
				log.warn("第{}次API请求异常，异常信息：{}", i + 1, lastError, e);
			}
			if (i < maxRetry - 1) {
				log.info("将在{}毫秒后进行第{}次重试...", retryIntervalMillis, i + 2);
				try {
					Thread.sleep(retryIntervalMillis); // 重试间隔
				} catch (InterruptedException ignored) {}
			}
		}
		log.error("API请求失败，重试{}次仍未成功，最后错误：{}", maxRetry, lastError);
		throw new RuntimeException("API请求失败，重试" + maxRetry + "次仍未成功，最后错误：" + lastError);
	}

	/**
	 * 获取发货订单列表v2
	 *
	 * @param reqVO 查询参数VO
	 * @return 发货订单列表
	 */
	public String getShipOrderListv2(TemuDeliveryOrderQueryReqVO reqVO) {
		TreeMap<String, Object> params = new TreeMap<>();
		params.put("type", "bg.shiporderv2.get");
		// 优先用reqVO里的分页参数
		Integer pageSize = reqVO.getPageSize() != null ? reqVO.getPageSize() : 10;
		Integer pageNo = reqVO.getPageNo() != null ? reqVO.getPageNo() : 1;
		params.put("pageSize", pageSize);
		params.put("pageNo", pageNo);

		if (reqVO.getProductSkcIdList() != null) params.put("productSkcIdList", reqVO.getProductSkcIdList());
		if (reqVO.getIsCustomProduct() != null) params.put("isCustomProduct", reqVO.getIsCustomProduct());
		if (reqVO.getPageSize() != null) params.put("pageSize", reqVO.getPageSize());
		if (reqVO.getPageNo() != null) params.put("pageNo", reqVO.getPageNo());
		if (reqVO.getExpressDeliverySnList() != null) params.put("expressDeliverySnList", reqVO.getExpressDeliverySnList());
		if (reqVO.getExpressWeightFeedbackStatus() != null) params.put("expressWeightFeedbackStatus", reqVO.getExpressWeightFeedbackStatus());
		if (reqVO.getIsPrintBoxMark() != null) params.put("isPrintBoxMark", reqVO.getIsPrintBoxMark());
		if (reqVO.getTargetDeliveryAddress() != null) params.put("targetDeliveryAddress", reqVO.getTargetDeliveryAddress());
		if (reqVO.getOnlyTaxWarehouseWaitApply() != null) params.put("onlyTaxWarehouseWaitApply", reqVO.getOnlyTaxWarehouseWaitApply());
		if (reqVO.getSubWarehouseIdList() != null) params.put("subWarehouseIdList", reqVO.getSubWarehouseIdList());
		if (reqVO.getSubPurchaseOrderSnList() != null) params.put("subPurchaseOrderSnList", reqVO.getSubPurchaseOrderSnList());
		if (reqVO.getLatestFeedbackStatusList() != null) params.put("latestFeedbackStatusList", reqVO.getLatestFeedbackStatusList());
		if (reqVO.getUrgencyType() != null) params.put("urgencyType", reqVO.getUrgencyType());
		if (reqVO.getTargetReceiveAddress() != null) params.put("targetReceiveAddress", reqVO.getTargetReceiveAddress());
		if (reqVO.getDeliverTimeFrom() != null) params.put("deliverTimeFrom", reqVO.getDeliverTimeFrom());
		if (reqVO.getDeliverTimeTo() != null) params.put("deliverTimeTo", reqVO.getDeliverTimeTo());
		if (reqVO.getSkcExtCodeList() != null) params.put("skcExtCodeList", reqVO.getSkcExtCodeList());
		if (reqVO.getDeliveryOrderSnList() != null) params.put("deliveryOrderSnList", reqVO.getDeliveryOrderSnList());
		if (reqVO.getInventoryRegion() != null) params.put("inventoryRegion", reqVO.getInventoryRegion());
		if (reqVO.getIsVmi() != null) params.put("isVmi", reqVO.getIsVmi());
		if (reqVO.getSortType() != null) params.put("sortType", reqVO.getSortType());
		if (reqVO.getIsJit() != null) params.put("isJit", reqVO.getIsJit());
		if (reqVO.getSortFieldName() != null) params.put("sortFieldName", reqVO.getSortFieldName());
		if (reqVO.getStatus() != null) params.put("status", reqVO.getStatus());

		return requestWithRetry(params, 10, 1000);
	}

	/**
	 * 调用Temu开放平台API
	 *
	 * @param params API请求参数
	 * @return API响应结果
	 */
	public String callApi(Map<String, Object> params) {
		// 1. 添加公共参数
		params.put("app_key", this.getAppKey());
		params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
		params.put("access_token", this.getAccessToken());
		params.put("data_type", "JSON");

		// 2. 生成签名
		String sign = createSign(new TreeMap<>(params)); // Assuming createSign is the correct method for signing
		params.put("sign", sign);

		// 3. 发送HTTP请求
		String response = HttpRequest.post(this.getBaseUrl())
				.body(JSONUtil.toJsonStr(params)) // Changed to .body for JSON
				.execute()
				.body();

		// 4. 处理响应
		return response; // No specific error handling added as per original code
	}

}
