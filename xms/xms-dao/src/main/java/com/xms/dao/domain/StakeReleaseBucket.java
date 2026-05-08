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
@TableName(value = "t_stake_release_bucket")
public class StakeReleaseBucket extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @Excel(name = "用户ID", sort = 1)
    @ApiModelProperty(value = "用户ID")
    private Long userId;

    @Excel(name = "释放币种", sort = 2, dictType = "biz_stake_reward_coin_type")
    @ApiModelProperty(value = "释放币种 3:OORT 5:产出DFC")
    private Integer coinType;

    @Excel(name = "订单号", sort = 3, width = 30)
    private String orderNo;

    @Excel(name = "释放天数", sort = 4)
    @ApiModelProperty(value = "线性释放天数")
    private Integer linearDays;

    @Excel(name = "剩余天数", sort = 5)
    private Integer haveDays;

    @Excel(name = "累计应释放总量", sort = 6)
    @ApiModelProperty(value = "桶内累计应线性释放总量")
    private BigDecimal totalAmount;

    @Excel(name = "剩余待释放量", sort = 7)
    private BigDecimal remainingAmount;

    @Excel(name = "每日释放数量", sort = 8)
    private BigDecimal dailyReleaseAmount;

    @Excel(name = "订单状态", sort = 9, dictType = "t_stake_order_status")
    @ApiModelProperty(value = "状态 0:释放中 1:已完成 2:暂停")
    private Integer status;

    private Integer startTime;

    private Integer lastReleaseTime;

    @Excel(name = "来源快照")
    @ApiModelProperty(value = "来源快照(json)")
    private String sourceSnapshot;

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
            .append("coinType", getCoinType())
            .append("orderNo", getOrderNo())
            .append("linearDays", getLinearDays())
            .append("haveDays", getHaveDays())
            .append("totalAmount", getTotalAmount())
            .append("remainingAmount", getRemainingAmount())
            .append("dailyReleaseAmount", getDailyReleaseAmount())
            .append("status", getStatus())
            .append("startTime", getStartTime())
            .append("lastReleaseTime", getLastReleaseTime())
            .append("sourceSnapshot", getSourceSnapshot())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
