package com.xms.dao.service;

import java.util.List;
import com.xms.dao.service.XmsDataService;
import com.xms.dao.domain.StakeOrder;

/**
 * 质押订单Service接口
 *
 * @author xms
 * @date 2026-02-27
 */
public interface IStakeOrderService extends XmsDataService<StakeOrder>
{

    /**
     * 查询质押订单列表
     *
     * @param stakeOrder 质押订单
     * @return 质押订单集合
     */
    public List<StakeOrder> selectStakeOrderList(StakeOrder stakeOrder);

}
