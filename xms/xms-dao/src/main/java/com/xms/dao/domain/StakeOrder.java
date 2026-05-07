package com.xms.dao.domain;

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
 * 质押订单对象 t_stake_order
 *
 * @author xms
 * @date 2026-02-27
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_stake_order")
public class StakeOrder extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键id */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户id */
    @Excel(name = "用户ID",sort = 1)
    @ApiModelProperty(value = "用户id")
    private Long userId;
    /** 质押订单号(唯一) */
    @Excel(name = "订单号", sort = 2, width = 30)
    @ApiModelProperty(value = "质押订单号(唯一)")
    private String orderNo;
    /** 购买份数 */
    @Excel(name = "购买份数", sort = 3)
    @ApiModelProperty(value = "购买份数")
    private Integer quantity;
    /** 固定质押OORT数量=stake_unit_amount*quantity */
    @Excel(name = "固定质押OORT数量", sort = 4)
    @ApiModelProperty(value = "固定质押OORT数量=stake_unit_amount*quantity")
    private BigDecimal stakeOortAmount;
    /** 额外质押USDT等值=extra_stake_value_usdt*quantity */
    @Excel(name = "额外质押USDT价值", sort = 5)
    @ApiModelProperty(value = "额外质押USDT等值=extra_stake_value_usdt*quantity")
    private BigDecimal extraValueUsdt;
    /** 下单时OORT价格(USDT) */
    @Excel(name = "OORT价格", sort = 6)
    @ApiModelProperty(value = "下单时OORT价格(USDT)")
    private BigDecimal oortPriceUsdt;
    /** 额外质押折算OORT数量=extra_value_usdt/oort_price_usdt */
    @Excel(name = "额外质押折算OORT数量", sort = 7)
    @ApiModelProperty(value = "额外质押折算OORT数量=extra_value_usdt/oort_price_usdt")
    private BigDecimal extraStakeOortAmount;
    /** 每日产出OORT=product.day_reward*quantity */
    @Excel(name = "每日产出OORT", sort = 8)
    @ApiModelProperty(value = "每日产出OORT=product.day_reward*quantity")
    private BigDecimal dayReward;
    /** 理论总产出OORT=day_reward*valid_days */
    @Excel(name = "理论总产出OORT", sort = 9)
    @ApiModelProperty(value = "理论总产出OORT=day_reward*valid_days")
    private BigDecimal totalYieldTarget;
    /** 已产出OORT(累计) */
    @Excel(name = "已产出OORT", sort = 10)
    @ApiModelProperty(value = "已产出OORT(累计)")
    private BigDecimal yieldedAmount;
    /** 状态 0:产出中,1:已到期,2:暂停,3:取消 */
    @Excel(name = "订单状态", sort = 11,dictType = "t_stake_order_status")
    @ApiModelProperty(value = "状态 0:产出中,1:已到期,2:暂停,3:取消")
    private Integer status;
    /** 有效期天数 */
    @Excel(name = "有效期", sort = 12)
    @ApiModelProperty(value = "有效期天数")
    private Integer validDays;
    /** 剩余有效期天数 */
    @Excel(name = "剩余有效期", sort = 13)
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
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("orderNo", getOrderNo())
            .append("quantity", getQuantity())
            .append("stakeOortAmount", getStakeOortAmount())
            .append("extraValueUsdt", getExtraValueUsdt())
            .append("oortPriceUsdt", getOortPriceUsdt())
            .append("extraStakeOortAmount", getExtraStakeOortAmount())
            .append("dayReward", getDayReward())
            .append("totalYieldTarget", getTotalYieldTarget())
            .append("yieldedAmount", getYieldedAmount())
            .append("status", getStatus())
            .append("validDays", getValidDays())
            .append("haveDays", getHaveDays())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
        .toString();
    }
}
