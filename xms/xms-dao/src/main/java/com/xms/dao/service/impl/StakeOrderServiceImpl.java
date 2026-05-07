package com.xms.dao.service.impl;

import java.util.List;
import com.xms.dao.service.impl.XmsDataServiceImpl;
import org.springframework.stereotype.Service;
import com.xms.dao.mapper.StakeOrderMapper;
import com.xms.dao.domain.StakeOrder;
import com.xms.dao.service.IStakeOrderService;

/**
 * 质押订单Service业务层处理
 *
 * @author xms
 * @date 2026-02-27
 */
@Service
public class StakeOrderServiceImpl extends XmsDataServiceImpl<StakeOrderMapper, StakeOrder> implements IStakeOrderService
{


    /**
     * 查询质押订单列表
     *
     *
     * @param stakeOrder 质押订单
     * @return 质押订单
     */
    @Override
    public List<StakeOrder> selectStakeOrderList(StakeOrder stakeOrder)
    {
        return baseMapper.selectStakeOrderList(stakeOrder);
    }

}
