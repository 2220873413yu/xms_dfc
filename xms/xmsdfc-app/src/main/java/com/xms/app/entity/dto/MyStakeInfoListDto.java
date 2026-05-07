package com.xms.app.entity.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xms.common.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 我的质押信息列表
 * @author xms
 * @date 2023/10/07
 */
@Data
public class MyStakeInfoListDto {
	/** 主键id */
	private Long id;
	/** 购买份数 */
	private Integer quantity;
	/**
	 * 固定质押OORT数量
	 */
	private BigDecimal stakeOortAmount;

	/** 已产出OORT(累计) */
	private BigDecimal yieldedAmount;

	/** 每日产出OORT=product.day_reward*quantity */
	private BigDecimal dayReward;

	/** 剩余有效期天数 */
	private Integer haveDays;

	/** 创建时间 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;
}
