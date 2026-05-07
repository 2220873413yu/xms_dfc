package com.xms.app.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.xms.app.entity.resp.DfTransferRecordResp;
import com.xms.app.entity.resp.SwapConfigInfoResp;
import com.xms.app.entity.resp.SwapConfigLog;
import com.xms.app.entity.vo.CreateSwapOrderReq;
import com.xms.app.entity.vo.TransferOrderVo;
import com.xms.app.handler.CustomException;
import com.xms.app.service.BizTransferService;
import com.xms.common.google.GoogleAuthenticator;
import com.xms.dao.domain.AssetTransferRecord;
import com.xms.dao.domain.SwapConfig;
import com.xms.dao.domain.SwapRecord;
import com.xms.dao.service.*;
import com.xms.common.config.redis.XmsRedis;
import com.xms.common.config.redis.lock.RedisLock;
import com.xms.common.constant.*;
import com.xms.common.core.domain.api.ResultPista;
import com.xms.common.exception.ServiceException;
import com.xms.common.result.ResponseCode;
import com.xms.common.utils.Func;
import com.xms.common.utils.SecurityUtils;
import com.xms.common.utils.uuid.IDUtils;
import com.xms.dao.api.PullThirdInfo;
import com.xms.dao.domain.UserTransfer;
import com.xms.dao.entity.domain.UserInfo;
import com.xms.dao.entity.domain.UserMoney;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.xms.app.service.impl.BizUserServiceImpl.checkWallet;

@Slf4j
@Service
public class BizTransferServiceImpl implements BizTransferService {

	@Autowired
	private IUserTransferService userTransferService;

	@Autowired
	private UserInfoService userInfoService;

	@Autowired
	private UserWalletService userWalletService;

	@Autowired
	private IUserMoneyService userMoneyService;

	@Autowired
	private ISysParaService sysParaServiceImpl;

	@Autowired
	private XmsRedis xmsRedis;

	@Autowired
	private ISysParaService sysParaService;
	@Autowired
	private PullThirdInfo pullThirdInfo;

	@Autowired
	private ISwapConfigService swapConfigService;

	@Autowired
	private ISwapRecordService swapRecordService;

	@Autowired
	private IAssetTransferRecordService assetTransferRecordService;

