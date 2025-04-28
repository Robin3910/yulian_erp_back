package cn.iocoder.yudao.module.temu.utils.weixin;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Data
public class WeiXinWebHookNotifyUtil {
	private final String url;
	
	private WeiXinWebHookNotifyUtil(String url) {
		this.url = url;
	}
	
	public static WeiXinWebHookNotifyUtil getInstance(String url) {
		return new WeiXinWebHookNotifyUtil(url);
	}
	
	public void sendMessage(HashMap<String, Object> hashMap) {
		try {
			HttpResponse response = HttpRequest.post(url).body(JSONUtil.toJsonStr(hashMap).getBytes(StandardCharsets.UTF_8)).execute();
			response.close();
		} catch (Exception e) {
			log.error("【企业微信通知】发送微信通知失败\n{},原始参数{}", e.getMessage(), JSONUtil.toJsonStr(hashMap));
		}
		
	}
	
	public void sendTextMessage(String text) {
		HashMap<String, Object> hashMap = new HashMap<>();
		HashMap<String, Object> contentMap = new HashMap<>();
		//消息类型
		hashMap.put("msgtype", "text");
		//组装消息体
		contentMap.put("content", text);
		contentMap.put("mentioned_mobile_list", new String[]{"@all"});
		hashMap.put("text", contentMap);
		sendMessage(hashMap);
	}
	
	public void sendTextMessage(String text, List<String> toUser) {
		HashMap<String, Object> hashMap = new HashMap<>();
		HashMap<String, Object> contentMap = new HashMap<>();
		//消息类型
		hashMap.put("msgtype", "text");
		//组装消息体
		contentMap.put("content", text);
		contentMap.put("mentioned_mobile_list", toUser);
		hashMap.put("text", contentMap);
		sendMessage(hashMap);
	}
}
