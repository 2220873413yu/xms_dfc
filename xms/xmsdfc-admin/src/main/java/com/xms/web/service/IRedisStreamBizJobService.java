package com.xms.web.service;

import java.util.List;

public interface IRedisStreamBizJobService {

	/**
	 * 矿机订单后的动态奖励结算处理失败
	 * @param list
	 * @return
	 */
	Integer handlerDynamicOrderSettlement(List list);
}
