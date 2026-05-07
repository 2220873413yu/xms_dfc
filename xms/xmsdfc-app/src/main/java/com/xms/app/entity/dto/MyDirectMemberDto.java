package com.xms.app.entity.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.xms.common.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 我的直推成员
 */
@Data
public class MyDirectMemberDto {

	/**
	 * 用户id
	 */
	private Long userId;

	/**
	 * 钱包地址
	 */
	private String account;

	/**
	 * 用户等级 0:暂无,1:区代理,2:县代理,3:市代理,4:省代理,5:全国代理
	 */
	private Integer gameLevel;

	/**
	 * 直推人数
	 */
	private Integer subNum;

	/**
	 * 团队用户数
	 */
	private Integer umbrellaNum;

	/**
	 * 大区业绩
	 */
	private BigDecimal maxTeamPerformance;

	/**
	 * 团队业绩(购买节点)
	 */
	private BigDecimal umbrellaPerformance;

	/**
	 * 小区业绩(购买矿机)
	 */
	private BigDecimal communityPerformance;

//	/**
//	 * 我的业绩(节点信息)
//	 */
//	private BigDecimal performance;

	/**
	 * 创建时间
	 */
	private Date createTime;

//

//	/**
//	 * 团队用户数(有效)
//	 */
//	private Integer validUmbrellaNum;


}
