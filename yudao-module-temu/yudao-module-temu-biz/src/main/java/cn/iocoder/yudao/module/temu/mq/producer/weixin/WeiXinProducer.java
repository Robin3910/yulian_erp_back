package cn.iocoder.yudao.module.temu.mq.producer.weixin;

import cn.iocoder.yudao.module.temu.mq.message.weixin.WeiXinNotifyMessage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class WeiXinProducer {
	@Resource
	private ApplicationContext applicationContext;
	
	public void sendMessage(String notifyUrl,String message) {
		applicationContext.publishEvent(new WeiXinNotifyMessage().setContent(message).setNotifyUrl(notifyUrl));
	}
	
	public void sendMessage(String notifyUrl,String message, String[] toUser) {
		applicationContext.publishEvent(new WeiXinNotifyMessage().setContent(message).setToUser(toUser).setNotifyUrl(notifyUrl));
	}
}
