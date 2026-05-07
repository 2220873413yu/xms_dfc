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
 * df转账记录
 */
@Data
public class DfTransferRecordResp {
	/** 主键id */
	private Long id;

	/** 用户ID */
	private Long userId;

	/** 划出地址 */
	private String fromAddress;

	/** 划入地址 */
	private String toAddress;

	/** 第三方来源订单号 */
	private String sourceOrderNo;

	/** 订单号 */
	private String orderNo;

	/** 划转金额 */
	private BigDecimal rechargeAmount;

	/** 到账金额 */
	private BigDecimal arrivalAmount;

	/** 创建时间 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;
}
