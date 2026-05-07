package com.xms.dao.service;

import java.util.List;
import com.xms.dao.service.XmsDataService;
import com.xms.dao.domain.SwapRecord;

/**
 * 闪兑记录Service接口
 *
 * @author xms
 * @date 2026-03-16
 */
public interface ISwapRecordService extends XmsDataService<SwapRecord>
{

    /**
     * 查询闪兑记录列表
     *
     * @param swapRecord 闪兑记录
     * @return 闪兑记录集合
     */
    public List<SwapRecord> selectSwapRecordList(SwapRecord swapRecord);

}
