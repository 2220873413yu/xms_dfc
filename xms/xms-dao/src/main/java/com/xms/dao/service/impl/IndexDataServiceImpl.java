package com.xms.dao.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xms.common.config.redis.XmsRedis;
import com.xms.common.constant.ConstantStatic;
import com.xms.common.constant.ConstantType;
import com.xms.common.constant.RedisConstant;
import com.xms.common.constant.SysConstant;
import com.xms.dao.domain.*;
import com.xms.dao.entity.bo.UserMoneySumDTO;
import com.xms.dao.entity.domain.Withdrawal;
import com.xms.dao.entity.domain.UserMoneyLog;
import com.xms.dao.entity.vo.IndexDataPanelVo;
import com.xms.dao.mapper.SysParaMapper;
import com.xms.dao.mapper.UserInfoMapper;
import com.xms.dao.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class IndexDataServiceImpl implements IndexDataService {

	@Autowired
	private UserInfoMapper userInfoMapper;

	@Autowired
	private XmsRedis xmsRedis;

	@Autowired
	private SysParaMapper sysParaMapper;

	@Resource(name = "asyncExecutor")
	private Executor asyncExecutor;


	@Autowired
	private IMiningPackageOrderService miningPackageOrderService;

	@Autowired
	private IStakeOrderService stakeOrderService;

	@Autowired
	private IStakeReleaseBucketService stakeReleaseBucketService;

	@Autowired
	private IRechargeRecordService rechargeRecordService;

	@Autowired
	private WithdrawalService withdrawalService;

	@Autowired
	private IUserTransferService userTransferService;

	/**
	 * 计算跌幅百分比
	 *
	 * @param previousPrice 前一天的价格
	 * @param currentPrice  今天的价格
	 * @return 跌幅百分比，如果前一天价格为零则返回 null
	 */
	public static BigDecimal calculateDeclinePercentage(BigDecimal previousPrice, BigDecimal currentPrice) {
		if (previousPrice.compareTo(BigDecimal.ZERO) == 0) {
			// 返回 null 表示无法计算
			return BigDecimal.ZERO;
		}
		BigDecimal difference = previousPrice.subtract(currentPrice);
		BigDecimal declinePercentage = difference.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew).multiply(SysConstant.BAIFENBI).setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
		return declinePercentage;
	}

	@Override
	public IndexDataPanelVo getIndexDataPanelVo() {
		IndexDataPanelVo indexDataPanelVo = new IndexDataPanelVo();
		indexDataPanelVo.setV10(BigDecimal.ZERO);

		//质押订单量
		BigDecimal v6 = new BigDecimal(stakeOrderService.lambdaQuery()
			.count());
		indexDataPanelVo.setV6(v6);

		// 质押OORT总金额
		StakeOrder yieldedSum = stakeOrderService.getOne(
			new QueryWrapper<StakeOrder>()
				.select("IFNULL(SUM(yielded_amount),0) AS yielded_amount"),
			false
		);
		BigDecimal v11 = yieldedSum == null || yieldedSum.getYieldedAmount() == null
			? BigDecimal.ZERO
			: yieldedSum.getYieldedAmount();
		indexDataPanelVo.setV11(v11);


		//计算锁仓总量
		StakeReleaseBucket totalRemainingAmountBucket = stakeReleaseBucketService.getOne(
			new QueryWrapper<StakeReleaseBucket>()
				.select("IFNULL(SUM(remaining_amount),0) AS remaining_amount"),
			false
		);
		BigDecimal v12 = totalRemainingAmountBucket == null || totalRemainingAmountBucket.getRemainingAmount() == null
			? BigDecimal.ZERO
			: totalRemainingAmountBucket.getRemainingAmount();
		indexDataPanelVo.setV12(v12);

		//计算已释放总量
		StakeReleaseBucket releasedAmountBucket = stakeReleaseBucketService.getOne(
			new QueryWrapper<StakeReleaseBucket>()
				.select("IFNULL(SUM(total_amount - remaining_amount),0) AS total_amount"),
			false
		);
		BigDecimal v13 = releasedAmountBucket == null || releasedAmountBucket.getTotalAmount() == null
			? BigDecimal.ZERO
			: releasedAmountBucket.getTotalAmount();
		indexDataPanelVo.setV13(v13);


		//全网服务身份数量
		BigDecimal v7 = userInfoMapper.userTotalComputingPower();
		indexDataPanelVo.setV7(v7);

		//全网矿机数量
		BigDecimal v8 = new BigDecimal(miningPackageOrderService.lambdaQuery()
			.count());
		indexDataPanelVo.setV8(v8);
		//质押中的订单
		BigDecimal v9 =  new BigDecimal(miningPackageOrderService.lambdaQuery()
			.gt(MiningPackageOrder::getStatus,0)
			.count());
		indexDataPanelVo.setV9(v9);

		CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
			//全网余额信息
			//indexDataPanelVo.setV3(userInfoService.lambdaQuery().eq(UserInfo::getIsValid, 1).apply("create_time >= CURDATE()").count());
			UserMoneySumDTO userMoneySumDTO = userInfoMapper.queryUserMoneySum();
			indexDataPanelVo.setV26(userMoneySumDTO.getTotalValidNum1());
			indexDataPanelVo.setV27(userMoneySumDTO.getTotalValidNum2());
			/*indexDataPanelVo.setV28(userMoneySumDTO.getTotalValidNum3());*/
		}, asyncExecutor);

		CompletableFuture<Void> future3 = CompletableFuture.runAsync(() -> {
			//转账币种 1:USDT,2:DFC,3:OORT,4:锁定USDT,5:产出DFC
			UserTransfer usdtTransferSum = userTransferService.getOne(
				new QueryWrapper<UserTransfer>()
					.select("IFNULL(SUM(change_balance),0) AS change_balance")
					.eq("coin_type", 1),
				false
			);
			//累计转账USDT
			BigDecimal v36 = usdtTransferSum == null || usdtTransferSum.getChangeBalance() == null
				? BigDecimal.ZERO
				: usdtTransferSum.getChangeBalance();

			UserTransfer dfcTransferSum = userTransferService.getOne(
				new QueryWrapper<UserTransfer>()
					.select("IFNULL(SUM(change_balance),0) AS change_balance")
					.eq("coin_type", 2),
				false
			);
			//累计转账DFC
			BigDecimal v37 = dfcTransferSum == null || dfcTransferSum.getChangeBalance() == null
				? BigDecimal.ZERO
				: dfcTransferSum.getChangeBalance();

			UserTransfer oortTransferSum = userTransferService.getOne(
				new QueryWrapper<UserTransfer>()
					.select("IFNULL(SUM(change_balance),0) AS change_balance")
					.eq("coin_type", 3),
				false
			);
			//累计转账OORT
			BigDecimal v38 = oortTransferSum == null || oortTransferSum.getChangeBalance() == null
				? BigDecimal.ZERO
				: oortTransferSum.getChangeBalance();

			UserTransfer lockedUsdtTransferSum = userTransferService.getOne(
				new QueryWrapper<UserTransfer>()
					.select("IFNULL(SUM(change_balance),0) AS change_balance")
					.eq("coin_type", 4),
				false
			);
			//累计转账锁定USDT
			BigDecimal v39 = lockedUsdtTransferSum == null || lockedUsdtTransferSum.getChangeBalance() == null
				? BigDecimal.ZERO
				: lockedUsdtTransferSum.getChangeBalance();

			UserTransfer outputDfcTransferSum = userTransferService.getOne(
				new QueryWrapper<UserTransfer>()
					.select("IFNULL(SUM(change_balance),0) AS change_balance")
					.eq("coin_type", 5),
				false
			);
			//累计转账产出DFC
			BigDecimal v40 = outputDfcTransferSum == null || outputDfcTransferSum.getChangeBalance() == null
				? BigDecimal.ZERO
				: outputDfcTransferSum.getChangeBalance();

			indexDataPanelVo.setV36(v36);
			indexDataPanelVo.setV37(v37);
			indexDataPanelVo.setV38(v38);
			indexDataPanelVo.setV39(v39);
			indexDataPanelVo.setV40(v40);
		}, asyncExecutor);

		CompletableFuture<Void> future4 = CompletableFuture.runAsync(() -> {
			//累计充值USDT
			RechargeRecord usdtSum = rechargeRecordService.getOne(
				new QueryWrapper<RechargeRecord>()
					.select("IFNULL(SUM(recharge_amount),0) AS recharge_amount")
					.eq("status", 1)
					.eq("coin_type", 1),
				false
			);
			BigDecimal v29 = usdtSum == null || usdtSum.getRechargeAmount() == null
				? BigDecimal.ZERO
				: usdtSum.getRechargeAmount();
			//累计充值DFC
			RechargeRecord dfcSum = rechargeRecordService.getOne(
				new QueryWrapper<RechargeRecord>()
					.select("IFNULL(SUM(recharge_amount),0) AS recharge_amount")
					.eq("status", 1)
					.eq("coin_type", 2),
				false
			);
			BigDecimal v30 = dfcSum == null || dfcSum.getRechargeAmount() == null
				? BigDecimal.ZERO
				: dfcSum.getRechargeAmount();
			//累计充值OORT
			RechargeRecord oortSum = rechargeRecordService.getOne(
				new QueryWrapper<RechargeRecord>()
					.select("IFNULL(SUM(recharge_amount),0) AS recharge_amount")
					.eq("status", 1)
					.eq("coin_type", 3),
				false
			);
			BigDecimal v31 = oortSum == null || oortSum.getRechargeAmount() == null
				? BigDecimal.ZERO
				: oortSum.getRechargeAmount();



			indexDataPanelVo.setV29(v29);
			indexDataPanelVo.setV30(v30);
			indexDataPanelVo.setV31(v31);

			//提现币种 1:USDT,2:DFC,3:OORT,5:产出DFC
			Withdrawal usdtWdwSum = withdrawalService.getOne(
				new QueryWrapper<Withdrawal>()
					.select("IFNULL(SUM(change_balance),0) AS change_balance")
					.eq("status", 3)
					.eq("coin_type", 1),
				false
			);
			//累计提现USDT
			BigDecimal v32 = usdtWdwSum == null || usdtWdwSum.getChangeBalance() == null
				? BigDecimal.ZERO
				: usdtWdwSum.getChangeBalance();
			Withdrawal dfcWdwSum = withdrawalService.getOne(
				new QueryWrapper<Withdrawal>()
					.select("IFNULL(SUM(change_balance),0) AS change_balance")
					.eq("status", 3)
					.eq("coin_type", 2),
				false
			);
			//累计提现DFC
			BigDecimal v33 = dfcWdwSum == null || dfcWdwSum.getChangeBalance() == null
				? BigDecimal.ZERO
				: dfcWdwSum.getChangeBalance();
			Withdrawal oortWdwSum = withdrawalService.getOne(
				new QueryWrapper<Withdrawal>()
					.select("IFNULL(SUM(change_balance),0) AS change_balance")
					.eq("status", 3)
					.eq("coin_type", 3),
				false
			);
			//累计提现OORT
			BigDecimal v34 = oortWdwSum == null || oortWdwSum.getChangeBalance() == null
				? BigDecimal.ZERO
				: oortWdwSum.getChangeBalance();
			Withdrawal outputDfcWdwSum = withdrawalService.getOne(
				new QueryWrapper<Withdrawal>()
					.select("IFNULL(SUM(change_balance),0) AS change_balance")
					.eq("status", 3)
					.eq("coin_type", 5),
				false
			);
			//累计提现产出DFC
			BigDecimal v35 = outputDfcWdwSum == null || outputDfcWdwSum.getChangeBalance() == null
				? BigDecimal.ZERO
				: outputDfcWdwSum.getChangeBalance();
			indexDataPanelVo.setV32(v32);
			indexDataPanelVo.setV33(v33);
			indexDataPanelVo.setV34(v34);
			indexDataPanelVo.setV35(v35);
		}, asyncExecutor);

		CompletableFuture.allOf(future2, future3,future4).join();
		return indexDataPanelVo;
	}

	public String getValue(String code) {
		return xmsRedis.get(RedisConstant.XMS_PARAM + code, () -> sysParaMapper.getValue(code), 15L, TimeUnit.DAYS);
	}
}
