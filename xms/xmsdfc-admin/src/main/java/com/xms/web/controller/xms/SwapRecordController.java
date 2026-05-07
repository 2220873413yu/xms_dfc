package com.xms.web.controller.xms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.xms.common.annotation.RepeatSubmit;
import com.xms.dao.domain.UserAddress;
import com.xms.dao.entity.domain.UserInfo;
import com.xms.dao.entity.domain.Withdrawal;
import com.xms.dao.service.UserInfoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xms.common.annotation.Log;
import com.xms.common.core.controller.BaseController;
import com.xms.common.core.domain.AjaxResult;
import com.xms.common.enums.BusinessType;
import com.xms.dao.domain.SwapRecord;
import com.xms.dao.service.ISwapRecordService;
import com.xms.common.utils.poi.ExcelUtil;
import com.xms.common.core.page.TableDataInfo;

/**
 * 闪兑记录Controller
 *
 * @author xms
 * @date 2026-03-16
 */
@RestController
@RequestMapping("/xms/swapRecord")
public class SwapRecordController extends BaseController
{
    @Autowired
    private ISwapRecordService swapRecordService;


	@Autowired
	private UserInfoService userInfoService;

/**
 * 查询闪兑记录列表
 */
@PreAuthorize("@ss.hasPermi('xms:swapRecord:list')")
@GetMapping("/list")
    public TableDataInfo list(SwapRecord swapRecord)
    {
		if(StrUtil.isNotBlank(swapRecord.getUserAccount())){
			UserInfo userInfo = userInfoService.lambdaQuery()
				.eq(UserInfo::getAccount, swapRecord.getUserAccount())
				.one();
			if(userInfo != null){
				swapRecord.setUserId(userInfo.getUserId());
			}else{
				return getDataTable(new ArrayList<>());
			}
		}
        startPage();
        List<SwapRecord> list = swapRecordService.selectSwapRecordList(swapRecord);
		if(CollectionUtil.isNotEmpty(list)){
			Map<Long, String> userAccountMap = userInfoService.getUserAccountById(list.stream().map(SwapRecord::getUserId).collect(Collectors.toList()));
			for (SwapRecord bankVo : list) {
				bankVo.setUserAccount(userAccountMap.get(bankVo.getUserId()));
			}
		}
        return getDataTable(list);
    }

    /**
     * 导出闪兑记录列表
     */
    @PreAuthorize("@ss.hasPermi('xms:swapRecord:export')")
    @Log(title = "闪兑记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SwapRecord swapRecord)
    {
		if(StrUtil.isNotBlank(swapRecord.getUserAccount())){
			UserInfo userInfo = userInfoService.lambdaQuery()
				.eq(UserInfo::getAccount, swapRecord.getUserAccount())
				.one();
			if(userInfo != null){
				swapRecord.setUserId(userInfo.getUserId());
			}else{
				ExcelUtil<Withdrawal> util = new ExcelUtil<Withdrawal>(Withdrawal.class);
				util.exportExcel(response, new ArrayList<>(), "闪兑记录数据");
			}
		}
        List<SwapRecord> list = swapRecordService.selectSwapRecordList(swapRecord);

		if(CollectionUtil.isNotEmpty(list)){
			Map<Long, String> userAccountMap = userInfoService.getUserAccountById(list.stream().map(SwapRecord::getUserId).collect(Collectors.toList()));
			for (SwapRecord bankVo : list) {
				bankVo.setUserAccount(userAccountMap.get(bankVo.getUserId()));
			}
		}
        ExcelUtil<SwapRecord> util = new ExcelUtil<SwapRecord>(SwapRecord.class);
        util.exportExcel(response, list, "闪兑记录数据");
    }

    /**
     * 获取闪兑记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('xms:swapRecord:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(swapRecordService.getById(id));
    }

    /**
     * 新增闪兑记录
     */
    @PreAuthorize("@ss.hasPermi('xms:swapRecord:add')")
    @Log(title = "闪兑记录", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit
    public AjaxResult add(@RequestBody SwapRecord swapRecord) {
        return toAjax(swapRecordService.save(swapRecord));
    }

    /**
     * 修改闪兑记录
     */
    @PreAuthorize("@ss.hasPermi('xms:swapRecord:edit')")
    @Log(title = "闪兑记录", businessType = BusinessType.UPDATE)
    @PutMapping
    @RepeatSubmit
    public AjaxResult edit(@RequestBody SwapRecord swapRecord) {
        return toAjax(swapRecordService.updateById(swapRecord));
    }

    /**
     * 删除闪兑记录
     */
    @PreAuthorize("@ss.hasPermi('xms:swapRecord:remove')")
    @Log(title = "闪兑记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(swapRecordService.removeByIds(Arrays.asList(ids)));
    }
}
