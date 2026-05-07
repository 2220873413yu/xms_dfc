package com.xms.app.service.impl;

import com.xms.app.entity.dto.MyReleaseBucketListDto;
import com.xms.app.entity.dto.MyStakeInfoDto;
import com.xms.app.entity.dto.MyStakeInfoListDto;
import com.xms.app.entity.dto.StakeInfoDTO;
import com.xms.app.entity.vo.CreateStakeOrderVo;
import com.xms.app.service.BizCommonService;
import com.xms.app.service.BizStakeService;
import com.xms.common.config.redis.XmsRedis;
import com.xms.common.config.redis.delayqueue.RedissonDelayHandler;
import com.xms.common.config.redis.lock.RedisLock;
import com.xms.common.constant.ConstantStatic;
import com.xms.common.constant.ConstantType;
import com.xms.common.constant.RedisConstant;
import com.xms.common.constant.SysConstant;
import com.xms.common.core.domain.api.ResultPista;
import com.xms.common.exception.ServiceException;
import com.xms.common.mq.dynamic.AsyncDynamicOrderSettlementService;
import com.xms.common.result.ResponseCode;
import com.xms.common.utils.BeanUtil;
import com.xms.common.utils.CollectionUtil;
import com.xms.common.utils.Func;
import com.xms.common.utils.SecurityUtils;
import com.xms.common.utils.uuid.IDUtils;
import com.xms.dao.domain.*;
import com.xms.dao.entity.domain.UserInfo;
import com.xms.dao.entity.domain.UserMoney;
import com.xms.dao.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.xms.app.service.impl.BizUserServiceImpl.checkWallet;

/**
 * 质押相关业务
 * @author xms
 * @date 2023/6/12
 */
@Service
@Slf4j
public class BizStakeServiceImpl implements BizStakeService {
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

	/**
	 * 质押订单列表
	 * @param lastId id
	 * @return
	 */
	@Override
	public List<MyStakeInfoListDto> destroyOrderList(Long lastId) {
		List<StakeOrder> packageOrderList = stakeOrderService.lambdaQuery()
			.eq(StakeOrder::getUserId, SecurityUtils.getFrontUserId())
			.lt(Func.isNotEmpty(lastId), StakeOrder::getId, lastId)
			.orderByDesc(StakeOrder::getId)
			.last(SysConstant.PAGE_LIMIT)
			.list();
		if(CollectionUtil.isEmpty(packageOrderList)){
			return new ArrayList<>();
		}
		List<MyStakeInfoListDto> result = packageOrderList.stream().map(item -> {
			MyStakeInfoListDto dto = new MyStakeInfoListDto();
			BeanUtil.copyProperties(item, dto);
			if(dto.getYieldedAmount().compareTo(BigDecimal.ZERO)<=0){
				dto.setDayReward(BigDecimal.ZERO);
			}
			return dto;
		}).collect(Collectors.toList());
		return result;
	}

	/**
	 * 锁仓订单列表
	 * @param lastId id
	 * @return
	 */
	@Override
	public List<MyReleaseBucketListDto> myReleaseBucketList(Long lastId) {
		List<StakeReleaseBucket> releaseBucketList = stakeReleaseBucketService.lambdaQuery()
			.eq(StakeReleaseBucket::getUserId, SecurityUtils.getFrontUserId())
			.lt(Func.isNotEmpty(lastId), StakeReleaseBucket::getId, lastId)
			.orderByDesc(StakeReleaseBucket::getId)
			.last(SysConstant.PAGE_LIMIT)
			.list();
		if(CollectionUtil.isEmpty(releaseBucketList)){
			return new ArrayList<>();
		}
		List<MyReleaseBucketListDto> result = releaseBucketList.stream().map(item -> {
			MyReleaseBucketListDto dto = new MyReleaseBucketListDto();
			BeanUtil.copyProperties(item, dto);
			return dto;
		}).collect(Collectors.toList());
		return result;
	}

