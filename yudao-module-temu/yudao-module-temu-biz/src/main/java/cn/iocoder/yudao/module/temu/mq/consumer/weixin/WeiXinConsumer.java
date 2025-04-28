package cn.iocoder.yudao.module.temu.mq.consumer.weixin;

import cn.iocoder.yudao.module.temu.mq.message.weixin.WeiXinNotifyMessage;
import cn.iocoder.yudao.module.temu.service.order.impl.CommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class WeiXinConsumer {
	@Resource
	CommonService commonService;
	
	@EventListener
	@Async
	public void doMessage(WeiXinNotifyMessage message) {
		log.info("[企业微信通知][消息内容({})]", message);
		commonService.doWeiXinNotifyMessage(message);
	}
}
