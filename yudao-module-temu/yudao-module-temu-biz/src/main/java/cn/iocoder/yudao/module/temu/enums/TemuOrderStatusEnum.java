package cn.iocoder.yudao.module.temu.enums;

public interface TemuOrderStatusEnum {
	//0待下单 -> 1已下单待送产 -> 2已送产待生产 -> 3已生产待发货 -> 4已发货--->已作废
	int UNDELIVERED = 0;
	int ORDERED = 1;
	int IN_PRODUCTION = 2;
	int SHIPPED = 3;
	int COMPLETED = 4;
	int CANCELLED = 5;
}