	@Override
	public ResultPista<List<DfTransferRecordResp>> dfTransferRecord(Long lastId) {
		Long userId = SecurityUtils.getLoginAppUser().getUserId();
		List<DfTransferRecordResp> result = assetTransferRecordService.lambdaQuery()
			.eq(AssetTransferRecord::getUserId, userId)
			.lt(Func.isNotEmpty(lastId), AssetTransferRecord::getId, lastId)
			.orderByDesc(AssetTransferRecord::getId).last(SysConstant.PAGE_LIMIT)
			.list().stream()
			.map(record -> {
				DfTransferRecordResp entity = new DfTransferRecordResp();
				BeanUtils.copyProperties(record, entity);
				return entity;
			}).collect(Collectors.toList());
		return ResultPista.data(result);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisLock(value = RedisConstant.LockConstant.XMS_TRANSFER_APPLY, param = "#userId")
	public ResultPista createSwapOrder(CreateSwapOrderReq req, Long userId) {
		req.setAmount(req.getAmount().setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew));
		SwapConfig swapConfig = swapConfigService.lambdaQuery()
			.eq(SwapConfig::getSourceCoinType, req.getCoinType())
			.one();

		if(swapConfig == null || swapConfig.getSwapOpen() == 0) {
			throw new CustomException(ResponseCode.CODE_1260);
		}
		UserMoney userMoney = userMoneyService.lambdaQuery()
			.eq(UserMoney::getId, userId)
			.one();

		if(req.getCoinType() == ConstantType.user_money_coin_type.type_5){
			if(userMoney.getValidNum5().compareTo(req.getAmount())<0){
				throw new CustomException(ResponseCode.CODE_1015);
			}
		}else if(req.getCoinType() == ConstantType.user_money_coin_type.type_6){
			if(userMoney.getValidNum6().compareTo(req.getAmount())<0){
				throw new CustomException(ResponseCode.CODE_1015);
			}
		}else if(req.getCoinType() == ConstantType.user_money_coin_type.type_7){
			if(userMoney.getValidNum7().compareTo(req.getAmount())<0){
				throw new CustomException(ResponseCode.CODE_1015);
			}
		}else{
			throw new CustomException(ResponseCode.CODE_114);
		}

		//订单号
		String orderNo = IDUtils.getSnowflake().nextIdStr();

		//闪兑扣除
		int count = userWalletService.handerUserMoney(req.getAmount().negate(), orderNo, userId, userId,
			ConstantType.user_money_log_source_type.type_26, req.getCoinType());
		if (count != 1) {
			throw new ServiceException(ResponseCode.CODE_1015);
		}
		BigDecimal targetNetAmount = swapConfig.getSwapPrice().multiply(req.getAmount())
			.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
		BigDecimal feeAmount =BigDecimal.ZERO;
			if(swapConfig.getFeeRatio().compareTo(BigDecimal.ZERO)>0){
				 feeAmount = swapConfig.getFeeRatio().divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew)
					.multiply(targetNetAmount).setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);

			}
		targetNetAmount = targetNetAmount.subtract(feeAmount);

		//闪兑增加usdt
		 count = userWalletService.handerUserMoney(targetNetAmount, orderNo, userId, userId,
			ConstantType.user_money_log_source_type.type_27, ConstantType.user_money_coin_type.type_1);
		if (count != 1) {
			throw new ServiceException(ResponseCode.CODE_1015);
		}

		SwapRecord swapRecord = new SwapRecord();
		swapRecord.setOrderNo(orderNo);
		swapRecord.setUserId(userId);
		swapRecord.setSourceCoinType(req.getCoinType());
		swapRecord.setTargetCoinType(ConstantType.user_money_coin_type.type_1);
		swapRecord.setSourceAmount(req.getAmount());
		swapRecord.setSwapPrice(swapConfig.getSwapPrice());
		swapRecord.setFeeRatio(swapConfig.getFeeRatio());
		swapRecord.setFeeAmount(feeAmount);
		swapRecord.setTargetNetAmount(targetNetAmount);
		swapRecord.setCreateTime(new Date());
		swapRecordService.save(swapRecord);
		return ResultPista.success();
	}

	@Override
	public ResultPista<List<SwapConfigLog>> swapConfigLog(Long lastId, Integer coinType) {
		List<SwapConfigLog> configLogList = swapRecordService.lambdaQuery()
			.eq(SwapRecord::getUserId, SecurityUtils.getLoginAppUser().getUserId())
			.eq(coinType != null, SwapRecord::getSourceCoinType, coinType)
			.lt(Func.isNotEmpty(lastId), SwapRecord::getId, lastId)
			.orderByDesc(SwapRecord::getId).last(SysConstant.PAGE_LIMIT)
			.list().stream()
			.map(record -> {
				;
				SwapConfigLog log = new SwapConfigLog();
				log.setId(record.getId());
				log.setOrderNo(record.getOrderNo());
				log.setUserId(record.getUserId());
				log.setSourceCoinType(record.getSourceCoinType());
				log.setTargetCoinType(record.getTargetCoinType());
				log.setSourceAmount(record.getSourceAmount());
				log.setSwapPrice(record.getSwapPrice());
				log.setFeeRatio(record.getFeeRatio());
				log.setFeeAmount(record.getFeeAmount());
				log.setTargetNetAmount(record.getTargetNetAmount());
				log.setCreateTime(record.getCreateTime());
				return log;
			}).collect(Collectors.toList());
		return ResultPista.data(configLogList);
	}

	@Override
	public ResultPista<List<SwapConfigInfoResp>> swapConfigInfo() {
		List<SwapConfigInfoResp> result = swapConfigService.lambdaQuery()
			.list()
			.stream().map(record -> {
				SwapConfigInfoResp resp = new SwapConfigInfoResp();
				resp.setSourceCoinType(record.getSourceCoinType());
				resp.setSwapOpen(record.getSwapOpen());
				resp.setSwapPrice(record.getSwapPrice());
				resp.setMinSwapAmount(record.getMinSwapAmount());
				resp.setFeeRatio(record.getFeeRatio());
				return resp;
			}).collect(Collectors.toList());
		return ResultPista.data(result);
	}

	/**
	 * 创建转账订单
	 *
	 * @param req
	 * @param userId
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisLock(value = RedisConstant.LockConstant.XMS_TRANSFER_APPLY, param = "#userId")
	public ResultPista createOrder(TransferOrderVo req, Long userId) {

		//保留小数点后2位
		req.setAmount(req.getAmount().setScale(ConstantStatic.twoScale, ConstantStatic.roundingModeNew));
		if(req.getAmount().compareTo(BigDecimal.ZERO)<=0){
			throw new ServiceException(ResponseCode.CODE_1117);
		}
		//转账是否开启
		if(req.getCoinType().equals(ConstantType.user_money_coin_type.type_1)){
			//usdt转账开关
			String biz_transfer_enable = sysParaService.getValue(ConstantSys.biz_transfer_usdt_enable);
			if(!"1".equals(biz_transfer_enable)){
				throw new ServiceException(ResponseCode.CODE_1251);
			}
			//转账最少限制
			BigDecimal biz_usdt_transfer_min_limit = new BigDecimal(sysParaService.getValue(ConstantSys.biz_usdt_transfer_min_limit));
			if(req.getAmount().compareTo(biz_usdt_transfer_min_limit)<0){
				throw new ServiceException(ResponseCode.CODE_1252);
			}
		}else if(req.getCoinType().equals(ConstantType.user_money_coin_type.type_2)){
			//DFC转账通道
			String biz_transfer_enable = sysParaService.getValue(ConstantSys.biz_transfer_dfc_enable);
			if(!"1".equals(biz_transfer_enable)){
				throw new ServiceException(ResponseCode.CODE_1251);
			}
			//转账最少限制
			BigDecimal biz_dfc_transfer_min_limit = new BigDecimal(sysParaService.getValue(ConstantSys.biz_dfc_transfer_min_limit));
			if(req.getAmount().compareTo(biz_dfc_transfer_min_limit)<0){
				throw new ServiceException(ResponseCode.CODE_1252);
			}
		}else if(req.getCoinType().equals(ConstantType.user_money_coin_type.type_3)){
			//OORT转账通道
			String biz_transfer_enable = sysParaService.getValue(ConstantSys.biz_transfer_oort_enable);
			if(!"1".equals(biz_transfer_enable)){
				throw new ServiceException(ResponseCode.CODE_1251);
			}
			//转账最少限制
			BigDecimal biz_oort_transfer_min_limit = new BigDecimal(sysParaService.getValue(ConstantSys.biz_oort_transfer_min_limit));
			if(req.getAmount().compareTo(biz_oort_transfer_min_limit)<0){
				throw new ServiceException(ResponseCode.CODE_1252);
			}
		}else if(req.getCoinType().equals(ConstantType.user_money_coin_type.type_4)){
			//锁定USDT转账通道
			String biz_transfer_enable = sysParaService.getValue(ConstantSys.biz_transfer_sd_usdt_enable);
			if(!"1".equals(biz_transfer_enable)){
				throw new ServiceException(ResponseCode.CODE_1251);
			}
			//转账最少限制
			BigDecimal biz_sd_usdt_transfer_min_limit = new BigDecimal(sysParaService.getValue(ConstantSys.biz_sd_usdt_transfer_min_limit));
			if(req.getAmount().compareTo(biz_sd_usdt_transfer_min_limit)<0){
				throw new ServiceException(ResponseCode.CODE_1252);
			}
		}else if(req.getCoinType().equals(ConstantType.user_money_coin_type.type_5)){
			//产出DFC转账通道
			String biz_transfer_enable = sysParaService.getValue(ConstantSys.biz_transfer_cc_dfc_enable);
			if(!"1".equals(biz_transfer_enable)){
				throw new ServiceException(ResponseCode.CODE_1251);
			}
			//转账最少限制
			BigDecimal biz_cc_dfc_transfer_min_limit = new BigDecimal(sysParaService.getValue(ConstantSys.biz_cc_dfc_transfer_min_limit));
			if(req.getAmount().compareTo(biz_cc_dfc_transfer_min_limit)<0){
				throw new ServiceException(ResponseCode.CODE_1252);
			}
		}else{
			throw new ServiceException(ResponseCode.CODE_1251);
		}

		UserInfo fromUserInfo = userInfoService.lambdaQuery()
			.eq(UserInfo::getUserId, userId)
			.one();

		//查询接收用户
		UserInfo toUserInfo = userInfoService.lambdaQuery()
			.eq(UserInfo::getAccount, req.getToUserAddress())
			.one();

		if(toUserInfo == null){
			throw new ServiceException(ResponseCode.CODE_1253);
		}

		if(toUserInfo.getUserId().equals(userId)){
			throw new ServiceException(ResponseCode.CODE_1254);
		}


		//判断余额
		UserMoney fromUserMoney = userMoneyService.lambdaQuery()
			.eq(UserMoney::getId, userId)
			.one();

		//验证转账是否符合条件（包括同线互转检查和余额验证）
		//获取对应币种的转账手续费率
		BigDecimal transferFeeRatio = validateTransferAndGetFeeRatio(req, fromUserInfo, toUserInfo, fromUserMoney);

		//验签，随机数
		checkWallet(req.getRandomNum(), req.getSignature(), fromUserInfo.getAccount(), xmsRedis);


		//订单号
		String orderNo = IDUtils.getSnowflake().nextIdStr();

		//转账
		int count = userWalletService.handerUserMoney(req.getAmount().negate(), orderNo, userId, userId, ConstantType.user_money_log_source_type.type_2, req.getCoinType());
		if (count != 1) {
			throw new ServiceException(ResponseCode.CODE_1015);
		}


		//计算手续费
		BigDecimal fee =transferFeeRatio.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew)
			.multiply(req.getAmount()).setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
		BigDecimal actualAmount = req.getAmount().subtract(fee);
		if(actualAmount.compareTo(BigDecimal.ZERO)<=0){
			throw new ServiceException(ResponseCode.CODE_1070);
		}

		//收款
		count = userWalletService.handerUserMoney(actualAmount, orderNo, toUserInfo.getUserId(), userId, ConstantType.user_money_log_source_type.type_3, req.getCoinType());
		if (count != 1) {
			throw new ServiceException(ResponseCode.CODE_1002);
		}
		UserTransfer userTransferOrder = UserTransfer.builder()
			.fromUserId(userId)
			.fromAccount(fromUserInfo.getAccount())
			.toUserId(toUserInfo.getUserId())
			.toAccount(req.getToUserAddress())
			.code(orderNo)
			.feeRatio(transferFeeRatio.toString())
			.changeBalance(req.getAmount())
			.feeBalance(fee)
			.actualAmount(actualAmount)
			.coinType(req.getCoinType())
			.build();
		boolean save = userTransferService.save(userTransferOrder);
		if(!save){
			throw new ServiceException(ResponseCode.CODE_1002);
		}
		return ResultPista.success();
	}

	/**
	 * 验证转账是否符合条件（包括同线互转检查和余额验证）
	 * 获取对应币种的转账手续费率
	 * @param req
	 * @param fromUserInfo
	 * @param toUserInfo
	 * @param fromUserMoney
	 * @return
	 */
	@NotNull
	private BigDecimal validateTransferAndGetFeeRatio(TransferOrderVo req, UserInfo fromUserInfo, UserInfo toUserInfo, UserMoney fromUserMoney) {
		BigDecimal transferFeeRatio =BigDecimal.ZERO;
		if(req.getCoinType().equals(ConstantType.user_money_coin_type.type_1)){

			if(fromUserMoney.getValidNum1().compareTo(req.getAmount())<0){
				throw new ServiceException(ResponseCode.CODE_1015);
			}
			//转账手续费率
			transferFeeRatio = new BigDecimal(sysParaService.getValue(ConstantSys.biz_usdt_transfer_fee_ratio));
		}else if(req.getCoinType().equals(ConstantType.user_money_coin_type.type_2)){
			if(fromUserMoney.getValidNum2().compareTo(req.getAmount())<0){
				throw new ServiceException(ResponseCode.CODE_1015);
			}
			//转账手续费率
			transferFeeRatio = new BigDecimal(sysParaService.getValue(ConstantSys.biz_dfc_transfer_fee_ratio));
		}else if(req.getCoinType().equals(ConstantType.user_money_coin_type.type_3)){
			if(fromUserMoney.getValidNum3().compareTo(req.getAmount())<0){
				throw new ServiceException(ResponseCode.CODE_1015);
			}
			//转账手续费率
			transferFeeRatio = new BigDecimal(sysParaService.getValue(ConstantSys.biz_oort_transfer_fee_ratio));
		}else if(req.getCoinType().equals(ConstantType.user_money_coin_type.type_4)){
			if(fromUserMoney.getValidNum4().compareTo(req.getAmount())<0){
				throw new ServiceException(ResponseCode.CODE_1015);
			}
			//转账手续费率
			transferFeeRatio = new BigDecimal(sysParaService.getValue(ConstantSys.biz_sd_usdt_transfer_fee_ratio));
		}else if(req.getCoinType().equals(ConstantType.user_money_coin_type.type_5)){
			if(fromUserMoney.getValidNum5().compareTo(req.getAmount())<0){
				throw new ServiceException(ResponseCode.CODE_1015);
			}
			//转账手续费率
			transferFeeRatio = new BigDecimal(sysParaService.getValue(ConstantSys.biz_cc_dfc_transfer_fee_ratio));
		}else{
			throw new ServiceException(ResponseCode.CODE_300);
		}
		return transferFeeRatio;
	}

	/**
	 * 转账记录
	 * @param lastId lastId
	 * @param type 1:转账记录,2:收款记录,3:查询转账+收款记录
	 * @param coinType 1:USDT,2:DFC,3:OORT
	 * @return
	 */
	@Override
	public ResultPista<List<UserTransfer>> listTransferRecord(Long lastId,Integer type,Integer coinType) {
		Long userId = SecurityUtils.getLoginAppUser().getUserId();
		type = type ==null ? SysConstant.ONE : type;
		LambdaQueryChainWrapper<UserTransfer> queryWrapper = userTransferService.lambdaQuery()
			.eq(type.equals(SysConstant.ONE),UserTransfer::getFromUserId, userId)
			.eq(type.equals(SysConstant.TWO), UserTransfer::getToUserId, userId)
			.eq(coinType!=null, UserTransfer::getCoinType, coinType)
			.lt(Func.isNotEmpty(lastId), UserTransfer::getId, lastId)
			.orderByDesc(UserTransfer::getId).last(SysConstant.PAGE_LIMIT);

		if (type.equals(SysConstant.THREE)) {
			queryWrapper.and(wrapper -> wrapper
				.eq(UserTransfer::getFromUserId, userId)
				.or()
				.eq(UserTransfer::getToUserId, userId)
			);
		}
		List<UserTransfer> list = queryWrapper.list();
		return ResultPista.data(list);
	}

}
