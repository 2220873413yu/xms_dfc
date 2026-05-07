package com.xms.dao.mapper;

import java.util.List;
import com.xms.dao.mapper.XmsMapper;

import com.xms.dao.domain.SwapConfig;

/**
 * 闪兑配置Mapper接口
 *
 * @author xms
 * @date 2026-03-16
 */
public interface SwapConfigMapper extends XmsMapper<SwapConfig>
{
    /**
     * 查询闪兑配置列表
     *
     * @param swapConfig 闪兑配置
     * @return 闪兑配置集合
     */
    public List<SwapConfig> selectSwapConfigList(SwapConfig swapConfig);

}
