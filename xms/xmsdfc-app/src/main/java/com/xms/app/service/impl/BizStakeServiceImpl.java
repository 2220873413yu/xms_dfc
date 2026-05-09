package com.xms.app.service.impl;

import com.xms.app.entity.dto.MyReleaseBucketListDto;
import com.xms.app.entity.dto.MyStakeInfoDto;
import com.xms.app.entity.dto.MyStakeInfoListDto;
import com.xms.app.entity.dto.StakeInfoDTO;
import com.xms.app.entity.vo.CreateStakeOrderVo;
import com.xms.app.service.BizCommonService;
import com.xms.app.service.BizStakeService;
import com.xms.common.config.redis.XmsRedis;
import com.xms.common.config.redis.lock.RedisLock;
import com.xms.common.constant.ConstantStatic;
import com.xms.common.constant.ConstantType;
import com.xms.common.constant.RedisConstant;
import com.xms.common.constant.SysConstant;
import com.xms.common.core.domain.api.ResultPista;
import com.xms.common.exception.ServiceException;
import com.xms.common.result.ResponseCode;
import com.xms.common.utils.BeanUtil;
import com.xms.common.utils.CollectionUtil;
import com.xms.common.utils.Func;
import com.xms.common.utils.SecurityUtils;
import com.xms.common.utils.uuid.IDUtils;
import com.xms.dao.domain.RewardRecord;
import com.xms.dao.domain.StakeOrder;
import com.xms.dao.domain.StakeProduct;
import com.xms.dao.domain.StakeReleaseBucket;
import com.xms.dao.entity.domain.UserInfo;
import com.xms.dao.entity.domain.UserMoney;
import com.xms.dao.service.IRewardRecordService;
import com.xms.dao.service.IStakeOrderService;
import com.xms.dao.service.IStakeProductService;
import com.xms.dao.service.IStakeReleaseBucketService;
import com.xms.dao.service.IUserMoneyService;
import com.xms.dao.service.UserInfoService;
import com.xms.dao.service.UserWalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.xms.app.service.impl.BizUserServiceImpl.checkWallet;

/**
 * 质押相关业务。
 *
 * OORT 和 DFC 复用同一套接口；不传 coinType 时默认查询 OORT，兼容旧 App。
 */
@Service
@Slf4j
public class BizStakeServiceImpl implements BizStakeService {
	private static final Integer DEFAULT_STAKE_COIN_TYPE = ConstantType.user_money_coin_type.type_3;
	private static final Integer DEFAULT_LINEAR_DAYS = 270;
	private static final BigDecimal DEFAULT_IMMEDIATE_RATIO = new BigDecimal("25");
	private static final BigDecimal DEFAULT_LINEAR_RATIO = new BigDecimal("75");

	@Autowired
	private UserInfoService userInfoService;
	@Autowired
	private XmsRedis xmsRedis;
	@Autowired
	private IUserMoneyService userMoneyService;
	@Autowired
	private UserWalletService userWalletService;
	@Autowired
	private BizCommonService bizCommonService;
	@Autowired
	private IStakeProductService stakeProductService;
	@Autowired
	private IStakeOrderService stakeOrderService;
	@Autowired
	private IStakeReleaseBucketService stakeReleaseBucketService;
	@Autowired
	private IRewardRecordService rewardRecordService;

	@Override
	public List<MyStakeInfoListDto> destroyOrderList(Long lastId, Integer coinType) {
		Integer queryCoinType = defaultStakeCoinType(coinType);
		List<StakeOrder> packageOrderList = stakeOrderService.lambdaQuery()
			.eq(StakeOrder::getUserId, SecurityUtils.getFrontUserId())
			.eq(StakeOrder::getCoinType, queryCoinType)
			.lt(Func.isNotEmpty(lastId), StakeOrder::getId, lastId)
			.orderByDesc(StakeOrder::getId)
			.last(SysConstant.PAGE_LIMIT)
			.list();
		if (CollectionUtil.isEmpty(packageOrderList)) {
			return new ArrayList<>();
		}
		return packageOrderList.stream().map(item -> {
			MyStakeInfoListDto dto = new MyStakeInfoListDto();
			BeanUtil.copyProperties(item, dto);
			if (dto.getYieldedAmount() != null && dto.getYieldedAmount().compareTo(BigDecimal.ZERO) <= 0) {
				dto.setDayReward(BigDecimal.ZERO);
			}
			return dto;
		}).collect(Collectors.toList());
	}

