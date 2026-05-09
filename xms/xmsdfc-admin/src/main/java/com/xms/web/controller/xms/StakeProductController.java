package com.xms.web.controller.xms;

import com.xms.common.annotation.Log;
import com.xms.common.annotation.RepeatSubmit;
import com.xms.common.constant.ConstantType;
import com.xms.common.core.controller.BaseController;
import com.xms.common.core.domain.AjaxResult;
import com.xms.common.core.page.TableDataInfo;
import com.xms.common.enums.BusinessType;
import com.xms.common.utils.poi.ExcelUtil;
import com.xms.dao.domain.StakeProduct;
import com.xms.dao.service.IStakeProductService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/xms/stakeProduct")
public class StakeProductController extends BaseController {
    @Autowired
    private IStakeProductService stakeProductService;

    @PreAuthorize("@ss.hasPermi('xms:stakeProduct:list')")
    @GetMapping("/list")
    public TableDataInfo list(StakeProduct stakeProduct) {
        startPage();
        List<StakeProduct> list = stakeProductService.selectStakeProductList(stakeProduct);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('xms:stakeProduct:export')")
    @Log(title = "Stake Product", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StakeProduct stakeProduct) {
        List<StakeProduct> list = stakeProductService.selectStakeProductList(stakeProduct);
        ExcelUtil<StakeProduct> util = new ExcelUtil<>(StakeProduct.class);
        util.exportExcel(response, list, "Stake Product Data");
    }

    @PreAuthorize("@ss.hasPermi('xms:stakeProduct:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(stakeProductService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('xms:stakeProduct:add')")
    @Log(title = "Stake Product", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit
    public AjaxResult add(@RequestBody StakeProduct stakeProduct) {
        validateStakeProduct(stakeProduct);
        return toAjax(stakeProductService.save(stakeProduct));
    }

    @PreAuthorize("@ss.hasPermi('xms:stakeProduct:edit')")
    @Log(title = "Stake Product", businessType = BusinessType.UPDATE)
    @PutMapping
    @RepeatSubmit
    public AjaxResult edit(@RequestBody StakeProduct stakeProduct) {
        if (stakeProduct.getId() == null) {
            throw new SecurityException("stake product id must not be empty");
        }
        StakeProduct dbProduct = stakeProductService.getById(stakeProduct.getId());
        if (dbProduct == null) {
            throw new SecurityException("stake product not found");
        }
        // 质押币种和产出币种只由初始化数据确定，后台编辑时不允许通过请求体变更，避免产出进错钱包字段。
        stakeProduct.setCoinType(dbProduct.getCoinType());
        stakeProduct.setRewardCoinType(dbProduct.getRewardCoinType());
        // OORT产品可通过数据库维护释放配置，但后台页面不开放编辑；页面保存其他字段时保留数据库原值。
        if (!Integer.valueOf(ConstantType.user_money_coin_type.type_2).equals(dbProduct.getCoinType())) {
            stakeProduct.setImmediateRatio(dbProduct.getImmediateRatio());
            stakeProduct.setLinearRatio(dbProduct.getLinearRatio());
            stakeProduct.setLinearDays(dbProduct.getLinearDays());
        }
        validateStakeProduct(stakeProduct);
        return toAjax(stakeProductService.updateById(stakeProduct));
    }

    @PreAuthorize("@ss.hasPermi('xms:stakeProduct:remove')")
    @Log(title = "Stake Product", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(stakeProductService.removeByIds(Arrays.asList(ids)));
    }

    private void validateStakeProduct(StakeProduct stakeProduct) {
        if (stakeProduct.getCoinType() == null) {
            stakeProduct.setCoinType(ConstantType.user_money_coin_type.type_3);
        }
        boolean dfcProduct = Integer.valueOf(ConstantType.user_money_coin_type.type_2).equals(stakeProduct.getCoinType());
        if (stakeProduct.getRewardCoinType() == null) {
            stakeProduct.setRewardCoinType(dfcProduct ? ConstantType.user_money_coin_type.type_5 : ConstantType.user_money_coin_type.type_3);
        }
        if (!dfcProduct) {
            if (stakeProduct.getImmediateRatio() == null) {
                stakeProduct.setImmediateRatio(new BigDecimal("25"));
            }
            if (stakeProduct.getLinearRatio() == null) {
                stakeProduct.setLinearRatio(new BigDecimal("75"));
            }
        }
        if (stakeProduct.getLinearDays() == null || stakeProduct.getLinearDays() <= 0) {
            stakeProduct.setLinearDays(dfcProduct ? 365 : 270);
        }
        if (stakeProduct.getExtraStakeValueUsdt() == null) {
            stakeProduct.setExtraStakeValueUsdt(BigDecimal.ZERO);
        }
        if (stakeProduct.getStakeUnitAmount() == null || stakeProduct.getStakeUnitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new SecurityException("stake amount must be greater than 0");
        }
        if (stakeProduct.getDayReward() == null || stakeProduct.getDayReward().compareTo(BigDecimal.ZERO) <= 0) {
            throw new SecurityException("day reward must be greater than 0");
        }
        if (stakeProduct.getValidDays() == null || stakeProduct.getValidDays() <= 0) {
            throw new SecurityException("valid days must be greater than 0");
        }
        if (dfcProduct) {
            if (stakeProduct.getAvailableStock() == null || stakeProduct.getAvailableStock() < 0) {
                throw new SecurityException("DFC stake stock must not be empty or negative");
            }
            if (stakeProduct.getImmediateRatio() == null || stakeProduct.getImmediateRatio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new SecurityException("DFC immediate release ratio must be greater than 0");
            }
            if (stakeProduct.getLinearRatio() == null || stakeProduct.getLinearRatio().compareTo(BigDecimal.ZERO) <= 0) {
                throw new SecurityException("DFC linear release ratio must be greater than 0");
            }
            if (stakeProduct.getImmediateRatio().add(stakeProduct.getLinearRatio()).compareTo(new BigDecimal("100")) > 0) {
                throw new SecurityException("DFC release ratio sum must not exceed 100");
            }
        } else if (stakeProduct.getExtraStakeValueUsdt().compareTo(BigDecimal.ZERO) <= 0) {
            throw new SecurityException("OORT extra stake value must be greater than 0");
        }
    }
}
