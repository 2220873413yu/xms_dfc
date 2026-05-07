package com.xms.dao.service.impl;

import java.util.List;
import com.xms.dao.service.impl.XmsDataServiceImpl;
import org.springframework.stereotype.Service;
import com.xms.dao.mapper.SwapRecordMapper;
import com.xms.dao.domain.SwapRecord;
import com.xms.dao.service.ISwapRecordService;

/**
 * 闪兑记录Service业务层处理
 *
 * @author xms
 * @date 2026-03-16
 */
@Service
public class SwapRecordServiceImpl extends XmsDataServiceImpl<SwapRecordMapper, SwapRecord> implements ISwapRecordService
{


    /**
     * 查询闪兑记录列表
     *
     *
     * @param swapRecord 闪兑记录
     * @return 闪兑记录
     */
    @Override
    public List<SwapRecord> selectSwapRecordList(SwapRecord swapRecord)
    {
        return baseMapper.selectSwapRecordList(swapRecord);
    }

}