	@Override
	public List<MyReleaseBucketListDto> myReleaseBucketList(Long lastId, Integer coinType) {
		Integer queryRewardCoinType = rewardCoinTypeByStakeCoin(defaultStakeCoinType(coinType));
		List<StakeReleaseBucket> releaseBucketList = stakeReleaseBucketService.lambdaQuery()
			.eq(StakeReleaseBucket::getUserId, SecurityUtils.getFrontUserId())
			.eq(StakeReleaseBucket::getCoinType, queryRewardCoinType)
			.lt(Func.isNotEmpty(lastId), StakeReleaseBucket::getId, lastId)
			.orderByDesc(StakeReleaseBucket::getId)
			.last(SysConstant.PAGE_LIMIT)
			.list();
		if (CollectionUtil.isEmpty(releaseBucketList)) {
			return new ArrayList<>();
		}
		return releaseBucketList.stream().map(item -> {
			MyReleaseBucketListDto dto = new MyReleaseBucketListDto();
			BeanUtil.copyProperties(item, dto);
			return dto;
		}).collect(Collectors.toList());
	}

	@Override
	public MyStakeInfoDto myStakeInfo(Integer coinType) {
		MyStakeInfoDto result = new MyStakeInfoDto();
		Long userId = SecurityUtils.getLoginAppUser().getUserId();
		Integer queryCoinType = defaultStakeCoinType(coinType);
		Integer queryRewardCoinType = rewardCoinTypeByStakeCoin(queryCoinType);

		List<StakeOrder> stakeOrderList = stakeOrderService.lambdaQuery()
			.eq(StakeOrder::getUserId, userId)
			.eq(StakeOrder::getCoinType, queryCoinType)
			.eq(StakeOrder::getStatus, 0)
			.select(StakeOrder::getQuantity)
			.list();
		if (CollectionUtil.isNotEmpty(stakeOrderList)) {
			Long count = stakeOrderList.stream().mapToLong(StakeOrder::getQuantity).sum();
			result.setHaveOrder(count);
		}

		int immediateSourceType = queryCoinType.equals(ConstantType.user_money_coin_type.type_2)
			? ConstantType.xms_reward_record_source_type.type_27
			: ConstantType.xms_reward_record_source_type.type_8;
		int linearSourceType = queryCoinType.equals(ConstantType.user_money_coin_type.type_2)
			? ConstantType.xms_reward_record_source_type.type_28
			: ConstantType.xms_reward_record_source_type.type_9;
		List<RewardRecord> rewardRecordList = rewardRecordService.lambdaQuery()
			.eq(RewardRecord::getUserId, userId)
			.in(RewardRecord::getSourceType, immediateSourceType, linearSourceType)
			.select(RewardRecord::getAmount, RewardRecord::getSourceType)
			.list();
		if (CollectionUtil.isNotEmpty(rewardRecordList)) {
			BigDecimal todayOutReward = rewardRecordList.stream()
				.filter(item -> item.getSourceType() != null && item.getSourceType() == immediateSourceType)
				.map(item -> item.getAmount() == null ? BigDecimal.ZERO : item.getAmount())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal todayOutBucketAmount = rewardRecordList.stream()
				.filter(item -> item.getSourceType() != null && item.getSourceType() == linearSourceType)
				.map(item -> item.getAmount() == null ? BigDecimal.ZERO : item.getAmount())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			result.setTodayOutReward(todayOutReward);
			result.setTodayOutBucketAmount(todayOutBucketAmount);
		}

		List<StakeReleaseBucket> releaseBucketList = stakeReleaseBucketService.lambdaQuery()
			.eq(StakeReleaseBucket::getUserId, userId)
			.eq(StakeReleaseBucket::getCoinType, queryRewardCoinType)
			.eq(StakeReleaseBucket::getStatus, 0)
			.list();
		if (CollectionUtil.isNotEmpty(releaseBucketList)) {
			BigDecimal totalAmount = releaseBucketList.stream()
				.map(item -> item.getRemainingAmount() == null ? BigDecimal.ZERO : item.getRemainingAmount())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			result.setBucketAmount(totalAmount);
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisLock(value = RedisConstant.LockConstant.XMS_STAKE_APPLY, param = "#userId")
	public ResultPista stakeOrder(CreateStakeOrderVo req, Long userId) {
		// 下单前先校验钱包签名，避免绕过前端直接发起扣款类请求。
		UserInfo userInfo = userInfoService.lambdaQuery()
			.eq(UserInfo::getUserId, userId)
			.one();

		checkWallet(req.getRandomNum(), req.getSignature(), userInfo.getAccount(), xmsRedis);

		StakeProduct stakeProduct = stakeProductService.lambdaQuery()
			.eq(StakeProduct::getId, req.getId())
			.eq(StakeProduct::getStatus, 1)
			.one();
		if (stakeProduct == null) {
			throw new ServiceException(ResponseCode.CODE_1002);
		}

		// coinType 为空时按旧 OORT 套餐处理；DFC 套餐初始化为 coinType=2、rewardCoinType=5。
		// DFC 本金下单扣可用 DFC(valid_num2)，每日产出由 rewardCoinType=5 进入产出 DFC(valid_num5)。
		// 释放比例和线性释放天数按产品配置快照到订单；OORT不在后台页面配置，但可由数据库配置后对新订单生效。
		Integer productCoinType = defaultStakeCoinType(stakeProduct.getCoinType());
		Integer rewardCoinType = stakeProduct.getRewardCoinType();
		BigDecimal bigNum = new BigDecimal(req.getNum());
		BigDecimal stakeAmount = stakeProduct.getStakeUnitAmount().multiply(bigNum)
			.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
		BigDecimal extraStakeValueUsdt = stakeProduct.getExtraStakeValueUsdt() == null ? BigDecimal.ZERO : stakeProduct.getExtraStakeValueUsdt();
		boolean dfcProduct = productCoinType.equals(ConstantType.user_money_coin_type.type_2);
		BigDecimal immediateRatio = defaultRatio(stakeProduct.getImmediateRatio(), DEFAULT_IMMEDIATE_RATIO);
		BigDecimal linearRatio = defaultRatio(stakeProduct.getLinearRatio(), DEFAULT_LINEAR_RATIO);
		Integer linearDays = stakeProduct.getLinearDays() == null ? DEFAULT_LINEAR_DAYS : stakeProduct.getLinearDays();
		BigDecimal payAmount = stakeAmount;
		BigDecimal oortPrice = BigDecimal.ZERO;
		BigDecimal dfcPrice = BigDecimal.ZERO;

		StakeOrder stakeOrder = new StakeOrder();
		// OORT 质押沿用旧规则：除质押数量外，还要按 USDT 等值换算额外需要扣除的 OORT。
		// DFC 质押没有这笔额外 OORT 扣款，extraStakeOortAmount 固定记 0。
		if (productCoinType.equals(ConstantType.user_money_coin_type.type_3)) {
			oortPrice = bizCommonService.getOortPrice();
			if (oortPrice.compareTo(BigDecimal.ZERO) <= 0) {
				throw new ServiceException(ResponseCode.CODE_1258);
			}
			if (extraStakeValueUsdt.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal extraOortAmount = extraStakeValueUsdt
					.divide(oortPrice, ConstantStatic.newScale, ConstantStatic.roundingModeNew)
					.multiply(bigNum)
					.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
				payAmount = payAmount.add(extraOortAmount);
				stakeOrder.setExtraStakeOortAmount(extraOortAmount);
			} else {
				stakeOrder.setExtraStakeOortAmount(BigDecimal.ZERO);
			}
		} else {
			// DFC 质押也记录下单时价格，后台订单和导出可按历史快照展示，避免后续实时价格变化影响查看。
			dfcPrice = bizCommonService.getDFcPrice();
			if (dfcPrice.compareTo(BigDecimal.ZERO) <= 0) {
				throw new ServiceException(ResponseCode.CODE_1258);
			}
			stakeOrder.setExtraStakeOortAmount(BigDecimal.ZERO);
		}

		UserMoney userInfoMoney = userMoneyService.lambdaQuery()
			.eq(UserMoney::getId, userId)
			.one();
		// DFC 质押扣可用 DFC(valid_num2)，并受库存限制；OORT 质押继续扣 OORT(valid_num3)，保证旧业务不变。
		if (dfcProduct) {
			if (stakeProduct.getAvailableStock() == null || stakeProduct.getAvailableStock() < req.getNum()) {
				throw new ServiceException(ResponseCode.CODE_1259);
			}
			if (userInfoMoney.getValidNum2().compareTo(payAmount) < 0) {
				throw new ServiceException(ResponseCode.CODE_1015);
			}
		} else if (userInfoMoney.getValidNum3().compareTo(payAmount) < 0) {
			throw new ServiceException(ResponseCode.CODE_1015);
		}

		// 销量和库存与订单在同一事务内更新；DFC 多加 available_stock >= num 条件，防止并发下单超卖。
		boolean updateProduct = stakeProductService.lambdaUpdate()
			.eq(StakeProduct::getId, stakeProduct.getId())
			.setSql("sales = sales + " + req.getNum())
			.setSql(dfcProduct,
				"available_stock = available_stock - " + req.getNum())
			.ge(dfcProduct,
				StakeProduct::getAvailableStock, req.getNum())
			.update();
		if (!updateProduct) {
			throw new ServiceException(ResponseCode.CODE_1259);
		}

		String orderNo = IDUtils.getSnowflake().nextIdStr();
		// 钱包流水类型按质押币种区分：DFC 使用新增的 33，OORT 保持旧的 21。
		int sourceType = productCoinType.equals(ConstantType.user_money_coin_type.type_2)
			? ConstantType.user_money_log_source_type.type_33
			: ConstantType.user_money_log_source_type.type_21;
		int count = userWalletService.handerUserMoney(payAmount.negate(), orderNo, userId, userId, sourceType, productCoinType);
		if (count != 1) {
			throw new ServiceException(ResponseCode.CODE_1015);
		}

		stakeOrder.setUserId(userId);
		stakeOrder.setProductId(stakeProduct.getId());
		stakeOrder.setCoinType(productCoinType);
		stakeOrder.setRewardCoinType(rewardCoinType);
		stakeOrder.setOrderNo(orderNo);
		stakeOrder.setQuantity(req.getNum());
		stakeOrder.setStakeAmount(stakeAmount);
		// 旧字段 stakeOortAmount 只记录 OORT 本金；DFC 本金统一看 stakeAmount，避免把 DFC 写入 OORT 字段。
		stakeOrder.setStakeOortAmount(productCoinType.equals(ConstantType.user_money_coin_type.type_3) ? stakeAmount : BigDecimal.ZERO);
		stakeOrder.setExtraValueUsdt(extraStakeValueUsdt.multiply(bigNum)
			.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew));
		stakeOrder.setOortPriceUsdt(oortPrice);
		stakeOrder.setDfcPriceUsdt(dfcPrice);
		stakeOrder.setDayReward(stakeProduct.getDayReward().multiply(bigNum)
			.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew));
		// 释放比例和线性天数下单时固化到订单，后续后台改套餐只影响新订单。
		stakeOrder.setImmediateRatio(immediateRatio);
		stakeOrder.setLinearRatio(linearRatio);
		stakeOrder.setLinearDays(linearDays);
		BigDecimal totalYieldTarget = stakeOrder.getDayReward().multiply(new BigDecimal(stakeProduct.getValidDays()))
			.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
		stakeOrder.setTotalYieldTarget(totalYieldTarget);
		stakeOrder.setYieldedAmount(BigDecimal.ZERO);
		stakeOrder.setStatus(0);
		stakeOrder.setPrincipalRefundStatus(0);
		stakeOrder.setValidDays(stakeProduct.getValidDays());
		stakeOrder.setHaveDays(stakeProduct.getValidDays());
		stakeOrder.setCreateTime(new Date());
		if (!stakeOrderService.save(stakeOrder)) {
			throw new ServiceException(ResponseCode.CODE_1002);
		}
		return ResultPista.success();
	}