	@Override
	public MyStakeInfoDto myStakeInfo() {
		MyStakeInfoDto result = new MyStakeInfoDto();
		Long userId = SecurityUtils.getLoginAppUser().getUserId();
		List<StakeOrder> stakeOrderList = stakeOrderService.lambdaQuery()
			.eq(StakeOrder::getUserId, userId)
			.eq(StakeOrder::getStatus, 0)
			.select(StakeOrder::getQuantity)
			.list();
		if(CollectionUtil.isEmpty(stakeOrderList)){
			result.setHaveOrder(0L);
		}else{
			Long count = stakeOrderList.stream()
				.mapToLong(StakeOrder::getQuantity)
				.sum();
			result.setHaveOrder(count);
		}

		//todo 今日产出
		List<RewardRecord> rewardRecordList = rewardRecordService.lambdaQuery()
			.eq(RewardRecord::getUserId, userId)
			.in(RewardRecord::getSourceType, 8, 9)
			.select(RewardRecord::getAmount, RewardRecord::getSourceType)
			.list();
		if(CollectionUtil.isNotEmpty(rewardRecordList)){
			BigDecimal todayOutReward = rewardRecordList.stream()
				.filter(item -> item.getSourceType() != null && item.getSourceType() == 8)
				.map(item -> item.getAmount() == null ? BigDecimal.ZERO : item.getAmount())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal todayOutBucketAmount = rewardRecordList.stream()
				.filter(item -> item.getSourceType() != null && item.getSourceType() == 9)
				.map(item -> item.getAmount() == null ? BigDecimal.ZERO : item.getAmount())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			result.setTodayOutReward(todayOutReward);
			result.setTodayOutBucketAmount(todayOutBucketAmount);
		}
		//锁仓数量
		List<StakeReleaseBucket> releaseBucketList = stakeReleaseBucketService.lambdaQuery()
			.eq(StakeReleaseBucket::getUserId, userId)
			.eq(StakeReleaseBucket::getStatus, 0)
			.list();
		if(CollectionUtil.isNotEmpty(releaseBucketList)){
			//对releaseBucketList中的remainingAmount求和
			BigDecimal totalAmount = releaseBucketList.stream()
				.map(StakeReleaseBucket::getRemainingAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			result.setBucketAmount(totalAmount);
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisLock(value = RedisConstant.LockConstant.XMS_STAKE_APPLY, param = "#userId")
	public ResultPista stakeOrder(CreateStakeOrderVo req, Long userId) {

		UserInfo userInfo = userInfoService.lambdaQuery()
			.eq(UserInfo::getUserId, userId)
			.one();

		//验签，随机数
		checkWallet(req.getRandomNum(), req.getSignature(), userInfo.getAccount(), xmsRedis);


		StakeProduct stakeProduct = stakeProductService.lambdaQuery()
			.eq(StakeProduct::getStatus, 1)
			.last("limit 1")
			.one();
		if(stakeProduct == null){
			throw new ServiceException(ResponseCode.CODE_1002);
		}

		//余额是否足够
		UserMoney userInfoMoney = userMoneyService.lambdaQuery()
			.eq(UserMoney::getId, userId)
			.one();
		BigDecimal stakeUnitAmount = stakeProduct.getStakeUnitAmount().multiply(new BigDecimal(req.getNum().toString()))
			.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
		BigDecimal payOortAmount = stakeUnitAmount;
		//获取oort价格
		BigDecimal oortPrice = bizCommonService.getOortPrice();
		if(oortPrice.compareTo(BigDecimal.ZERO)<=0){
			throw new ServiceException(ResponseCode.CODE_1258);
		}

		//创建质押对象
		StakeOrder stakeOrder = new StakeOrder();
		BigDecimal bigNum = new BigDecimal(req.getNum());
		if(stakeProduct.getExtraStakeValueUsdt().compareTo(BigDecimal.ZERO)>0){
			BigDecimal extraOortAmount = stakeProduct.getExtraStakeValueUsdt()
				.divide(oortPrice, ConstantStatic.newScale, ConstantStatic.roundingModeNew);
			extraOortAmount = extraOortAmount.multiply(bigNum)
				.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
			payOortAmount = payOortAmount.add(extraOortAmount);
			stakeOrder.setExtraStakeOortAmount(extraOortAmount);
		}else{
			stakeOrder.setExtraStakeOortAmount(BigDecimal.ZERO);
		}

		if(userInfoMoney.getValidNum3().compareTo(payOortAmount)<0){
			throw new ServiceException(ResponseCode.CODE_1015);
		}

		stakeProductService.lambdaUpdate()
			.eq(StakeProduct::getId, stakeProduct.getId())
			.setSql("sales = sales + " + req.getNum())
			.update();

		//订单号
		String orderNo = IDUtils.getSnowflake().nextIdStr();

		//扣款
		int count = userWalletService.handerUserMoney(payOortAmount.negate(), orderNo, userId, userId,
			ConstantType.user_money_log_source_type.type_21, ConstantType.user_money_coin_type.type_3);
		if (count != 1) {
			throw new ServiceException(ResponseCode.CODE_1015);
		}

		stakeOrder.setUserId(userId);
		stakeOrder.setOrderNo(orderNo);
		stakeOrder.setQuantity(req.getNum());
		stakeOrder.setStakeOortAmount(stakeUnitAmount);
		stakeOrder.setExtraValueUsdt(stakeProduct.getExtraStakeValueUsdt()
			.multiply(bigNum)
			.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew));
		stakeOrder.setOortPriceUsdt(oortPrice);

		stakeOrder.setDayReward(stakeProduct.getDayReward()
			.multiply(bigNum).setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew));
		BigDecimal totalYieldTarget = stakeOrder.getDayReward().multiply(new BigDecimal(stakeProduct.getValidDays()))
			.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
		//总产出
		stakeOrder.setTotalYieldTarget(totalYieldTarget);
		stakeOrder.setYieldedAmount(BigDecimal.ZERO);
		stakeOrder.setStatus(0);
		stakeOrder.setValidDays(stakeProduct.getValidDays());
		stakeOrder.setHaveDays(stakeProduct.getValidDays());
		stakeOrder.setCreateTime(new Date());
		boolean save = stakeOrderService.save(stakeOrder);
		if (!save) {
			throw new ServiceException(ResponseCode.CODE_1002);
		}
		return ResultPista.success();
	}

	/**
	 * 获取质押信息
	 * @return
	 */
	@Override
	public StakeInfoDTO stakeInfo() {
		StakeProduct stakeProduct = stakeProductService.lambdaQuery()
			.eq(StakeProduct::getStatus,1)
			.last("limit 1")
			.select(StakeProduct::getId,StakeProduct::getSales,StakeProduct::getStakeUnitAmount,
				StakeProduct::getExtraStakeValueUsdt,StakeProduct::getDayReward,StakeProduct::getValidDays)
			.one();
		StakeInfoDTO stakeInfoDTO = new StakeInfoDTO();
		if(stakeProduct == null){
			return stakeInfoDTO;
		}
		stakeInfoDTO.setId(stakeProduct.getId());
		stakeInfoDTO.setSales(stakeProduct.getSales());
		stakeInfoDTO.setStakeUnitAmount(stakeProduct.getStakeUnitAmount());
		stakeInfoDTO.setExtraStakeValueUsdt(stakeProduct.getExtraStakeValueUsdt());
		stakeInfoDTO.setDayReward(stakeProduct.getDayReward());
		stakeInfoDTO.setValidDays(stakeProduct.getValidDays());
		return stakeInfoDTO;
	}
}
