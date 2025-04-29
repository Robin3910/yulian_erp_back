package cn.iocoder.yudao.module.temu.utils.openapi;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.module.temu.api.openapi.dto.OrderInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
}
