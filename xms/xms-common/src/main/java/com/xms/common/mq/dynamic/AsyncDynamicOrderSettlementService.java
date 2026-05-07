package com.xms.common.mq.dynamic;

import java.util.List;

/**
 * 发放团队收益任务
 */
public interface AsyncDynamicOrderSettlementService {
	/**
	 * 购买矿机的订单处理
	 */
	public void sendMessage(List<OrderMsgDO> orderMsgDOList);


	/**
	 * 跨链分发之后的订单处理
	 * @param performanceUpdateVO
	 */
	public void sendMessage(UserPerformanceUpdateVO performanceUpdateVO);
}
