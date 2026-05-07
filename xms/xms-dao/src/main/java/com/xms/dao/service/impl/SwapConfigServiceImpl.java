package com.xms.dao.service.impl;

import java.math.BigDecimal;
import java.util.List;

import com.xms.common.exception.ServiceException;
import com.xms.dao.service.impl.XmsDataServiceImpl;
import org.springframework.stereotype.Service;
import com.xms.dao.mapper.SwapConfigMapper;
import com.xms.dao.domain.SwapConfig;
import com.xms.dao.service.ISwapConfigService;

/**
 * 闪兑配置Service业务层处理
 *
 * @author xms
 * @date 2026-03-16
 */
@Service
public class SwapConfigServiceImpl extends XmsDataServiceImpl<SwapConfigMapper, SwapConfig> implements ISwapConfigService
{


    /**
     * 查询闪兑配置列表
     *
     *
     * @param swapConfig 闪兑配置
     * @return 闪兑配置
     */
    @Override
    public List<SwapConfig> selectSwapConfigList(SwapConfig swapConfig)
    {
        return baseMapper.selectSwapConfigList(swapConfig);
    }

	@Override
	public int updateSwapConfigById(SwapConfig req) {
		if(req.getSwapPrice() == null ||
		req.getSwapPrice().compareTo(BigDecimal.ZERO)<=0){
			throw new ServiceException("闪兑价格不能小于等于0");
		}

		if(req.getFeeRatio() == null ||
			req.getFeeRatio().compareTo(BigDecimal.ZERO)<=0){
			throw new ServiceException("闪兑手续费不能小于等于0");
		}

		if(req.getMinSwapAmount() == null ||
			req.getMinSwapAmount().compareTo(BigDecimal.ZERO)<=0){
			throw new ServiceException("闪兑最小闪兑数量不能小于等于0");
		}
		updateById(req);
		return 1;
	}
}
