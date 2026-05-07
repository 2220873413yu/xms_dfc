package com.xms.app.service;

import com.xms.app.entity.resp.DfTransferRecordResp;
import com.xms.app.entity.resp.SwapConfigInfoResp;
import com.xms.app.entity.resp.SwapConfigLog;
import com.xms.app.entity.vo.CreateSwapOrderReq;
import com.xms.app.entity.vo.TransferOrderVo;
import com.xms.common.core.domain.api.ResultPista;
import com.xms.dao.domain.UserTransfer;
import jakarta.validation.Valid;

import java.util.List;

public interface BizTransferService {
	/**
	 * 发起转账
	 * @param req
	 * @param userId
	 * @return
	 */
	ResultPista createOrder(TransferOrderVo req, Long userId);

	/**
	 * 转账记录
	 * @param lastId lastId
	 * @param type 1:转账记录,2:收款记录,3:查询转账+收款记录
	 * @param coinType 1:USDT,2:DFC,3:OORT
	 * @return
	 */
	ResultPista<List<UserTransfer>> listTransferRecord(Long lastId, Integer type, Integer coinType);

	/**
	 * 闪兑配置
	 * @return
	 */
	ResultPista<List<SwapConfigInfoResp>> swapConfigInfo();

	/**
	 * 产出记录
	 * @param lastId
	 * @param coinType
	 * @return
	 */
	ResultPista<List<SwapConfigLog>> swapConfigLog(Long lastId, Integer coinType);

	/**
	 * 发起闪兑
	 * @param req
	 * @param userId
	 * @return
	 */
	ResultPista createSwapOrder(CreateSwapOrderReq req, Long userId);

	/**
	 * df转账记录
	 * @param lastId
	 * @return
	 */
	ResultPista<List<DfTransferRecordResp>> dfTransferRecord(Long lastId);
}
