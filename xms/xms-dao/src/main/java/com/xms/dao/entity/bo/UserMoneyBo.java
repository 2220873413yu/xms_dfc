package com.xms.dao.entity.bo;

import com.xms.common.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @createDate: 2023/7/26 15:18
 */
@Data
public class UserMoneyBo{
	/**
	 * USDT
	 */
	private BigDecimal validNum1;

	/**
	 * DFC
	 */
	private BigDecimal validNum2;

	/**
	 * OORT
	 */
	private BigDecimal validNum3;

	/**
	 * 锁定USDT
	 */
	private BigDecimal validNum4;

	/**
	 * 产出DFC
	 */
	private BigDecimal validNum5;

	/**
	 * 代理分红收益
	 */
	private BigDecimal validNum6;

	/**
	 * 运营收益
	 */
	private BigDecimal validNum7;
}
