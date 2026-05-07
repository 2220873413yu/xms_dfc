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
 * 闪兑记录对象 t_swap_record
 *
 * @author xms
 * @date 2026-03-16
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_swap_record")
public class SwapRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键id */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 闪兑单号 */
    @Excel(name = "闪兑单号")
    @ApiModelProperty(value = "闪兑单号")
    private String orderNo;
    /** 用户id */
    @Excel(name = "用户id")
    @ApiModelProperty(value = "用户id")
    private Long userId;
    /** 源币种类型 5:产出DFC,6:代理分红收益,7:运营收益 */
    @Excel(name = "源币种类型 5:产出DFC,6:代理分红收益,7:运营收益")
    @ApiModelProperty(value = "源币种类型 5:产出DFC,6:代理分红收益,7:运营收益")
    private Integer sourceCoinType;
    /** 目标币种类型 1:USDT */
    @Excel(name = "目标币种类型 1:USDT")
    @ApiModelProperty(value = "目标币种类型 1:USDT")
    private Integer targetCoinType;
    /** 闪兑源数量 */
    @Excel(name = "闪兑源数量")
    @ApiModelProperty(value = "闪兑源数量")
    private BigDecimal sourceAmount;
    /** 闪兑价格快照 */
    @Excel(name = "闪兑价格快照")
    @ApiModelProperty(value = "闪兑价格快照")
    private BigDecimal swapPrice;
    /** 手续费率快照 */
    @Excel(name = "手续费率快照")
    @ApiModelProperty(value = "手续费率快照")
    private BigDecimal feeRatio;
    /** 手续费金额 */
    @Excel(name = "手续费金额")
    @ApiModelProperty(value = "手续费金额")
    private BigDecimal feeAmount;
    /** 实际到账USDT */
    @Excel(name = "实际到账USDT")
    @ApiModelProperty(value = "实际到账USDT")
    private BigDecimal targetNetAmount;

	/** 创建者 */
	@JsonIgnore
	@TableField(exist = false)
	private String createBy;

	/** 更新者 */
	@JsonIgnore
	@TableField(exist = false)
	private String updateBy;
	@JsonIgnore
	@TableField(exist = false)
	private Integer deleted;

	/**
	 * 钱包地址
	 */
	@TableField(exist = false)
	@Excel(name = "钱包地址",sort = 2,width = 40)
	private String userAccount;
    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("orderNo", getOrderNo())
            .append("userId", getUserId())
            .append("sourceCoinType", getSourceCoinType())
            .append("targetCoinType", getTargetCoinType())
            .append("sourceAmount", getSourceAmount())
            .append("swapPrice", getSwapPrice())
            .append("feeRatio", getFeeRatio())
            .append("feeAmount", getFeeAmount())
            .append("targetNetAmount", getTargetNetAmount())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
        .toString();
    }
}