	@Override
	public StakeInfoDTO stakeInfo(Integer coinType) {
		Integer queryCoinType = defaultStakeCoinType(coinType);
		StakeProduct stakeProduct = stakeProductService.lambdaQuery()
			.eq(StakeProduct::getCoinType, queryCoinType)
			.eq(StakeProduct::getStatus, 1)
			.last("limit 1")
			.select(StakeProduct::getId, StakeProduct::getCoinType, StakeProduct::getRewardCoinType,
				StakeProduct::getSales, StakeProduct::getAvailableStock, StakeProduct::getStakeUnitAmount,
				StakeProduct::getExtraStakeValueUsdt, StakeProduct::getDayReward,
				StakeProduct::getImmediateRatio, StakeProduct::getLinearRatio,
				StakeProduct::getLinearDays, StakeProduct::getValidDays)
			.one();
		StakeInfoDTO stakeInfoDTO = new StakeInfoDTO();
		if (stakeProduct == null) {
			return stakeInfoDTO;
		}
		stakeInfoDTO.setId(stakeProduct.getId());
		stakeInfoDTO.setCoinType(defaultStakeCoinType(stakeProduct.getCoinType()));
		stakeInfoDTO.setRewardCoinType(stakeProduct.getRewardCoinType());
		stakeInfoDTO.setSales(stakeProduct.getSales());
		stakeInfoDTO.setAvailableStock(stakeProduct.getAvailableStock());
		stakeInfoDTO.setStakeUnitAmount(stakeProduct.getStakeUnitAmount());
		stakeInfoDTO.setExtraStakeValueUsdt(stakeProduct.getExtraStakeValueUsdt());
		stakeInfoDTO.setDayReward(stakeProduct.getDayReward());
		stakeInfoDTO.setImmediateRatio(defaultRatio(stakeProduct.getImmediateRatio(), DEFAULT_IMMEDIATE_RATIO));
		stakeInfoDTO.setLinearRatio(defaultRatio(stakeProduct.getLinearRatio(), DEFAULT_LINEAR_RATIO));
		stakeInfoDTO.setLinearDays(stakeProduct.getLinearDays());
		stakeInfoDTO.setValidDays(stakeProduct.getValidDays());
		return stakeInfoDTO;
	}

	private Integer defaultStakeCoinType(Integer coinType) {
		return coinType == null ? DEFAULT_STAKE_COIN_TYPE : coinType;
	}

	private BigDecimal defaultRatio(BigDecimal ratio, BigDecimal defaultValue) {
		return ratio == null ? defaultValue : ratio;
	}

	private Integer rewardCoinTypeByStakeCoin(Integer coinType) {
		return Integer.valueOf(ConstantType.user_money_coin_type.type_2).equals(coinType)
			? ConstantType.user_money_coin_type.type_5
			: ConstantType.user_money_coin_type.type_3;
	}
}
