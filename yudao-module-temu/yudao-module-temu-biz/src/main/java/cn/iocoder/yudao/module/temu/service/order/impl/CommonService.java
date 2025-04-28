package cn.iocoder.yudao.module.temu.service.order.impl;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuProductCategoryDO;
import cn.iocoder.yudao.module.temu.dal.dataobject.TemuShopDO;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuProductCategoryMapper;
import cn.iocoder.yudao.module.temu.dal.mysql.TemuShopMapper;
import cn.iocoder.yudao.module.temu.mq.message.weixin.WeiXinNotifyMessage;
import cn.iocoder.yudao.module.temu.mq.producer.weixin.WeiXinProducer;
import cn.iocoder.yudao.module.temu.service.order.ICommonService;

import cn.iocoder.yudao.module.temu.utils.openapi.TemuOpenApiBuilder;
import cn.iocoder.yudao.module.temu.utils.openapi.TemuOpenApiUtil;
import cn.iocoder.yudao.module.temu.utils.weixin.WeiXinWebHookNotifyUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class CommonService implements ICommonService {
	
	@Resource
	private TemuProductCategoryMapper temuProductCategoryMapper;
	@Resource
	private TemuShopMapper temuShopMapper;
	@Resource
	private TemuOpenApiBuilder temuOpenApiBuilder;
	
	@Resource
	WeiXinProducer weiXinProducer;
	
	@Override
	public PageResult<TemuProductCategoryDO> list() {
		return temuProductCategoryMapper.selectPage();
	}
	
	@Override
	public PageResult<TemuShopDO> listShop() {
		return temuShopMapper.selectPage();
	}
	
	@Override
	public PageResult<TemuShopDO> listShop(Long loginUserId) {
		return temuShopMapper.selectPage(loginUserId);
	}
	
	@Override
	public Object testTemuOpenApi() {
		ArrayList<String> strings = new ArrayList<>();
		strings.add("\uD83D\uDD14【订单状态】");
		strings.add("状态:已下单");
		strings.add("备注:要生成一个随机的 20 个字符的字符串要生成一个随机的 20 个字符的字符串要生成一个随机的 20 个字符的字符串");
		weiXinProducer.sendMessage("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=adb5e6ed-5b94-435e-8212-3a6636195bcc",
				String.join("\n", strings)
		);
		return null;
		//TemuShopDO temuShopDO = temuShopMapper.selectById(25);
		//TreeMap<String, Object> map = new TreeMap<>();
		//map.put("pageNo", 1);
		//map.put("pageSize", 10);
		//return temuOpenApiBuilder.builder(temuShopDO.getAccessToken()).getOrderInfo(map);
	}
	
	@Override
	public void doWeiXinNotifyMessage(WeiXinNotifyMessage message) {
		if (message != null) {
			WeiXinWebHookNotifyUtil instance = WeiXinWebHookNotifyUtil.getInstance(message.getNotifyUrl());
			if (message.getToUser() != null && message.getToUser().length > 0) {
				instance.sendTextMessage(message.getContent(), Arrays.asList(message.getToUser()));
			} else {
				instance.sendTextMessage(message.getContent());
			}
		}
	}
	
}
