package com.xms.app.entity.vo;

import com.xms.common.annotation.ValidDiyStatus;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 发起闪兑
 */
@Data
public class CreateSwapOrderReq {

	/**
	 * 源币种数量
	 */
	@NotNull
	@Positive
	private BigDecimal amount;

	/**
	 * 币种类型 5:产出DFC,6:代理分红收益,7:运营收益
	 */
	@NotNull
	@ValidDiyStatus(values = {5,6,7}, message = "coinType error")
	private Integer coinType;
	/**
	 * 签名
	 */
	@ApiModelProperty(value = "签名")
	private String signature;

	/**
	 * 随机数不能为空
	 */
	@NotBlank(message = "随机数不能为空")
	private String randomNum;
}
