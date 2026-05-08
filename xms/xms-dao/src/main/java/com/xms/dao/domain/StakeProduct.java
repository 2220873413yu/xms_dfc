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

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_stake_product")
public class StakeProduct extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @Excel(name = "质押名称")
    @ApiModelProperty(value = "质押名称")
    private String name;

    @Excel(name = "质押币种", dictType = "biz_stake_coin_type")
    @ApiModelProperty(value = "质押币种 2:DFC 3:OORT")
    private Integer coinType;

    @Excel(name = "产出币种", dictType = "biz_stake_reward_coin_type")
    @ApiModelProperty(value = "产出币种 3:OORT 5:产出DFC")
    private Integer rewardCoinType;

    @Excel(name = "销量")
    @ApiModelProperty(value = "销量")
    private Integer sales;

    @Excel(name = "可用库存")
    @ApiModelProperty(value = "可用库存份数，OORT可为空不校验")
    private Integer availableStock;

    @Excel(name = "每份质押数量")
    @ApiModelProperty(value = "每份质押数量")
    private BigDecimal stakeUnitAmount;

    @Excel(name = "额外质押USDT等值金额")
    @ApiModelProperty(value = "额外质押USDT等值金额，按OORT价格换算")
    private BigDecimal extraStakeValueUsdt;

    @Excel(name = "每天产出")
    @ApiModelProperty(value = "每天产出")
    private BigDecimal dayReward;

    @Excel(name = "线性释放天数")
    @ApiModelProperty(value = "线性释放天数")
    private Integer linearDays;

    @Excel(name = "订单有效期")
    @ApiModelProperty(value = "订单有效期，单位天")
    private Integer validDays;

    @Excel(name = "上架状态", dictType = "t_user_info_is_valid")
    @ApiModelProperty(value = "是否上架 0:否 1:是")
    private Integer status;

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
            .append("name", getName())
            .append("coinType", getCoinType())
            .append("rewardCoinType", getRewardCoinType())
            .append("sales", getSales())
            .append("availableStock", getAvailableStock())
            .append("stakeUnitAmount", getStakeUnitAmount())
            .append("extraStakeValueUsdt", getExtraStakeValueUsdt())
            .append("dayReward", getDayReward())
            .append("linearDays", getLinearDays())
            .append("validDays", getValidDays())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
