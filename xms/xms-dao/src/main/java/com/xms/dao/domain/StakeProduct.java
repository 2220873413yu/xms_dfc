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
 * 质押套餐对象 t_stake_product
 *
 * @author xms
 * @date 2026-02-27
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_stake_product")
public class StakeProduct extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键id */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 质押名称 */
    @Excel(name = "质押名称")
    @ApiModelProperty(value = "质押名称")
    private String name;
    /** 销量 */
    @Excel(name = "销量")
    @ApiModelProperty(value = "销量")
    private Integer sales;
    /** 质押数量 */
    @Excel(name = "质押数量")
    @ApiModelProperty(value = "质押数量")
    private BigDecimal stakeUnitAmount;
    /** 额外需要质押的USDT等值金额(按价格换算为OORT) */
    @Excel(name = "额外需要质押的USDT等值金额(按价格换算为OORT)")
    @ApiModelProperty(value = "额外需要质押的USDT等值金额(按价格换算为OORT)")
    private BigDecimal extraStakeValueUsdt;
    /** 每天产出 */
    @Excel(name = "每天产出")
    @ApiModelProperty(value = "每天产出")
    private BigDecimal dayReward;
    /** 订单有效期(天)，如360 */
    @Excel(name = "订单有效期(天)，如360")
    @ApiModelProperty(value = "订单有效期(天)，如360")
    private Integer validDays;
    /** 是否上架 0:否,1:是 */
    @Excel(name = "是否上架 0:否,1:是")
    @ApiModelProperty(value = "是否上架 0:否,1:是")
    private Integer status;

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
            .append("name", getName())
            .append("sales", getSales())
            .append("stakeUnitAmount", getStakeUnitAmount())
            .append("extraStakeValueUsdt", getExtraStakeValueUsdt())
            .append("dayReward", getDayReward())
            .append("validDays", getValidDays())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
        .toString();
    }
}
