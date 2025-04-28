package cn.iocoder.yudao.module.temu.mq.message.weixin;

import lombok.Data;

@Data
public class WeiXinNotifyMessage {
	private String content;
	private String[] toUser;
	private String notifyUrl;
}
