package com.xms.dao.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xms.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import java.math.BigDecimal;
import com.xms.common.annotation.Excel;
import com.baomidou.mybatisplus.annotation.TableField;

/**
 * 闪兑配置对象 t_swap_config
 *
 * @author xms
 * @date 2026-03-16
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_swap_config")
public class SwapConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键id */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 源币种类型 5:产出DFC,6:代理分红收益,7:运营收益 */
    @Excel(name = "源币种类型 5:产出DFC,6:代理分红收益,7:运营收益")
    @ApiModelProperty(value = "源币种类型 5:产出DFC,6:代理分红收益,7:运营收益")
    private Integer sourceCoinType;
    /** 源币种编码 */
    @Excel(name = "源币种编码")
    @ApiModelProperty(value = "源币种编码")
    private String sourceCoinCode;
    /** 目标币种类型 1:USDT */
    @Excel(name = "目标币种类型 1:USDT")
    @ApiModelProperty(value = "目标币种类型 1:USDT")
    private Integer targetCoinType;
    /** 目标币种编码 */
    @Excel(name = "目标币种编码")
    @ApiModelProperty(value = "目标币种编码")
    private String targetCoinCode;
    /** 闪兑是否打开 0:否,1:是 */
    @Excel(name = "闪兑是否打开 0:否,1:是")
    @ApiModelProperty(value = "闪兑是否打开 0:否,1:是")
    private Integer swapOpen;
    /** 闪兑价格(1个源币种可兑换多少USDT) */
    @Excel(name = "闪兑价格(1个源币种可兑换多少USDT)")
    @ApiModelProperty(value = "闪兑价格(1个源币种可兑换多少USDT)")
    private BigDecimal swapPrice;
    /** 手续费率(例如:5表示5%) */
    @Excel(name = "手续费率(例如:5表示5%)")
    @ApiModelProperty(value = "手续费率(例如:5表示5%)")
    private BigDecimal feeRatio;
    /** 最小闪兑数量 */
    @Excel(name = "最小闪兑数量")
    @ApiModelProperty(value = "最小闪兑数量")
    private BigDecimal minSwapAmount;
    /** 单笔最大闪兑数量,0表示不限制 */
    @Excel(name = "单笔最大闪兑数量,0表示不限制")
    @ApiModelProperty(value = "单笔最大闪兑数量,0表示不限制")
    private BigDecimal maxSwapAmount;
    /** 单日闪兑额度,0表示不限制 */
    @Excel(name = "单日闪兑额度,0表示不限制")
    @ApiModelProperty(value = "单日闪兑额度,0表示不限制")
    private BigDecimal dailySwapLimit;
    /** 排序值 */
    @Excel(name = "排序值")
    @ApiModelProperty(value = "排序值")
    private Long sortOrder;

	@TableField(exist = false)
	private String createBy;
	/** 更新者 */
	@TableField(exist = false)
	private String updateBy;
	@TableField(exist = false)
	private Integer deleted;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("sourceCoinType", getSourceCoinType())
            .append("sourceCoinCode", getSourceCoinCode())
            .append("targetCoinType", getTargetCoinType())
            .append("targetCoinCode", getTargetCoinCode())
            .append("swapOpen", getSwapOpen())
            .append("swapPrice", getSwapPrice())
            .append("feeRatio", getFeeRatio())
            .append("minSwapAmount", getMinSwapAmount())
            .append("maxSwapAmount", getMaxSwapAmount())
            .append("dailySwapLimit", getDailySwapLimit())
            .append("sortOrder", getSortOrder())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
        .toString();
    }
}
