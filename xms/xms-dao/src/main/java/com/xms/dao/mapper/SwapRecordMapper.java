package com.xms.dao.mapper;

import java.util.List;
import com.xms.dao.mapper.XmsMapper;

import com.xms.dao.domain.SwapRecord;

/**
 * 闪兑记录Mapper接口
 *
 * @author xms
 * @date 2026-03-16
 */
public interface SwapRecordMapper extends XmsMapper<SwapRecord>
{
    /**
     * 查询闪兑记录列表
     *
     * @param swapRecord 闪兑记录
     * @return 闪兑记录集合
     */
    public List<SwapRecord> selectSwapRecordList(SwapRecord swapRecord);

}
