package com.xms.dao.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xms.common.annotation.Excel;
import com.xms.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;
import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_stake_order")
public class StakeOrder extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @Excel(name = "用户ID", sort = 1)
    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @Excel(name = "质押产品ID", sort = 2)
    @ApiModelProperty(value = "质押产品ID")
    private Long productId;

    @Excel(name = "质押币种", sort = 3, dictType = "biz_stake_coin_type")
    @ApiModelProperty(value = "质押币种 2:DFC 3:OORT")
    private Integer coinType;

    @Excel(name = "产出币种", sort = 4, dictType = "biz_stake_reward_coin_type")
    @ApiModelProperty(value = "产出币种 3:OORT 5:产出DFC")
    private Integer rewardCoinType;

    @Excel(name = "订单号", sort = 5, width = 30)
    @ApiModelProperty(value = "质押订单号")
    private String orderNo;

    @Excel(name = "购买份数", sort = 6)
    @ApiModelProperty(value = "购买份数")
    private Integer quantity;

    @Excel(name = "质押本金数量", sort = 7)
    @ApiModelProperty(value = "实际质押本金数量")
    private BigDecimal stakeAmount;

    @Excel(name = "固定质押OORT数量", sort = 8)
    @ApiModelProperty(value = "固定质押OORT数量=stake_unit_amount*quantity")
    private BigDecimal stakeOortAmount;

    @Excel(name = "额外质押USDT价值", sort = 9)
    @ApiModelProperty(value = "额外质押USDT价值=extra_stake_value_usdt*quantity")
    private BigDecimal extraValueUsdt;

    @Excel(name = "OORT价格", sort = 10)
    @ApiModelProperty(value = "下单时OORT价格(USDT)")
    private BigDecimal oortPriceUsdt;

    @Excel(name = "DFC价格", sort = 11)
    @ApiModelProperty(value = "下单时DFC价格(USDT)")
    private BigDecimal dfcPriceUsdt;

    @Excel(name = "额外质押折算OORT数量", sort = 12)
    @ApiModelProperty(value = "额外质押折算OORT数量=extra_value_usdt/oort_price_usdt")
    private BigDecimal extraStakeOortAmount;

    @Excel(name = "每日产出", sort = 13)
    @ApiModelProperty(value = "每日产出=product.day_reward*quantity")
    private BigDecimal dayReward;

    @Excel(name = "立即释放比例", sort = 14)
    @ApiModelProperty(value = "立即释放比例快照，百分比")
    private BigDecimal immediateRatio;

    @Excel(name = "线性释放比例", sort = 15)
    @ApiModelProperty(value = "线性释放比例快照，百分比")
    private BigDecimal linearRatio;

    @Excel(name = "线性释放天数", sort = 16)
    @ApiModelProperty(value = "线性释放天数")
    private Integer linearDays;

    @Excel(name = "理论总产出", sort = 17)
    @ApiModelProperty(value = "理论总产出=day_reward*valid_days")
    private BigDecimal totalYieldTarget;

    @Excel(name = "已产出", sort = 18)
    @ApiModelProperty(value = "已产出")
    private BigDecimal yieldedAmount;

    @Excel(name = "订单状态", sort = 19, dictType = "t_stake_order_status")
    @ApiModelProperty(value = "状态 0:产出中 1:已到期 2:暂停 3:取消")
    private Integer status;

    @Excel(name = "本金退还状态", sort = 20, dictType = "biz_principal_refund_status")
    @ApiModelProperty(value = "本金退还状态 0:未退还 1:已退还")
    private Integer principalRefundStatus;

    @Excel(name = "本金退还时间", sort = 21, width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "本金退还时间")
    private Date principalRefundTime;

    @Excel(name = "有效期", sort = 22)
    @ApiModelProperty(value = "有效期天数")
    private Integer validDays;

    @Excel(name = "剩余有效期", sort = 23)
    @ApiModelProperty(value = "剩余有效期天数")
    private Integer haveDays;

    @TableField(exist = false)
    private String createBy;

    @TableField(exist = false)
    private String updateBy;

    @TableField(exist = false)
    private Integer deleted;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("productId", getProductId())
            .append("coinType", getCoinType())
            .append("rewardCoinType", getRewardCoinType())
            .append("orderNo", getOrderNo())
            .append("quantity", getQuantity())
            .append("stakeAmount", getStakeAmount())
            .append("stakeOortAmount", getStakeOortAmount())
            .append("extraValueUsdt", getExtraValueUsdt())
            .append("oortPriceUsdt", getOortPriceUsdt())
            .append("dfcPriceUsdt", getDfcPriceUsdt())
            .append("extraStakeOortAmount", getExtraStakeOortAmount())
            .append("dayReward", getDayReward())
            .append("immediateRatio", getImmediateRatio())
            .append("linearRatio", getLinearRatio())
            .append("linearDays", getLinearDays())
            .append("totalYieldTarget", getTotalYieldTarget())
            .append("yieldedAmount", getYieldedAmount())
            .append("status", getStatus())
            .append("validDays", getValidDays())
            .append("haveDays", getHaveDays())
            .append("principalRefundStatus", getPrincipalRefundStatus())
            .append("principalRefundTime", getPrincipalRefundTime())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
