package com.xms.dao.entity.bo;

import com.xms.common.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;

/**
 * <p>
 * 用户信息表
 * </p>
 *
 *
 * @since 2023-07-25
 */
@Data
public class UserInfoBo{


	/**
	 * 用户id
	 */
	private Long userId;

	/**
	 * 用户钱包地址
	 */
	private String account;

	/**
	 * 用户编码
	 */
	private String userCode;

	/**
	 * 是否服务中心身份 0:否,1:是
	 */
	private Integer hasServiceCenter;


	/**
	 * 用户等级 0:暂无,1:区代理,2:县代理,3:市代理,4:省代理,5:全国代理
	 */
	private Integer gameLevel;

//	/**
//	 * 邀请用户编码
//	 */
//	private String inviteUserCode;

	/**
	 * 邀请用户钱包地址
	 */
	private String inviteUserAccount;

//	/**
//	 * 邀请用户id
//	 */
//	private Long inviteUserId;

	/**
	 * 是否有效用户 买过矿机(0.否 1.是)
	 */
	private Integer isValid;

	/**
	 * 直推用户数
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

//	/**
//	 * 直推业绩(节点数量)
//	 */
//	private BigDecimal subPerformance;

	/**
	 * 团队业绩(节点数量)
	 */
	private BigDecimal umbrellaPerformance;

	/**
	 * 小区业绩(购买矿机)
	 */
	private BigDecimal communityPerformance;
}
