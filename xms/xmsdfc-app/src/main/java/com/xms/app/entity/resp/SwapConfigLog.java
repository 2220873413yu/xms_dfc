package com.xms.app.entity.resp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xms.common.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 闪对记录
 */
@Data
public class SwapConfigLog {

	/** 主键id */
	private Long id;
	/** 闪兑单号 */
	private String orderNo;
	/** 用户id */
	private Long userId;
	private Integer sourceCoinType;
	/** 目标币种类型 1:USDT */
	private Integer targetCoinType;
	/** 闪兑源数量 */
	private BigDecimal sourceAmount;
	/** 闪兑价格快照 */
	private BigDecimal swapPrice;
	/** 手续费率快照 */
	private BigDecimal feeRatio;
	/** 手续费金额 */
	private BigDecimal feeAmount;
	/** 实际到账USDT */
	private BigDecimal targetNetAmount;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;
}
