package com.xms.app.entity.resp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.xms.common.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 闪兑配置信息
 */
@Data
public class SwapConfigInfoResp {
	/** 源币种类型 5:产出DFC,6:代理分红收益,7:运营收益 */
	private Integer sourceCoinType;
	/** 闪兑是否打开 0:否,1:是 */
	private Integer swapOpen;
	/** 闪兑价格(1个源币种可兑换多少USDT) */
	private BigDecimal swapPrice;
	/** 手续费率(例如:5表示5%) */
	private BigDecimal feeRatio;
	/** 最小闪兑数量 */
	private BigDecimal minSwapAmount;
}
