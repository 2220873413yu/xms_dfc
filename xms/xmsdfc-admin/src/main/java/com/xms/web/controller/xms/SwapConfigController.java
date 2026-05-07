package com.xms.web.controller.xms;

import java.util.Arrays;
import java.util.List;

import com.xms.common.annotation.RepeatSubmit;
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
import com.xms.dao.domain.SwapConfig;
import com.xms.dao.service.ISwapConfigService;
import com.xms.common.utils.poi.ExcelUtil;
import com.xms.common.core.page.TableDataInfo;

/**
 * 闪兑配置Controller
 *
 * @author xms
 * @date 2026-03-16
 */
@RestController
@RequestMapping("/xms/swapConfig")
public class SwapConfigController extends BaseController
{
    @Autowired
    private ISwapConfigService swapConfigService;

/**
 * 查询闪兑配置列表
 */
@PreAuthorize("@ss.hasPermi('xms:swapConfig:list')")
@GetMapping("/list")
    public TableDataInfo list(SwapConfig swapConfig)
    {
        startPage();
        List<SwapConfig> list = swapConfigService.selectSwapConfigList(swapConfig);
        return getDataTable(list);
    }

    /**
     * 导出闪兑配置列表
     */
    @PreAuthorize("@ss.hasPermi('xms:swapConfig:export')")
    @Log(title = "闪兑配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SwapConfig swapConfig)
    {
        List<SwapConfig> list = swapConfigService.selectSwapConfigList(swapConfig);
        ExcelUtil<SwapConfig> util = new ExcelUtil<SwapConfig>(SwapConfig.class);
        util.exportExcel(response, list, "闪兑配置数据");
    }

    /**
     * 获取闪兑配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('xms:swapConfig:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(swapConfigService.getById(id));
    }

    /**
     * 新增闪兑配置
     */
    @PreAuthorize("@ss.hasPermi('xms:swapConfig:add')")
    @Log(title = "闪兑配置", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit
    public AjaxResult add(@RequestBody SwapConfig swapConfig) {
        return toAjax(swapConfigService.save(swapConfig));
    }

    /**
     * 修改闪兑配置
     */
    @PreAuthorize("@ss.hasPermi('xms:swapConfig:edit')")
    @Log(title = "闪兑配置", businessType = BusinessType.UPDATE)
    @PutMapping
    @RepeatSubmit
    public AjaxResult edit(@RequestBody SwapConfig swapConfig) {
        return toAjax(swapConfigService.updateSwapConfigById(swapConfig));
    }

    /**
     * 删除闪兑配置
     */
    @PreAuthorize("@ss.hasPermi('xms:swapConfig:remove')")
    @Log(title = "闪兑配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(swapConfigService.removeByIds(Arrays.asList(ids)));
    }
}
