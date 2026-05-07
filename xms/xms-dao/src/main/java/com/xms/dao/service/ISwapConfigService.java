package com.xms.dao.service;

import java.util.List;
import com.xms.dao.service.XmsDataService;
import com.xms.dao.domain.SwapConfig;

/**
 * 闪兑配置Service接口
 *
 * @author xms
 * @date 2026-03-16
 */
public interface ISwapConfigService extends XmsDataService<SwapConfig>
{

    /**
     * 查询闪兑配置列表
     *
     * @param swapConfig 闪兑配置
     * @return 闪兑配置集合
     */
    public List<SwapConfig> selectSwapConfigList(SwapConfig swapConfig);

	/**
	 * 闪对修改
	 * @param swapConfig
	 * @return
	 */
	int updateSwapConfigById(SwapConfig swapConfig);
}
