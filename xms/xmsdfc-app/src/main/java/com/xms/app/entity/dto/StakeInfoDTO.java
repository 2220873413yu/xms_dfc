package com.xms.app.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.xms.common.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 质押信息
 * @author xms
 * @date 2023/6/12
 */
@Data
public class StakeInfoDTO {

	/** 主键id */
	private Long id;
	private Integer coinType;
	private Integer rewardCoinType;
	/** 销量 */
	private Integer sales;
	private Integer availableStock;
	/** 质押价格 */
	private BigDecimal stakeUnitAmount;
	/** 额外需要质押的USDT等值金额(按价格换算为OORT) */
	private BigDecimal extraStakeValueUsdt;
	/** 每天产出 */
	private BigDecimal dayReward;
	private Integer linearDays;
	/** 订单有效期(天)，如360 */
	private Integer validDays;
}
