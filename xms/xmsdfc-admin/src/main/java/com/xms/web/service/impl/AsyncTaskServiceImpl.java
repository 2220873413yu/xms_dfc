package com.xms.web.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.xms.common.config.redis.XmsRedis;
import com.xms.common.config.redis.delayqueue.RedissonDelayHandler;
import com.xms.common.config.redis.delayqueue.RedissonDelayOrder;
import com.xms.common.config.redis.delayqueue.config.RedissonTemplate;
import com.xms.common.config.redis.stream.ReadOffsetModel;
import com.xms.common.config.redis.stream.RenegadeStreamTemplate;
import com.xms.common.constant.*;
import com.xms.common.core.domain.BaseEntity;
import com.xms.common.exception.ServiceException;
import com.xms.common.mq.dynamic.AsyncDynamicOrderSettlementService;
import com.xms.common.mq.dynamic.OrderMsgDO;
import com.xms.common.utils.Func;
import com.xms.common.utils.SecurityUtils;
import com.xms.common.utils.spring.SpringUtils;
import com.xms.common.utils.uuid.IDUtils;
import com.xms.dao.domain.*;
import com.xms.dao.entity.bo.*;
import com.xms.dao.entity.domain.*;
import com.xms.dao.entity.vo.ParentUserTaskVo;
import com.xms.dao.mapper.AsyncTaskMapper;
import com.xms.dao.service.*;
import com.xms.dao.service.UserRelationService;
import com.xms.dao.service.impl.*;
import com.xms.quartz.mapper.SysJobLogMapper;
import com.xms.system.mapper.SysLogininforMapper;
import com.xms.system.mapper.SysOperLogMapper;
import com.xms.web.domain.ReleaseMiningBo;
import com.xms.web.domain.dto.SourceType0OrderDto;
import com.xms.web.domain.dto.TodayRewardOrderDto;
import com.xms.web.service.IAsyncTaskService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.core.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: renengadePISTA
 * @createDate: 2023/9/18
 */
@Service
@AllArgsConstructor
@Slf4j
public class AsyncTaskServiceImpl implements IAsyncTaskService {
	private static final String SQL_VALID_NUM1 = "UPDATE t_user_money SET update_time=?,gt_id=?,valid_num1=valid_num1+?,source_code=?,source_type=?,source_id=? WHERE id=? ";
	private static final String SQL_VALID_NUM2 = "UPDATE t_user_money SET update_time=?,gt_id=?,valid_num2=valid_num2+?,source_code=?,source_type=?,source_id=? WHERE id=? ";
	private static final String SQL_VALID_NUM6 = "UPDATE t_user_money SET update_time=?,gt_id=?,valid_num6=valid_num6+?,source_code=?,source_type=?,source_id=? WHERE id=? ";
	private static final String SQL_VALID_NUM7 = "UPDATE t_user_money SET update_time=?,gt_id=?,valid_num7=valid_num7+?,source_code=?,source_type=?,source_id=? WHERE id=? ";
	private static final String SQL_VALID_NUM3 = "UPDATE t_user_money SET update_time=?,gt_id=?,valid_num3=valid_num3+?,source_code=?,source_type=?,source_id=? WHERE id=? ";

	private static final String SQL_VALID_NUM5 = "UPDATE t_user_money SET update_time=?,gt_id=?,valid_num5=valid_num5+?,source_code=?,source_type=?,source_id=? WHERE id=? ";
	private static final String SQL_UPDATE_AWAITING_AMOUNT = "UPDATE t_mining_package_order SET awaiting_amount = awaiting_amount + ? WHERE id = ?";
	private static final BigDecimal STAKE_IMMEDIATE_RATIO = new BigDecimal("25");
	private static final BigDecimal STAKE_LINEAR_RATIO = new BigDecimal("75");
	private final AsyncTaskMapper asyncTaskMapper;
	private final JdbcTemplate jdbcTemplate;
	private final RenegadeStreamTemplate redisTemplate;
	private final SysJobLogMapper jobLogMapper;
	private final SysOperLogMapper sysOperLogMapper;
	private final SysLogininforMapper sysLogininforMapper;
	private final IMqTransactionLogService mqTransactionLogServiceImpl;
	private final UserInfoService userInfoService;
	private final IUserLevelConfigService userLevelConfigService;
	private final WithdrawalService withdrawalServiceImpl;
	private final AsyncDynamicOrderSettlementService asyncDynamicOrderSettlementServiceImpl;
	private final IMiningPackageOrderService miningPackageOrderService;
	private final ISysParaService sysParaServiceImpl;
	private final IRewardRecordService rewardRecordService;
	private final UserRelationService userRelationService;
	private final IMiningMgmtFeeConfigService miningMgmtFeeConfigService;
	private final IStakeOrderService stakeOrderService;
	private final IStakeReleaseBucketService stakeReleaseBucketService;
	private final IMiningPackageTierService miningPackageTierService;
	private final UserMoneyLogService userMoneyLogService;
	private final UserWalletService userWalletServiceImpl;
	private final IUserMoneyService userMoneyService;
	/**
	 * 批量更新订单可领取金额
	 *
	 * @param orderList 订单列表
	 */
	private void batchUpdateAwaitingAmount(List<SourceType0OrderDto> orderList) {
		int[] ints = jdbcTemplate.batchUpdate(SQL_UPDATE_AWAITING_AMOUNT, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setBigDecimal(1, orderList.get(i).getAwaitingAmount());
				ps.setLong(2, orderList.get(i).getId());
			}

			@Override
			public int getBatchSize() {
				return orderList.size();
			}
		});

		if (ArrayUtil.contains(ints, 0)) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			log.error("订单可领取金额更新回滚");
			throw new ServiceException("订单可领取金额更新失败");
		}
	}

	@Override
	public Map<String, Object> getTask(String type, String date) {
		Map<String, Object> task = new HashMap<>();
		task.put("type", type);
		task.put("date", date);
		return asyncTaskMapper.getTask(task);
	}


	@Override
	public int addTask(String type, String date) {
		Map<String, Object> task = new HashMap<>();
		task.put("type", type);
		task.put("date", date);
		return asyncTaskMapper.addTask(task);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean transactionExcute(String customerNo, String transactionId) {
		//这里写入业务逻辑的干活
		// 以传入的transactionId作为主键或者单独使用一个字段记录该事务id需要添加唯一索引用于消息回查.必须唯一
		log.info("分布式事务业务处理成功");
		return true;

	}

	@Override
	public void dealRedisDeadMsg() {
		int size;
		while (true) {
			List<MapRecord<String, String, byte[]>> read = redisTemplate.xRead(RedisConstant.StreamMsgConstant.XMS_DEAD_MSG, 1000,
				ReadOffsetModel.START.getReadOffset().getOffset());
			if (read == null) {
				return;
			}
			size = read.size();
			if (size < 1) {
				return;
			}
			log.info("read ===========>>>>>>>>>{}", read);
			for (MapRecord<String, String, byte[]> entries : read) {
				Map<String, byte[]> recordValue = entries.getValue();
				log.debug("{} 拉取msg：{}", RedisConstant.StreamMsgConstant.XMS_DEAD_MSG, recordValue);
				for (Map.Entry<String, byte[]> entry : recordValue.entrySet()) {
					String strKey = entry.getKey();
					String[] keys = strKey.split("@");
					if (keys.length != SysConstant.TWO) {
						return;
					}
					ThreadUtil.sleep(300L);
					//往回发
					backSendMsg(keys, entry.getValue());
				}
				redisTemplate.acknowledge(RedisConstant.StreamMsgConstant.XMS_DEAD_MSG, entries);
				redisTemplate.delete(entries);
			}
		}
	}

	private void backSendMsg(String[] keys, Object messageBody) {
		while (true) {
			RecordId result = redisTemplate.send(keys[0], keys[1], messageBody);
			log.info("result 结果：{}", result.getTimestamp());
			if (Func.isNotEmpty(result.getTimestamp())) {
				return;
			}
			ThreadUtil.sleep(300L);
		}
	}

	@Override
	public void dealSysLogs(Integer days) {
		//默认60
		if (days == null) {
			days = 60;
		}
		//清空调度日志
		List<Long> ids = jobLogMapper.listSysLogsByDays(days);
		if (ids.isEmpty()) {
			return;
		}
		Long[] logIds = ids.toArray(new Long[0]);
		jobLogMapper.deleteJobLogByIds(logIds);
		//  BY RENEGADE PISTA: 2023/9/23  操作日志，登陆日志，目前不算多，可直接清楚days+30天前的数据
		ids = sysOperLogMapper.listOperLogByDays(days * 3);
		if (ids.isEmpty()) {
			return;
		}
		Long[] operLogIds = ids.toArray(new Long[0]);
		sysOperLogMapper.deleteOperLogByIds(operLogIds);
		ids = sysLogininforMapper.listLoginLogByDays(days * 3);
		if (!ids.isEmpty()) {
			Long[] idsLong = ids.toArray(new Long[0]);
			sysLogininforMapper.deleteLogininforByIds(idsLong);
		}
	}

	/**
	 * 查询没有处理的矿机订单
	 */
	@Override
	public void processOverdueMiningOrders() {
		// 获取当前时间减去5分钟的时间点
		Date fiveMinutesBefore = DateUtil.offsetMinute(new Date(), -5);
		List<MiningPackageOrder> miningOrderList = miningPackageOrderService.lambdaQuery()
			.eq(MiningPackageOrder::getBizStatus, 0)
			// 过期时间 + 5 分钟 <= 当前
			.lt(MiningPackageOrder::getCreateTime, fiveMinutesBefore)
			.list();
		if(CollectionUtil.isNotEmpty(miningOrderList)){
			for (MiningPackageOrder packageOrder : miningOrderList) {
					List<OrderMsgDO> orderMsgDOList = new ArrayList<>();
					OrderMsgDO orderMsgDO = new OrderMsgDO();
				orderMsgDO.setId(packageOrder.getId());
					orderMsgDO.setBizType(1);
						orderMsgDOList.add(orderMsgDO);
						asyncDynamicOrderSettlementServiceImpl.sendMessage(orderMsgDOList);
			}
		}

	}


	@Override
	public void task103Handler() {
/*		List<DestroyOrder> destroyOrderList = destroyOrderService.lambdaQuery()
			.eq(DestroyOrder::getPayStatus, 1)
			.eq(DestroyOrder::getBizStatus, 0)
			.list();
		if (CollectionUtil.isNotEmpty(destroyOrderList)) {
			for (DestroyOrder destroyOrder : destroyOrderList) {
				List<OrderMsgDO> orderMsgDOList = new ArrayList<>();
				OrderMsgDO orderMsgDO = new OrderMsgDO();
				orderMsgDO.setId(destroyOrder.getId());
				orderMsgDO.setBizType(1);
				orderMsgDOList.add(orderMsgDO);
				RecordId res = streamTemplate.send(RedisConstant.StreamMsgConstant.ORDER_DYNAMIC_SETTLEMENT, IdUtil.getSnowflakeNextIdStr(), JsonUtil.toJsonAsBytes(orderMsgDOList));
				if (res == null || Func.isAllEmpty(res.getTimestamp())) {
					log.error("执行");
					throw new ServiceException("挖矿订单之后的用户升级处理更新失败");
				}
			}
		}*/
	}

	@Override
	public void task102Handler() {
	}

	/**
	 * 定时拉取ido订单
	 */
	@Override
	public void getIdoOrder(Integer date) {
		List<UserInfo> userInfos = userInfoService.lambdaQuery()
			.orderByDesc(UserInfo::getUserId)
			.list();
		List<UserLevelConfig> userLevelConfigList = userLevelConfigService.lambdaQuery()
			.gt(UserLevelConfig::getLevel,0)
			.orderByAsc(UserLevelConfig::getLevel)
			.list();
		for (UserInfo userInfo : userInfos) {
			List<Long> parentIds = userInfo.getParentIds();
			parentIds.add(userInfo.getUserId());
			List<UserInfo> parentUserInfoList = userInfoService.lambdaQuery()
				.in(UserInfo::getUserId, parentIds)
				.orderByDesc(UserInfo::getUserId)
				.list();
			log.info("正在计算用户{}的等级", userInfo.getUserId());
			for (UserInfo info : parentUserInfoList) {
				callUserLevel(info, userLevelConfigList);
			}
		}
	}


	/**
	 * 计算用户等级
	 * @param info
	 * @param userLevelConfigList
	 */
	private  void callUserLevel(UserInfo info, List<UserLevelConfig> userLevelConfigList) {
		Integer origLevel = info.getGameLevel();
		Integer currentLevel = 0;

		// 计算直推线数量
		List<UserInfo> directUserList = userInfoService.lambdaQuery()
			.eq(UserInfo::getInviteUserId, info.getUserId())
			.select(UserInfo::getUserId,UserInfo::getGameLevel,UserInfo::getPerformance,UserInfo::getUmbrellaPerformance)
			.list();

		Map<Long, List<UserInfo>> directLineUserMap = new HashMap<>();
		for (UserInfo childUser : directUserList) {
			List<UserInfo> childUserList = userInfoService.getChildUserList(childUser.getUserId());
			List<UserInfo> directLineUsers = childUserList == null ? new ArrayList<>() : new ArrayList<>(childUserList);
			// 该线包含直推本人
			directLineUsers.add(childUser);
			directLineUserMap.put(childUser.getUserId(), directLineUsers);
		}

		//现在计算大区业绩
		info.setUmbrellaPerformance(UserInfoServiceImpl.getMaxTeamPerformance(directUserList));

		for (UserLevelConfig userLevelConfig : userLevelConfigList) {
			// 团队业绩与小区业绩满足档位条件
			if(info.getUmbrellaPerformance().compareTo(userLevelConfig.getTeamPerformance())>=0
				&& info.getCommunityPerformance().compareTo(userLevelConfig.getCommunityPerformance())>=0){
				if(userLevelConfig.getLevel()>1){
					if(CollectionUtil.isEmpty(directUserList) || directUserList.size()<userLevelConfig.getRequiredLegNum()){
						log.info("计算用户等级 伞下人不够 userId:{}, directUserListSize:{}, levelConfig:{}",info.getUserId(),directUserList.size(),
							userLevelConfig);
						continue;
					}

					Integer legLevelMin = userLevelConfig.getLegLevelMin();
					Integer legLevelCount = userLevelConfig.getLegLevelCount();
					int hitLines = 0;
					// 逐条线统计是否满足“该等级人数”要求
					for (UserInfo childUser : directUserList) {
						List<UserInfo> childUserList = directLineUserMap.get(childUser.getUserId());
						if (childUserList == null) {
							childUserList = Collections.singletonList(childUser);
						}
//						List<UserInfo> childUserList = userInfoService.getChildUserList(childUser.getUserId());
//						if (childUserList == null) {
//							childUserList = new ArrayList<>();
//						}
//						// 该线包含直推本人
//						childUserList.add(childUser);
						long matchCount = childUserList.stream()
							.filter(u -> u.getGameLevel() != null && legLevelMin != null && u.getGameLevel() >= legLevelMin)
							.count();
						// 该线满足人数要求则计为命中
						if (legLevelCount == null || matchCount >= legLevelCount) {
							hitLines++;
						}
						// 命中线数已满足要求，提前结束
						if (hitLines >= userLevelConfig.getRequiredLegNum()) {
							break;
						}
					}
					// 命中线数不足，跳过该档位
					if (hitLines < userLevelConfig.getRequiredLegNum()) {
						log.info("计算用户等级 命中线数不足 userId:{}, directUserListSize:{}, levelConfig:{},hitLines:{}",info.getUserId(),directUserList.size(),
							userLevelConfig,hitLines);
						continue;
					}
				}
				// 记录当前满足的最高档位
				currentLevel = userLevelConfig.getLevel();
			}
		}
		if(currentLevel !=origLevel){
			log.info("计算用户等级 修改等级 userId:{}, origLevel:{}, currentLevel:{}",info.getUserId(),origLevel,currentLevel);

			boolean update = userInfoService.lambdaUpdate()
				.eq(UserInfo::getUserId, info.getUserId())
				.set(UserInfo::getGameLevel, currentLevel)
				.update();
			if(!update){
				throw new ServiceException("更新用户等级信息失败");
			}
		}
	}

	/**
	 * 任务类型102 v9节点均分提现手续费分红任务
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void distributePtbInterest102(Integer parDate) {
	}


	@Override
	public void taskMsgCycle() {
		//获取当前时间时间的减去29L
		long epochMilli = Instant.now().toEpochMilli() - 300000L;
		List<MqTransactionLog> tMqTransactionLogs = mqTransactionLogServiceImpl.lambdaQuery().lt(MqTransactionLog::getCreateTime, epochMilli).list();
		if (tMqTransactionLogs.isEmpty()) {
			return;
		}
		for (MqTransactionLog tMqTransactionLog : tMqTransactionLogs) {
			byte[] contens = tMqTransactionLog.getLog();
			MqMsgDO sendMqDo = JsonUtil.readValue(contens, MqMsgDO.class);
			sendMqDo.setTransactionId(tMqTransactionLog.getId().toString());
			redisTemplate.send(sendMqDo.getTopic(), sendMqDo.getTransactionId(), JsonUtil.toJsonAsBytes(sendMqDo.getBody()));
			log.warn("{} 重新发送 发送订单ok ", sendMqDo.getBody());

		}
	}


	/**
	 * 任务类型101 每天发放质押奖励
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void distributePtbInterest101() {
		String strDate = DateUtil.format(DateUtil.date(), "yyyyMMdd");
		long currentDate = Long.parseLong(strDate);

		Map<String, Object> task = getTask(SysConstant.TSK_TYPE_101, currentDate + "");
		if (!CollectionUtil.isEmpty(task)) {
			log.debug("stake reward task already exists, skip");
			return;
		}

		stakeReleaseBucketService.lambdaUpdate()
			.eq(StakeReleaseBucket::getStatus, 0)
			.gt(StakeReleaseBucket::getHaveDays, 0)
			.setSql("have_days = have_days - 1")
			.update();

		List<StakeReleaseBucket> releaseBucketList = stakeReleaseBucketService.lambdaQuery()
			.eq(StakeReleaseBucket::getStatus, 0)
			.list();
		if (CollectionUtil.isNotEmpty(releaseBucketList)) {
			doReleaseBucketList(releaseBucketList);
		}

		stakeOrderService.lambdaUpdate()
			.eq(StakeOrder::getStatus, 0)
			.gt(StakeOrder::getHaveDays, 0)
			.setSql("have_days = have_days - 1 ")
			.update();

		List<StakeOrder> stakeOrderList = stakeOrderService.lambdaQuery()
			.eq(StakeOrder::getStatus, 0)
			.list();
		if (CollectionUtil.isNotEmpty(stakeOrderList)) {
			Integer currentDateInt = Integer.parseInt(strDate);
			List<RewardRecord> rewardRecordList = new ArrayList<>(stakeOrderList.size());
			List<UserMoney> userMoneyValidNum3List = new ArrayList<>(stakeOrderList.size() > 1000 ? 1000 : stakeOrderList.size());
			List<UserMoney> userMoneyValidNum5List = new ArrayList<>(stakeOrderList.size() > 1000 ? 1000 : stakeOrderList.size());
			List<Long> finishStakeOrderIds = new ArrayList<>();
			List<StakeOrder> refundStakeOrderList = new ArrayList<>();
			List<StakeOrder> updateStakeOrderList = new ArrayList<>(stakeOrderList.size());
			int batchSize = 1000;
			int stakeCount = 0;
			List<StakeReleaseBucket> bucketList = new ArrayList<>(stakeOrderList.size());

			for (StakeOrder stakeOrder : stakeOrderList) {
				if (stakeOrder.getHaveDays() == 0) {
					finishStakeOrderIds.add(stakeOrder.getId());
					if (Integer.valueOf(ConstantType.user_money_coin_type.type_2).equals(stakeOrder.getCoinType())
						&& !Integer.valueOf(1).equals(stakeOrder.getPrincipalRefundStatus())) {
						refundStakeOrderList.add(stakeOrder);
					}
				}

				Integer rewardCoinType = stakeOrder.getRewardCoinType() == null
					? ConstantType.user_money_coin_type.type_3
					: stakeOrder.getRewardCoinType();
				// 质押比例按下单时订单快照执行；OORT不在后台页面配置，但可通过数据库产品配置影响新订单。
				BigDecimal immediateReleasePercent = stakeOrder.getImmediateRatio() != null
					? stakeOrder.getImmediateRatio()
					: STAKE_IMMEDIATE_RATIO;
				BigDecimal linearReleasePercent = stakeOrder.getLinearRatio() != null
					? stakeOrder.getLinearRatio()
					: STAKE_LINEAR_RATIO;
				Integer linearDays = stakeOrder.getLinearDays() == null || stakeOrder.getLinearDays() <= 0
					? 270
					: stakeOrder.getLinearDays();
				BigDecimal currentYielded = stakeOrder.getYieldedAmount() == null
					? BigDecimal.ZERO
					: stakeOrder.getYieldedAmount();
				BigDecimal dayReward = stakeOrder.getDayReward() == null ? BigDecimal.ZERO : stakeOrder.getDayReward();

				StakeOrder updateOrder = new StakeOrder();
				updateOrder.setId(stakeOrder.getId());
				updateOrder.setYieldedAmount(currentYielded.add(dayReward));
				updateOrder.setUpdateTime(new Date());
				updateStakeOrderList.add(updateOrder);

				BigDecimal useReward = dayReward.multiply(immediateReleasePercent)
					.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew)
					.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
				BigDecimal releaseBucket = dayReward.multiply(linearReleasePercent)
					.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew)
					.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);

				if (useReward.compareTo(BigDecimal.ZERO) > 0) {
					RewardRecord rewardRecord = new RewardRecord();
					rewardRecord.setOrderCode(IDUtils.getSnowflakeStr());
					rewardRecord.setUserId(stakeOrder.getUserId());
					rewardRecord.setAmount(useReward);
					rewardRecord.setCoinType(rewardCoinType);
					rewardRecord.setSourceType(getStakeImmediateRewardSourceType(rewardCoinType));
					rewardRecord.setSourceOrderCode(stakeOrder.getOrderNo());
					rewardRecord.setSourceUserId(stakeOrder.getUserId());
					rewardRecord.setCreateTime(new Date());
					rewardRecordList.add(rewardRecord);

					UserMoney entity = new UserMoney();
					entity.setId(stakeOrder.getUserId());
					setRewardMoneyField(entity, rewardCoinType, useReward);
					entity.setGtId(IDUtils.getSnowflakeStr());
					entity.setSourceCode(stakeOrder.getOrderNo());
					entity.setSourceId(stakeOrder.getUserId());
					entity.setSourceType(getStakeImmediateMoneySourceType(rewardCoinType));
					entity.setUpdateTime(new Date());
					addRewardMoney(userMoneyValidNum3List, userMoneyValidNum5List, rewardCoinType, entity);
					stakeCount++;
					if (stakeCount >= batchSize) {
						flushStakeRewardMoney(userMoneyValidNum3List, userMoneyValidNum5List);
						log.info("stake immediate reward wallet updated");
						stakeCount = 0;
					}
				}

				if (releaseBucket.compareTo(BigDecimal.ZERO) > 0) {
					StakeReleaseBucket bucket = new StakeReleaseBucket();
					bucket.setUserId(stakeOrder.getUserId());
					bucket.setCoinType(rewardCoinType);
					bucket.setOrderNo(IDUtils.getSnowflakeStr());
					bucket.setLinearDays(linearDays);
					bucket.setHaveDays(linearDays);
					bucket.setTotalAmount(releaseBucket);
					bucket.setDailyReleaseAmount(releaseBucket.divide(new BigDecimal(linearDays), ConstantStatic.newScale, ConstantStatic.roundingModeNew));
					bucket.setRemainingAmount(releaseBucket);
					bucket.setStatus(0);
					bucket.setStartTime(currentDateInt);
					bucket.setSourceSnapshot("[{\"orderNo\":\"" + stakeOrder.getOrderNo()
						+ "\",\"orderRemainDays\":" + stakeOrder.getHaveDays()
						+ ",\"todayLinearAmount\":\"" + releaseBucket.stripTrailingZeros().toPlainString() + "\"}]");
					bucket.setCreateTime(new Date());
					bucketList.add(bucket);
				}
			}

			if (CollectionUtil.isNotEmpty(rewardRecordList)) {
				rewardRecordService.saveBatch(rewardRecordList);
			}
			flushStakeRewardMoney(userMoneyValidNum3List, userMoneyValidNum5List);
			if (CollectionUtil.isNotEmpty(bucketList)) {
				stakeReleaseBucketService.saveBatch(bucketList);
			}
			if (CollectionUtil.isNotEmpty(updateStakeOrderList)) {
				stakeOrderService.updateBatchById(updateStakeOrderList);
			}
			if (CollectionUtil.isNotEmpty(finishStakeOrderIds)) {
				stakeOrderService.lambdaUpdate()
					.in(StakeOrder::getId, finishStakeOrderIds)
					.set(StakeOrder::getStatus, 1)
					.update();
			}
			refundDfcStakePrincipal(refundStakeOrderList);
		}

		int i = addTask(SysConstant.TSK_TYPE_101, currentDate + "");
		if (i != 1) {
			throw new RuntimeException("add task type 101 failed");
		}
	}

	private void doReleaseBucketList(List<StakeReleaseBucket> releaseBucketList) {
		List<RewardRecord> rewardRecordList = new ArrayList<>(releaseBucketList.size());
		List<UserMoney> userMoneyValidNum3List = new ArrayList<>(releaseBucketList.size() > 1000 ? 1000 : releaseBucketList.size());
		List<UserMoney> userMoneyValidNum5List = new ArrayList<>(releaseBucketList.size() > 1000 ? 1000 : releaseBucketList.size());
		List<StakeReleaseBucket> updateBucketList = new ArrayList<>(releaseBucketList.size());
		List<Long> finishBucketIds = new ArrayList<>();
		int batchSize = 1000;
		int stakeCount = 0;
		for (StakeReleaseBucket stakeReleaseBucket : releaseBucketList) {
			BigDecimal dailyReleaseAmount = stakeReleaseBucket.getDailyReleaseAmount();
			if (dailyReleaseAmount == null || dailyReleaseAmount.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			Integer rewardCoinType = stakeReleaseBucket.getCoinType() == null
				? ConstantType.user_money_coin_type.type_3
				: stakeReleaseBucket.getCoinType();
			Integer haveDays = stakeReleaseBucket.getHaveDays();
			BigDecimal remainingAmount = stakeReleaseBucket.getRemainingAmount() == null
				? BigDecimal.ZERO
				: stakeReleaseBucket.getRemainingAmount();
			if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
				finishBucketIds.add(stakeReleaseBucket.getId());
				continue;
			}
			BigDecimal releaseAmount = haveDays == 0
				? remainingAmount
				: dailyReleaseAmount.min(remainingAmount);
			if (releaseAmount.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			stakeReleaseBucket.setRemainingAmount(remainingAmount.subtract(releaseAmount));
			stakeReleaseBucket.setUpdateTime(new Date());
			updateBucketList.add(stakeReleaseBucket);
			if (stakeReleaseBucket.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
				finishBucketIds.add(stakeReleaseBucket.getId());
			}

			RewardRecord rewardRecord = new RewardRecord();
			rewardRecord.setOrderCode(IDUtils.getSnowflakeStr());
			rewardRecord.setUserId(stakeReleaseBucket.getUserId());
			rewardRecord.setAmount(releaseAmount);
			rewardRecord.setCoinType(rewardCoinType);
			rewardRecord.setSourceType(getStakeLinearRewardSourceType(rewardCoinType));
			rewardRecord.setSourceOrderCode(stakeReleaseBucket.getOrderNo());
			rewardRecord.setSourceUserId(stakeReleaseBucket.getUserId());
			rewardRecord.setCreateTime(new Date());
			rewardRecordList.add(rewardRecord);

			UserMoney entity = new UserMoney();
			entity.setId(stakeReleaseBucket.getUserId());
			setRewardMoneyField(entity, rewardCoinType, releaseAmount);
			entity.setGtId(IDUtils.getSnowflakeStr());
			entity.setSourceCode(stakeReleaseBucket.getOrderNo());
			entity.setSourceId(stakeReleaseBucket.getUserId());
			entity.setSourceType(getStakeLinearMoneySourceType(rewardCoinType));
			entity.setUpdateTime(new Date());
			addRewardMoney(userMoneyValidNum3List, userMoneyValidNum5List, rewardCoinType, entity);
			stakeCount++;
			if (stakeCount >= batchSize) {
				flushStakeRewardMoney(userMoneyValidNum3List, userMoneyValidNum5List);
				log.info("stake linear reward wallet updated");
				stakeCount = 0;
			}
		}

		if (CollectionUtil.isNotEmpty(rewardRecordList)) {
			rewardRecordService.saveBatch(rewardRecordList);
		}
		flushStakeRewardMoney(userMoneyValidNum3List, userMoneyValidNum5List);
		if (CollectionUtil.isNotEmpty(updateBucketList)) {
			stakeReleaseBucketService.updateBatchById(updateBucketList);
		}
		if (CollectionUtil.isNotEmpty(finishBucketIds)) {
			stakeReleaseBucketService.lambdaUpdate()
				.in(StakeReleaseBucket::getId, finishBucketIds)
				.eq(StakeReleaseBucket::getStatus, 0)
				.set(StakeReleaseBucket::getStatus, 1)
				.update();
		}
	}

	private void setRewardMoneyField(UserMoney entity, Integer coinType, BigDecimal amount) {
		// DFC质押产出使用“产出DFC”(valid_num5)；OORT产出保持原钱包字段(valid_num3)。
		if (Integer.valueOf(ConstantType.user_money_coin_type.type_5).equals(coinType)) {
			entity.setValidNum5(amount);
		} else {
			entity.setValidNum3(amount);
		}
	}

	private void addRewardMoney(List<UserMoney> validNum3List, List<UserMoney> validNum5List, Integer coinType, UserMoney entity) {
		if (Integer.valueOf(ConstantType.user_money_coin_type.type_5).equals(coinType)) {
			validNum5List.add(entity);
		} else {
			validNum3List.add(entity);
		}
	}

	private void flushStakeRewardMoney(List<UserMoney> validNum3List, List<UserMoney> validNum5List) {
		if (CollectionUtil.isNotEmpty(validNum3List)) {
			bachUpdateMoneyValid3(validNum3List);
			validNum3List.clear();
		}
		if (CollectionUtil.isNotEmpty(validNum5List)) {
			bachUpdateMoneyValid5(validNum5List);
			validNum5List.clear();
		}
	}

	private int getStakeImmediateRewardSourceType(Integer rewardCoinType) {
		return Integer.valueOf(ConstantType.user_money_coin_type.type_5).equals(rewardCoinType)
			? ConstantType.xms_reward_record_source_type.type_27
			: ConstantType.xms_reward_record_source_type.type_8;
	}

	private int getStakeLinearRewardSourceType(Integer rewardCoinType) {
		return Integer.valueOf(ConstantType.user_money_coin_type.type_5).equals(rewardCoinType)
			? ConstantType.xms_reward_record_source_type.type_28
			: ConstantType.xms_reward_record_source_type.type_9;
	}

	private int getStakeImmediateMoneySourceType(Integer rewardCoinType) {
		return Integer.valueOf(ConstantType.user_money_coin_type.type_5).equals(rewardCoinType)
			? ConstantType.user_money_log_source_type.type_34
			: ConstantType.user_money_log_source_type.type_22;
	}

	private int getStakeLinearMoneySourceType(Integer rewardCoinType) {
		return Integer.valueOf(ConstantType.user_money_coin_type.type_5).equals(rewardCoinType)
			? ConstantType.user_money_log_source_type.type_35
			: ConstantType.user_money_log_source_type.type_23;
	}

	private void refundDfcStakePrincipal(List<StakeOrder> stakeOrderList) {
		if (CollectionUtil.isEmpty(stakeOrderList)) {
			return;
		}
		List<RewardRecord> refundRecordList = new ArrayList<>(stakeOrderList.size());
		for (StakeOrder stakeOrder : stakeOrderList) {
			BigDecimal stakeAmount = stakeOrder.getStakeAmount() == null ? BigDecimal.ZERO : stakeOrder.getStakeAmount();
			if (stakeAmount.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			boolean locked = stakeOrderService.lambdaUpdate()
				.eq(StakeOrder::getId, stakeOrder.getId())
				.eq(StakeOrder::getPrincipalRefundStatus, 0)
				.eq(StakeOrder::getCoinType, ConstantType.user_money_coin_type.type_2)
				.set(StakeOrder::getPrincipalRefundStatus, 1)
				.set(StakeOrder::getPrincipalRefundTime, new Date())
				.update();
			if (!locked) {
				continue;
			}
			// DFC质押本金到期退回可用DFC(valid_num2)，不进入产出DFC(valid_num5)。
			int count = userWalletServiceImpl.handerUserMoney(stakeAmount, stakeOrder.getOrderNo(), stakeOrder.getUserId(), stakeOrder.getUserId(),
				ConstantType.user_money_log_source_type.type_36, ConstantType.user_money_coin_type.type_2);
			if (count != 1) {
					throw new ServiceException("DFC stake principal refund failed");
			}
			RewardRecord rewardRecord = new RewardRecord();
			rewardRecord.setOrderCode(IDUtils.getSnowflakeStr());
			rewardRecord.setUserId(stakeOrder.getUserId());
			rewardRecord.setAmount(stakeAmount);
			rewardRecord.setCoinType(ConstantType.user_money_coin_type.type_2);
			rewardRecord.setSourceType(ConstantType.xms_reward_record_source_type.type_29);
			rewardRecord.setSourceOrderCode(stakeOrder.getOrderNo());
			rewardRecord.setSourceUserId(stakeOrder.getUserId());
			rewardRecord.setCreateTime(new Date());
			refundRecordList.add(rewardRecord);
		}
		if (CollectionUtil.isNotEmpty(refundRecordList)) {
			rewardRecordService.saveBatch(refundRecordList);
		}
	}



	private void addInteractRewardRecord(List<RewardRecord> recordList, Long receiveUserId, Long sourceUserId,
										 BigDecimal reward, int sourceType, BigDecimal boomaiPrice) {
		if (reward == null || reward.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}
		RewardRecord rr = new RewardRecord();
		rr.setUserId(receiveUserId);
		rr.setAmount(reward);
		rr.setBusinessType(ConstantType.xms_reward_record_business_type.type_2);
		rr.setSourceType(sourceType);
		rr.setSourceOrderCode(IDUtils.getSnowflakeStr());
		rr.setOrderCode(IDUtils.getSnowflakeStr());
		rr.setSourceUserId(sourceUserId);
		rr.setRealTimePrice(boomaiPrice);
		rr.setCreateTime(new Date());
		recordList.add(rr);
	}

	private void saveInteractRewardRecords(List<RewardRecord> recordList) {
		if (CollectionUtil.isEmpty(recordList)) {
			return;
		}
		rewardRecordService.saveBatch(recordList);
	}







	/**
	 * 每日矿发放矿机奖励
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void distributePtbInterest100() {
		String strDate = DateUtil.format(DateUtil.date(), "yyyyMMdd");
		//long类型日期
		long currentDate = Long.parseLong(strDate);

		Map<String, Object> task = getTask(SysConstant.TSK_TYPE_100, currentDate + "");
		if (!CollectionUtil.isEmpty(task)) {
			log.debug("每日矿发放矿机奖励任务已存在跳过");
			return;
		}

		//增加对应天数
		miningPackageOrderService.lambdaUpdate()
			.eq(MiningPackageOrder::getStatus, 2)
			.setSql("run_days = run_days + 1")
			.update();

		List<MiningPackageOrder> packageOrderList = miningPackageOrderService.lambdaQuery()
			.eq(MiningPackageOrder::getStatus, 2)
			.list();
		if(CollectionUtil.isNotEmpty(packageOrderList)){

			//0.需求问题要 重新渲染一下每日收益值。如果原本的数据值<=0就去减产规则的值
			List<MiningPackageTier> tierList = miningPackageTierService.lambdaQuery()
				.orderByAsc(MiningPackageTier::getStartIndex)
				.list();
			for (MiningPackageOrder beforeOrder : packageOrderList) {
				if(beforeOrder.getDayReward().compareTo(BigDecimal.ZERO)<=0){
					beforeOrder.setDayReward(matchMiningPackageTier(tierList, beforeOrder.getId().intValue()));
				}
			}

			BigDecimal miningRatio = new BigDecimal("80")
				.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew);
			// 汇总每个用户的日收益（同一用户可能有多条订单）
			Map<Long, BigDecimal> userDailyRewardMap = new HashMap<>();
			//管理费
			Map<Long, BigDecimal> userDailyRewardMap1 = new HashMap<>();

			List<RewardRecord> rewardRecordList = new ArrayList<>(packageOrderList.size());
			RewardRecord rewardRecordEntity = null;
			List<UserMoney> userMoneyValidNum1List = new ArrayList<>(packageOrderList.size()>1000?1000:packageOrderList.size());
			List<MiningPackageOrder> updateTotalRewardList = new ArrayList<>(packageOrderList.size()>1000?1000:packageOrderList.size());
			int batchSize = 1000;
			int stakeCount1 = 0;
			int updateTotalRewardCount = 0;
			UserMoney entity = null;
			for (MiningPackageOrder packageOrder : packageOrderList) {
				BigDecimal dayReward = packageOrder.getDayReward();
				if (dayReward == null || dayReward.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}
				//累计收益 = 原累计 + 当日收益
				MiningPackageOrder updateOrder = new MiningPackageOrder();
				updateOrder.setId(packageOrder.getId());
				BigDecimal totalReward = packageOrder.getTotalReward() == null ? BigDecimal.ZERO : packageOrder.getTotalReward();
				updateOrder.setTotalReward(totalReward.add(dayReward));
				updateTotalRewardList.add(updateOrder);
				updateTotalRewardCount++;
				if (updateTotalRewardCount >= batchSize) {
					miningPackageOrderService.updateBatchById(updateTotalRewardList);
					updateTotalRewardList.clear();
					updateTotalRewardCount = 0;
				}
				BigDecimal userReward = packageOrder.getDayReward().multiply(miningRatio)
					.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);

				rewardRecordEntity = new RewardRecord();
				rewardRecordEntity.setUserId(packageOrder.getUserId());
				rewardRecordEntity.setAmount(userReward);
				rewardRecordEntity.setCoinType(ConstantType.user_money_coin_type.type_5);
				rewardRecordEntity.setSourceType(ConstantType.xms_reward_record_source_type.type_1);
				rewardRecordEntity.setSourceOrderCode(packageOrder.getOrderNo());
				rewardRecordEntity.setOrderCode(IDUtils.getSnowflakeStr());
				rewardRecordEntity.setSourceUserId(packageOrder.getUserId());
				rewardRecordEntity.setCreateTime(new Date());
				rewardRecordEntity.setManageReward(packageOrder.getDayReward());
				rewardRecordList.add(rewardRecordEntity);
				userDailyRewardMap.merge(packageOrder.getUserId(), userReward, BigDecimal::add);

				//钱包流水
				entity = new UserMoney();
				entity.setId(packageOrder.getUserId());
				entity.setValidNum5(userReward);
				entity.setGtId(IDUtils.getSnowflakeStr());
				entity.setSourceCode(packageOrder.getOrderNo());
				entity.setSourceId(packageOrder.getUserId());
				entity.setSourceType(ConstantType.user_money_log_source_type.type_16);
				entity.setUpdateTime(new Date());
				userMoneyValidNum1List.add(entity);
				stakeCount1++;
				if (stakeCount1 >= batchSize) {
					//静态产出发的是产出DFC奖励
					bachUpdateMoneyValid5(userMoneyValidNum1List);
					userMoneyValidNum1List.clear();
					log.info("更新成功");
					stakeCount1 = 0;
				}

				//管理费大于0
				if(packageOrder.getDayReward().compareTo(BigDecimal.ZERO)>0){
					userDailyRewardMap1.merge(packageOrder.getUserId(), packageOrder.getDayReward(), BigDecimal::add);
				}
			}

			//用户的挖矿奖励奖金记录
			if (CollectionUtil.isNotEmpty(rewardRecordList)) {
				rewardRecordService.saveBatch(rewardRecordList);
			}
			if (CollectionUtil.isNotEmpty(updateTotalRewardList)) {
				miningPackageOrderService.updateBatchById(updateTotalRewardList);
			}
			//用户的挖矿钱包流水
			if (CollectionUtil.isNotEmpty(userMoneyValidNum1List)) {
				//静态产出发的是产出DFC奖励
				bachUpdateMoneyValid5(userMoneyValidNum1List);
			}

			//todo 管理费分配
			MiningMgmtFeeConfig miningMgmtFeeConfig = miningMgmtFeeConfigService.lambdaQuery()
				.last("limit 1")
				.one();

			//对userDailyRewardMap1里面元素求和
			BigDecimal totalManageReward = userDailyRewardMap1.values().stream()
				.reduce(BigDecimal.ZERO, BigDecimal::add);
			if(totalManageReward.compareTo(BigDecimal.ZERO)>0){
				//市场代理费分佣
				Set<Long> userIds = packageOrderList.stream().map(MiningPackageOrder::getUserId)
					.collect(Collectors.toSet());
				List<UserInfo> userInfoList = userInfoService.lambdaQuery()
					.in(UserInfo::getUserId, userIds)
					.select(UserInfo::getUserId, UserInfo::getParentChain, UserInfo::getInviteUserId)
					.list();
				Map<Long,List<ParentUserTaskVo>> parentChainMap = new HashMap<>();
				for (UserInfo userInfo : userInfoList) {
					parentChainMap.put(userInfo.getUserId(), userInfoService.getParentUserTaskVo(userInfo.getUserId()));
				}

				//市场代理费分配
				distributeMarketAgentDiffReward(rewardRecordList, parentChainMap,miningMgmtFeeConfig);
				//发放直推、间推奖励
				calculateInviteReward(parentChainMap, rewardRecordList, miningMgmtFeeConfig);
				//计算服务中心费
				calculateServiceCenterFee(totalManageReward, miningMgmtFeeConfig, packageOrderList);
				//平台管理费比例
				calculateMgmtFee(miningMgmtFeeConfig, totalManageReward);
			}else{
				log.info("管理费为0, userDailyRewardMap1:{}", userDailyRewardMap1);
			}
		}

		//先扣减质押启动期剩余天数
		miningPackageOrderService.lambdaUpdate()
			.eq(MiningPackageOrder::getStatus, 1)
			.gt(MiningPackageOrder::getStakeStartupRemainingDays,0)
			.setSql("stake_startup_remaining_days = stake_startup_remaining_days - 1")
			.update();

		//如果天数小于等于0变为释放中
		miningPackageOrderService.lambdaUpdate()
			.eq(MiningPackageOrder::getStatus, 1)
			.le(MiningPackageOrder::getStakeStartupRemainingDays,0)
			.set(MiningPackageOrder::getStatus, 2)
			.update();
		//找到释放中的订单释放收益
		//消费分红池分红任务
		int i = addTask(SysConstant.TSK_TYPE_100, currentDate + "");
		if (i != 1) {
			throw new RuntimeException("添加任务类型100:每日释放线性订单失败");
		}
	}

	/**
	 * 匹配质押档位
	 */
	private BigDecimal matchMiningPackageTier(List<MiningPackageTier> list, Integer miningNo) {
		if (list == null || list.isEmpty()) {
			throw new ServiceException("质押档位未配置");
		}
		for (MiningPackageTier tier : list) {
			Integer startIndex = tier.getStartIndex();
			Integer endIndex = tier.getEndIndex();
			if (startIndex == null || endIndex == null) {
				continue;
			}
			if (miningNo >= startIndex && miningNo <= endIndex) {
				return tier.getDayReward();
			}
		}
		return list.get(list.size() - 1).getDayReward();
	}

	/**
	 * 市场代理极差/平级奖励发放
	 */
	private  void distributeMarketAgentDiffReward(List<RewardRecord> rewardRecordList,
														Map<Long, List<ParentUserTaskVo>> parentChainMap,
														MiningMgmtFeeConfig miningMgmtFeeConfig) {
		RewardRecord rewardRecordEntity = null;
		List<RewardRecord> teamRewardRecordList = new ArrayList<>();
		List<UserMoney> userMoneyValidNum6List = new ArrayList<>(1000);
		UserMoney entity = null;
		int batchSize = 1000;
		int stakeCount1 = 0;
		Map<Integer,BigDecimal > levelConfigMap = new HashMap<>();
		// 代理等级 -> 比例（百分比转小数）
		levelConfigMap.put(0,BigDecimal.ZERO);
		levelConfigMap.put(1,miningMgmtFeeConfig.getAgentDiffAreaRatio()
			.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew));
		levelConfigMap.put(2,miningMgmtFeeConfig.getAgentDiffCountyRatio()
			.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew));
		levelConfigMap.put(3,miningMgmtFeeConfig.getAgentDiffCityRatio()
			.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew));
		levelConfigMap.put(4,miningMgmtFeeConfig.getAgentDiffProvinceRatio()
			.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew));
		levelConfigMap.put(5,miningMgmtFeeConfig.getAgentDiffNationalRatio()
			.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew));

		// 平级奖比例（只针对全国代理）
		BigDecimal nationalSameLevelRatio =miningMgmtFeeConfig.getNationalSameLevelRatio()
				.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew);

		for (RewardRecord rewardRecord : rewardRecordList) {
			// 管理费为 0 不参与分配
			if(rewardRecord.getManageReward().compareTo(BigDecimal.ZERO)<=0){
				continue;
			}

			if(parentChainMap.containsKey(rewardRecord.getUserId())){
				List<ParentUserTaskVo> parentUserTaskVos = parentChainMap.get(rewardRecord.getUserId());
				if(CollectionUtil.isNotEmpty(parentUserTaskVos)){
					// 已发放的最高比例（用于计算极差）
					BigDecimal initRewardRatio = BigDecimal.ZERO;
					// 本条链路是否已发放平级奖
					boolean peerRewardSent = false;
					// 上一个拿到级差奖的用户
					ParentUserTaskVo lastRewardUser = null;
					// 上一个级差奖金额
					BigDecimal lastRewardAmount = BigDecimal.ZERO;
					// 上一个拿奖用户的等级
					Integer lastRewardLevel = null;
					for (ParentUserTaskVo p : parentUserTaskVos) {
						p.setGameLevel(p.getGameLevel()>p.getMinGameLevel()?p.getGameLevel():p.getMinGameLevel());
						// 无效用户不参与
						if (p.getIsValid() == null || p.getIsValid() == 0) {
							continue;
						}

						rewardRecordEntity = new  RewardRecord();
						BigDecimal levelRatio = levelConfigMap.get(p.getGameLevel());
						if(levelRatio.compareTo(BigDecimal.ZERO)<=0){
							continue;
						}

						BigDecimal finalRewardRatio = levelRatio.subtract(initRewardRatio);
						// 1) 极差奖：用当前等级比例减去已发放比例
						if (finalRewardRatio.compareTo(BigDecimal.ZERO) > 0) {
							BigDecimal teamReward = rewardRecord.getManageReward().multiply(finalRewardRatio)
								.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
							if (teamReward.compareTo(BigDecimal.ZERO) > 0) {
								//插入奖金明细
								rewardRecordEntity.setOrderCode(IDUtils.getSnowflakeStr());
								rewardRecordEntity.setUserId(p.getUserId());
								rewardRecordEntity.setAmount(teamReward);
								rewardRecordEntity.setCoinType(ConstantType.user_money_coin_type.type_6);
								rewardRecordEntity.setSourceType(ConstantType.xms_reward_record_source_type.type_6);
								rewardRecordEntity.setSourceUserId(rewardRecord.getUserId());
								rewardRecordEntity.setSourceOrderCode(rewardRecord.getSourceOrderCode());
								rewardRecordEntity.setGtId(IDUtils.getSnowflakeStr());
								teamRewardRecordList.add(rewardRecordEntity);

								entity = new UserMoney();
								entity.setId(p.getUserId());
								entity.setValidNum6(teamReward);
								entity.setGtId(IDUtils.getSnowflakeStr());
								entity.setSourceCode(rewardRecord.getSourceOrderCode());
								entity.setSourceId(rewardRecord.getUserId());
								entity.setSourceType(ConstantType.user_money_log_source_type.type_20);
								entity.setUpdateTime(new Date());

								userMoneyValidNum6List.add(entity);
								stakeCount1++;
								if (stakeCount1 >= batchSize) {
									bachUpdateMoneyValid6(userMoneyValidNum6List);
									userMoneyValidNum6List.clear();
									log.info("更新成功");
									stakeCount1 = 0;
								}

								// 更新极差状态
								initRewardRatio = levelRatio;
								lastRewardUser = p;
								lastRewardAmount = teamReward;
								lastRewardLevel = p.getGameLevel();
							}
						continue;
					}

						// 2) 平级奖（仅全国代理，finalRewardRatio == 0）
						if (finalRewardRatio.compareTo(BigDecimal.ZERO) == 0
							&& p.getGameLevel() != null
							&& p.getGameLevel() == 5) {
							if (!peerRewardSent
								&& lastRewardUser != null
								&& lastRewardLevel != null
								&& lastRewardLevel.equals(p.getGameLevel())) {
								BigDecimal peerReward = lastRewardAmount.multiply(nationalSameLevelRatio)
						.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
								if (peerReward.compareTo(BigDecimal.ZERO) > 0) {
									//奖金明细
									rewardRecordEntity.setOrderCode(IDUtils.getSnowflakeStr());
									rewardRecordEntity.setUserId(p.getUserId());
									rewardRecordEntity.setAmount(peerReward);
									rewardRecordEntity.setCoinType(ConstantType.user_money_coin_type.type_6);
									rewardRecordEntity.setSourceType(ConstantType.xms_reward_record_source_type.type_7);
									rewardRecordEntity.setSourceUserId(rewardRecord.getUserId());
									rewardRecordEntity.setSourceOrderCode(rewardRecord.getSourceOrderCode());
									rewardRecordEntity.setGtId(IDUtils.getSnowflakeStr());
									teamRewardRecordList.add(rewardRecordEntity);

									entity = new UserMoney();
									entity.setId(p.getUserId());
									entity.setValidNum6(peerReward);
									entity.setGtId(IDUtils.getSnowflakeStr());
									entity.setSourceCode(rewardRecord.getSourceOrderCode());
									entity.setSourceId(rewardRecord.getUserId());
									entity.setSourceType(ConstantType.user_money_log_source_type.type_25);
									entity.setUpdateTime(new Date());

									userMoneyValidNum6List.add(entity);
									stakeCount1++;
									if (stakeCount1 >= batchSize) {
										bachUpdateMoneyValid6(userMoneyValidNum6List);
										userMoneyValidNum6List.clear();
										log.info("更新成功");
										stakeCount1 = 0;
									}
									peerRewardSent = true;
								}
							}
						}

						// 3) 超越奖（finalRewardRatio < 0）
//						if (finalRewardRatio.compareTo(BigDecimal.ZERO) < 0 && exceedAwardFlag) {
//
//						}

					}
				}
			}
		}

		// 用户的市场代理极差奖励流水记录
		if (CollectionUtil.isNotEmpty(userMoneyValidNum6List)) {
			bachUpdateMoneyValid6(userMoneyValidNum6List);
		}

		// 用户的市场代理极差奖励记录
		if (CollectionUtil.isNotEmpty(teamRewardRecordList)) {
			rewardRecordService.saveBatch(teamRewardRecordList);
		}
	}

	/**
	 * 发放直推、间推奖励(产出DFC)
	 * 基于订单用户的管理费，按配置比例给直推/间推发放奖励
	 * @param parentChainMap 用户上级链缓存（按距离从近到远）
	 * @param sourceRewardList 订单用户的管理费奖励记录
	 * @param miningMgmtFeeConfig 管理费配置（含直推/间推比例）
	 */
	private void calculateInviteReward(Map<Long, List<ParentUserTaskVo>> parentChainMap,
									   List<RewardRecord> sourceRewardList,
									   MiningMgmtFeeConfig miningMgmtFeeConfig) {
		// 直推/间推比例（百分比转小数）
		BigDecimal directPushRatio = miningMgmtFeeConfig.getDirectPushRatio()
				.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew);
		BigDecimal indirectPushRatio = miningMgmtFeeConfig.getIndirectPushRatio()
			.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew);

		// 生成奖励记录 + 钱包流水（分批写入）
		List<RewardRecord> rewardRecordList = new ArrayList<>(sourceRewardList.size()*2);
		RewardRecord rewardRecordEntity = null;
		List<UserMoney> userMoneyValidNum6List = new ArrayList<>(1000);
		int batchSize = 1000;
		int stakeCount1 = 0;
		UserMoney entity = null;
		for (RewardRecord rewardRecord : sourceRewardList) {
			// 管理费大于0才发放奖励
			if(rewardRecord.getManageReward().compareTo(BigDecimal.ZERO) <= 0){
				continue;
			}
			List<ParentUserTaskVo> parentUserTaskVos = parentChainMap.get(rewardRecord.getUserId());
			if(CollectionUtil.isEmpty(parentUserTaskVos)){
				continue;
			}

			ParentUserTaskVo directUser = null;
			ParentUserTaskVo indirectUser = null;
			for (ParentUserTaskVo parentUserTaskVo : parentUserTaskVos) {
				if (parentUserTaskVo.getIsValid() == null || parentUserTaskVo.getIsValid() == 0) {
					continue;
				}
				if (directUser == null) {
					directUser = parentUserTaskVo;
					continue;
				}
				indirectUser = parentUserTaskVo;
				break;
			}

			if(directUser != null && directPushRatio.compareTo(BigDecimal.ZERO)>0){
				BigDecimal directPushReward = directPushRatio.multiply(rewardRecord.getManageReward())
					.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
				if(directPushReward.compareTo(BigDecimal.ZERO)>0){
					rewardRecordEntity = new RewardRecord();
					rewardRecordEntity.setUserId(directUser.getUserId());
					rewardRecordEntity.setAmount(directPushReward);
					rewardRecordEntity.setCoinType(ConstantType.user_money_coin_type.type_6);
					rewardRecordEntity.setSourceType(ConstantType.xms_reward_record_source_type.type_4);
					rewardRecordEntity.setSourceOrderCode(rewardRecord.getSourceOrderCode());
					rewardRecordEntity.setOrderCode(IDUtils.getSnowflakeStr());
					rewardRecordEntity.setSourceUserId(rewardRecord.getUserId());
					rewardRecordEntity.setCreateTime(new Date());
					rewardRecordList.add(rewardRecordEntity);

					entity = new UserMoney();
					entity.setId(directUser.getUserId());
					entity.setValidNum6(directPushReward);
					entity.setGtId(IDUtils.getSnowflakeStr());
					entity.setSourceCode(rewardRecord.getSourceOrderCode());
					entity.setSourceId(rewardRecord.getUserId());
					entity.setSourceType(ConstantType.user_money_log_source_type.type_18);
					entity.setUpdateTime(new Date());
					userMoneyValidNum6List.add(entity);
					stakeCount1++;
					if (stakeCount1 >= batchSize) {
						bachUpdateMoneyValid6(userMoneyValidNum6List);
						userMoneyValidNum6List.clear();
						log.info("更新成功");
						stakeCount1 = 0;
					}
				}
			}

			if(indirectUser != null && indirectPushRatio.compareTo(BigDecimal.ZERO)>0){
				BigDecimal indirectPushReward = indirectPushRatio.multiply(rewardRecord.getManageReward())
					.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
				if(indirectPushReward.compareTo(BigDecimal.ZERO)>0){
					rewardRecordEntity = new RewardRecord();
					rewardRecordEntity.setUserId(indirectUser.getUserId());
					rewardRecordEntity.setAmount(indirectPushReward);
					rewardRecordEntity.setCoinType(ConstantType.user_money_coin_type.type_6);
					rewardRecordEntity.setSourceType(ConstantType.xms_reward_record_source_type.type_5);
					rewardRecordEntity.setSourceOrderCode(rewardRecord.getSourceOrderCode());
					rewardRecordEntity.setOrderCode(IDUtils.getSnowflakeStr());
					rewardRecordEntity.setSourceUserId(rewardRecord.getUserId());
					rewardRecordEntity.setCreateTime(new Date());
					rewardRecordList.add(rewardRecordEntity);

					entity = new UserMoney();
					entity.setId(indirectUser.getUserId());
					entity.setValidNum6(indirectPushReward);
					entity.setGtId(IDUtils.getSnowflakeStr());
					entity.setSourceCode(rewardRecord.getSourceOrderCode());
					entity.setSourceId(rewardRecord.getUserId());
					entity.setSourceType(ConstantType.user_money_log_source_type.type_19);
					entity.setUpdateTime(new Date());
					userMoneyValidNum6List.add(entity);
					stakeCount1++;
					if (stakeCount1 >= batchSize) {
						bachUpdateMoneyValid6(userMoneyValidNum6List);
						userMoneyValidNum6List.clear();
						log.info("更新成功");
						stakeCount1 = 0;
					}
				}
			}
		}
		// 用户的直推、间推管理费奖金记录
		if (CollectionUtil.isNotEmpty(rewardRecordList)) {
			rewardRecordService.saveBatch(rewardRecordList);
		}

		// 用户的直推、间推管理费钱包流水
		if (CollectionUtil.isNotEmpty(userMoneyValidNum6List)) {
			bachUpdateMoneyValid6(userMoneyValidNum6List);
		}
	}

	private void calculateMgmtFee(MiningMgmtFeeConfig miningMgmtFeeConfig, BigDecimal totalManageReward) {
		if(miningMgmtFeeConfig.getPlatformFeeRatio().compareTo(BigDecimal.ZERO)>0){
			//平台管理费
			BigDecimal dbPlatformFee = new BigDecimal(sysParaServiceImpl.getValue(ConstantSys.biz_platform_fee));
			BigDecimal platformFee = totalManageReward.multiply(miningMgmtFeeConfig.getPlatformFeeRatio())
				.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew)
				.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
			if(platformFee.compareTo(BigDecimal.ZERO)>0){
				BigDecimal finalPlatformFee = platformFee.add(dbPlatformFee);
				sysParaServiceImpl.updateSysParaByCode(ConstantSys.biz_platform_fee,finalPlatformFee.stripTrailingZeros().toPlainString());
				//添加奖金记录
				RewardRecord platformFeeRecord = new RewardRecord();
				platformFeeRecord.setUserId(0L);
				platformFeeRecord.setAmount(platformFee);
				platformFeeRecord.setCoinType(ConstantType.user_money_coin_type.type_2);
				platformFeeRecord.setSourceType(ConstantType.xms_reward_record_source_type.type_2);
				platformFeeRecord.setSourceOrderCode(IDUtils.getSnowflakeStr());
				platformFeeRecord.setOrderCode(IDUtils.getSnowflakeStr());
				platformFeeRecord.setSourceUserId(0L);
				platformFeeRecord.setCreateTime(new Date());
				platformFeeRecord.setRemark(dbPlatformFee.stripTrailingZeros().toPlainString());
				rewardRecordService.save(platformFeeRecord);
			}
		}
	}

	/**
	 * 计算服务中心费
	 * @param totalManageReward
	 * @param miningMgmtFeeConfig
	 * @param packageOrderList
	 */
	private void calculateServiceCenterFee(BigDecimal totalManageReward, MiningMgmtFeeConfig miningMgmtFeeConfig, List<MiningPackageOrder> packageOrderList) {
		BigDecimal serviceCenterFee = totalManageReward.multiply(miningMgmtFeeConfig.getServiceCenterRatio())
			.divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew)
			.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
		if(serviceCenterFee.compareTo(BigDecimal.ZERO)>0){
			List<UserInfo> serviceCenterUserList = userInfoService.lambdaQuery()
				.eq(UserInfo::getHasServiceCenter, 1)
				.select(UserInfo::getUserId)
				.list();
			if(CollectionUtil.isNotEmpty(serviceCenterUserList)){
				BigDecimal userServiceCenterReward = serviceCenterFee.divide(new BigDecimal(serviceCenterUserList.size()),
					ConstantStatic.newScale, ConstantStatic.roundingModeNew);
				if(userServiceCenterReward.compareTo(BigDecimal.ZERO)>0){
					List<RewardRecord> rewardRecordList = new ArrayList<>(packageOrderList.size());
					RewardRecord rewardRecordEntity = null;
					List<UserMoney> userMoneyValidNum7List = new ArrayList<>(packageOrderList.size()>1000?1000: packageOrderList.size());
					int batchSize = 1000;
					int stakeCount1 = 0;
					UserMoney entity = null;
					String orderNo = IDUtils.getSnowflakeStr();
					for (UserInfo userInfo : serviceCenterUserList) {
						rewardRecordEntity = new RewardRecord();
						rewardRecordEntity.setUserId(userInfo.getUserId());
						rewardRecordEntity.setAmount(userServiceCenterReward);
						rewardRecordEntity.setCoinType(ConstantType.user_money_coin_type.type_7);
						rewardRecordEntity.setSourceType(ConstantType.xms_reward_record_source_type.type_3);
						rewardRecordEntity.setSourceOrderCode(orderNo);
						rewardRecordEntity.setOrderCode(IDUtils.getSnowflakeStr());
						rewardRecordEntity.setSourceUserId(userInfo.getUserId());
						rewardRecordEntity.setCreateTime(new Date());
						rewardRecordList.add(rewardRecordEntity);

						//钱包流水
						entity = new UserMoney();
						entity.setId(userInfo.getUserId());
						entity.setValidNum7(userServiceCenterReward);
						entity.setGtId(IDUtils.getSnowflakeStr());
						entity.setSourceCode(orderNo);
						entity.setSourceId(userInfo.getUserId());
						entity.setSourceType(ConstantType.user_money_log_source_type.type_17);
						entity.setUpdateTime(new Date());
						userMoneyValidNum7List.add(entity);
						stakeCount1++;
						if (stakeCount1 >= batchSize) {
							bachUpdateMoneyValid7(userMoneyValidNum7List);
							userMoneyValidNum7List.clear();
							log.info("更新成功");
							stakeCount1 = 0;
						}
					}

					//用户的服务中心费奖金记录
					if (CollectionUtil.isNotEmpty(rewardRecordList)) {
						rewardRecordService.saveBatch(rewardRecordList);
					}
					//用户的服务中心费钱包流水
					if (CollectionUtil.isNotEmpty(userMoneyValidNum7List)) {
						bachUpdateMoneyValid7(userMoneyValidNum7List);
					}
				}
			}
		}
	}







	private void distributeConsumptionStatic(BigDecimal consumptionStaticReward,
											 List<UserComputingPowerBo> userComputingPowerList,
											 BigDecimal totalComputingPower,
											 String orderCode) {
		if (totalComputingPower.compareTo(BigDecimal.ZERO) <= 0) {
			log.info("消费分红池静态：全网算力为0，无法分配");
			return;
		}
		List<RewardRecord> rewardRecordList = new ArrayList<>(userComputingPowerList.size());
		List<UserMoney> userMoneyValidNum1List = new ArrayList<>(userComputingPowerList.size());
		int batchSize = 1000;
		int stakeCount = 0;
		Date now = new Date();
		for (UserComputingPowerBo userComputingPowerBo : userComputingPowerList) {
			BigDecimal eachReward = userComputingPowerBo.getComputingPower()
				.divide(totalComputingPower, ConstantStatic.newScale, ConstantStatic.roundingModeNew)
				.multiply(consumptionStaticReward);
			if (eachReward.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			UserMoney entity = new UserMoney();
			entity.setId(userComputingPowerBo.getUserId());
			entity.setValidNum2(eachReward);
			entity.setGtId(IDUtils.getSnowflakeStr());
			entity.setSourceCode(orderCode);
			entity.setSourceId(userComputingPowerBo.getUserId());
			entity.setSourceType(ConstantType.user_money_log_source_type.type_24);
			entity.setUpdateTime(now);
			userMoneyValidNum1List.add(entity);
			stakeCount++;
			if (stakeCount >= batchSize) {
				bachUpdateMoneyValid2(userMoneyValidNum1List);
				userMoneyValidNum1List.clear();
				stakeCount = 0;
			}

			RewardRecord rewardRecord = new RewardRecord();
			rewardRecord.setUserId(userComputingPowerBo.getUserId());
			rewardRecord.setAmount(eachReward);
			rewardRecord.setSourceType(ConstantType.xms_reward_record_source_type.type_20);
			rewardRecord.setCoinType(ConstantType.user_money_coin_type.type_2);
			rewardRecord.setSourceOrderCode(orderCode);
			rewardRecord.setOrderCode(IDUtils.getSnowflakeStr());
			rewardRecord.setSourceUserId(userComputingPowerBo.getUserId());
			rewardRecord.setCreateTime(now);
			rewardRecordList.add(rewardRecord);
		}
		if (CollectionUtil.isNotEmpty(userMoneyValidNum1List)) {
			bachUpdateMoneyValid2(userMoneyValidNum1List);
		}
		if (CollectionUtil.isNotEmpty(rewardRecordList)) {
			rewardRecordService.saveBatch(rewardRecordList);
		}
	}

	private void distributeConsumptionDynamic(BigDecimal consumptionDynamicReward,
											  List<UserLevelBo> userLevelBoList,
											  String orderCode,List<UserLevelConfig> userLevelConfigList) {
		/*
		if (consumptionDynamicReward.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}
		Map<Integer, List<UserLevelBo>> eligibleCache = new HashMap<>();
		List<UserMoney> dynamicMoneyList = new ArrayList<>();
		List<RewardRecord> dynamicRewardRecordList = new ArrayList<>();
		int batchSize = 1000;
		int stakeCount = 0;
		Date now = new Date();
		for (UserLevelConfig userLevelConfig : userLevelConfigList) {
			if (userLevelConfig.getLevel().equals(0)) {
				continue;
			}
			if (userLevelConfig.getRewardRatio() == null ||
				userLevelConfig.getRewardRatio().compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			BigDecimal levelTotalReward = consumptionDynamicReward.multiply(userLevelConfig.getRewardRatio())
				.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
			if (levelTotalReward.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			List<UserLevelBo> eligibleUsers = eligibleCache.computeIfAbsent(userLevelConfig.getLevel(), level ->
				userLevelBoList.stream()
					.filter(user -> user.getLevel() != null && user.getLevel() >= level)
					.collect(Collectors.toList())
			);
			if (CollectionUtil.isEmpty(eligibleUsers)) {
				log.info("消费分红动态：等级{} 无符合条件用户", userLevelConfig.getLevel());
				continue;
			}
			BigDecimal perUserReward = levelTotalReward.divide(new BigDecimal(eligibleUsers.size()),
				ConstantStatic.newScale, ConstantStatic.roundingModeNew);
			if (perUserReward.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			for (UserLevelBo userLevelBo : eligibleUsers) {
				UserMoney entity = new UserMoney();
				entity.setId(userLevelBo.getUserId());
				entity.setValidNum2(perUserReward);
				entity.setGtId(IDUtils.getSnowflakeStr());
				entity.setSourceCode(orderCode);
				entity.setSourceId(userLevelBo.getUserId());
				//entity.setSourceType(ConstantType.user_money_log_source_type.type_43);
				entity.setSourceType((getMoneySourceTypeByConsumptionStatic(userLevelConfig.getLevel())));
				entity.setUpdateTime(now);
				dynamicMoneyList.add(entity);
				stakeCount++;
				if (stakeCount >= batchSize) {
					bachUpdateMoneyValid2(dynamicMoneyList);
					dynamicMoneyList.clear();
					stakeCount = 0;
				}

				RewardRecord rewardRecord = new RewardRecord();
				rewardRecord.setUserId(userLevelBo.getUserId());
				rewardRecord.setAmount(perUserReward);
				//rewardRecord.setSourceType(ConstantType.xms_reward_record_source_type.type_17);
				rewardRecord.setSourceType((getRewardSourceTypeByConsumptionStatic(userLevelConfig.getLevel())));
				rewardRecord.setCoinType(ConstantType.user_money_coin_type.type_2);
				rewardRecord.setSourceOrderCode(orderCode);
				rewardRecord.setOrderCode(IDUtils.getSnowflakeStr());
				rewardRecord.setSourceUserId(userLevelBo.getUserId());
				rewardRecord.setCreateTime(now);
				dynamicRewardRecordList.add(rewardRecord);
			}
		}
		if (CollectionUtil.isNotEmpty(dynamicMoneyList)) {
			bachUpdateMoneyValid2(dynamicMoneyList);
		}
		if (CollectionUtil.isNotEmpty(dynamicRewardRecordList)) {
			rewardRecordService.saveBatch(dynamicRewardRecordList);
		}*/
	}

	private void distributeUCardStatic(BigDecimal reward, List<UserComputingPowerBo> userComputingPowerList,
									   BigDecimal totalComputingPower, String orderCode) {
		if (totalComputingPower.compareTo(BigDecimal.ZERO) <= 0) {
			log.info("U卡分红池静态：全网算力为0，无法分配");
			return;
		}
		List<RewardRecord> rewardRecordList = new ArrayList<>(userComputingPowerList.size());
		List<UserMoney> userMoneyValidNum1List = new ArrayList<>(userComputingPowerList.size());
		int batchSize = 1000;
		int stakeCount = 0;
		Date now = new Date();
		for (UserComputingPowerBo userComputingPowerBo : userComputingPowerList) {
			BigDecimal eachReward = userComputingPowerBo.getComputingPower()
				.divide(totalComputingPower, ConstantStatic.newScale, ConstantStatic.roundingModeNew)
				.multiply(reward);
			if (eachReward.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			UserMoney entity = new UserMoney();
			entity.setId(userComputingPowerBo.getUserId());
			entity.setValidNum2(eachReward);
			entity.setGtId(IDUtils.getSnowflakeStr());
			entity.setSourceCode(orderCode);
			entity.setSourceId(userComputingPowerBo.getUserId());
			entity.setSourceType(ConstantType.user_money_log_source_type.type_44);
			entity.setUpdateTime(now);
			userMoneyValidNum1List.add(entity);
			stakeCount++;
			if (stakeCount >= batchSize) {
				bachUpdateMoneyValid2(userMoneyValidNum1List);
				userMoneyValidNum1List.clear();
				stakeCount = 0;
			}

			RewardRecord rewardRecord = new RewardRecord();
			rewardRecord.setUserId(userComputingPowerBo.getUserId());
			rewardRecord.setAmount(eachReward);
			rewardRecord.setSourceType(ConstantType.xms_reward_record_source_type.type_18);
			rewardRecord.setCoinType(ConstantType.user_money_coin_type.type_2);
			rewardRecord.setSourceOrderCode(orderCode);
			rewardRecord.setOrderCode(IDUtils.getSnowflakeStr());
			rewardRecord.setSourceUserId(userComputingPowerBo.getUserId());
			rewardRecord.setCreateTime(now);
			rewardRecordList.add(rewardRecord);
		}
		if (CollectionUtil.isNotEmpty(userMoneyValidNum1List)) {
			bachUpdateMoneyValid2(userMoneyValidNum1List);
		}
		if (CollectionUtil.isNotEmpty(rewardRecordList)) {
			rewardRecordService.saveBatch(rewardRecordList);
		}
	}

	private void distributeUCardDynamic(BigDecimal reward, List<UserLevelBo> userLevelBoList, String orderCode,
										List<UserLevelConfig> userLevelConfigList) {
		/*
		if (reward.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}
		Map<Integer, List<UserLevelBo>> eligibleCache = new HashMap<>();
		List<UserMoney> dynamicMoneyList = new ArrayList<>();
		List<RewardRecord> dynamicRewardRecordList = new ArrayList<>();
		int batchSize = 1000;
		int stakeCount = 0;
		Date now = new Date();
		for (UserLevelConfig userLevelConfig : userLevelConfigList) {
			if (userLevelConfig.getLevel().equals(0)) {
				continue;
			}
			if (userLevelConfig.getRewardRatio() == null ||
				userLevelConfig.getRewardRatio().compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			BigDecimal levelTotalReward = reward.multiply(userLevelConfig.getRewardRatio())
				.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
			if (levelTotalReward.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			List<UserLevelBo> eligibleUsers = eligibleCache.computeIfAbsent(userLevelConfig.getLevel(), level ->
				userLevelBoList.stream()
					.filter(user -> user.getLevel() != null && user.getLevel() >= level)
					.collect(Collectors.toList())
			);
			if (CollectionUtil.isEmpty(eligibleUsers)) {
				log.info("U卡分红动态：等级{} 无符合条件用户", userLevelConfig.getLevel());
				continue;
			}
			BigDecimal perUserReward = levelTotalReward.divide(new BigDecimal(eligibleUsers.size()),
				ConstantStatic.newScale, ConstantStatic.roundingModeNew);
			if (perUserReward.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			for (UserLevelBo userLevelBo : eligibleUsers) {
				UserMoney entity = new UserMoney();
				entity.setId(userLevelBo.getUserId());
				entity.setValidNum2(perUserReward);
				entity.setGtId(IDUtils.getSnowflakeStr());
				entity.setSourceCode(orderCode);
				entity.setSourceId(userLevelBo.getUserId());
				//entity.setSourceType(ConstantType.user_money_log_source_type.type_45);
				entity.setUpdateTime(now);
				dynamicMoneyList.add(entity);
				stakeCount++;
				if (stakeCount >= batchSize) {
					bachUpdateMoneyValid2(dynamicMoneyList);
					dynamicMoneyList.clear();
					stakeCount = 0;
				}

				RewardRecord rewardRecord = new RewardRecord();
				rewardRecord.setUserId(userLevelBo.getUserId());
				rewardRecord.setAmount(perUserReward);
				//rewardRecord.setSourceType(ConstantType.xms_reward_record_source_type.type_25);
				rewardRecord.setCoinType(ConstantType.user_money_coin_type.type_2);
				rewardRecord.setSourceOrderCode(orderCode);
				rewardRecord.setOrderCode(IDUtils.getSnowflakeStr());
				rewardRecord.setSourceUserId(userLevelBo.getUserId());
				rewardRecord.setCreateTime(now);
				dynamicRewardRecordList.add(rewardRecord);
			}
		}
		if (CollectionUtil.isNotEmpty(dynamicMoneyList)) {
			bachUpdateMoneyValid2(dynamicMoneyList);
		}
		if (CollectionUtil.isNotEmpty(dynamicRewardRecordList)) {
			rewardRecordService.saveBatch(dynamicRewardRecordList);
		}*/
	}


/**
 * 计算第N天应释放的金额
 *
 * @param principal 本金
 * @param dailyRate 日利率
 * @param runDays   实际运行天数（产生了多少笔收益）
 * @param dayN      当前是第几天
 * @return 第N天释放金额
 */
public BigDecimal calculateDayNRelease(BigDecimal principal,
									   BigDecimal dailyRate,
									   int runDays,
									   int dayN) {
	if (dayN <= 1) {
		return BigDecimal.ZERO;
	}

	// 每日产生收益
	BigDecimal dailyReward = principal.multiply(dailyRate);

	// 每日从单笔收益中释放的金额
	BigDecimal dailyReleasePerReward = dailyReward.divide(
		new BigDecimal(100), 8, RoundingMode.HALF_UP);

	// 计算当天应该释放几笔收益
	int releaseCount = 0;
	for (int i = 1; i <= runDays; i++) {
		// 第i天产生的收益：
		// - 从第i+1天开始释放
		// - 释放100天，所以结束日期是 i+1+99 = i+100
		int startDay = i + 1;
		int endDay = i + 100;

		if (dayN >= startDay && dayN <= endDay) {
			releaseCount++;
		}
	}

	return dailyReleasePerReward.multiply(new BigDecimal(releaseCount));
}


/**
 * 批量更新活期矿机 可领取利息和累计产出金额
 */
private void w3Order0UpdateReward(List<w30OrderBo> w30OrderBoList) {
	// SQL语句：更新total_reward和unclaimed_reward字段
	String sql = "UPDATE t_w3_mining_package_order SET day_reward = ?, " +
		"total_reward = total_reward + ? WHERE id = ? and status = 0";

	int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {
			w30OrderBo stake = w30OrderBoList.get(i);
			ps.setBigDecimal(1, stake.getReward()); // total_reward增加的值
			ps.setBigDecimal(2, stake.getReward()); // unclaimed_reward增加的值
			ps.setLong(3, stake.getId()); // 记录ID
		}

		@Override
		public int getBatchSize() {
			return w30OrderBoList.size();
		}
	});

	// 检查更新结果
	if (ArrayUtil.contains(results, 0)) {
		TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		log.error("批量更新质押记录奖励失败 w30OrderBoList:{}", w30OrderBoList);
		throw new ServiceException("批量更新质押记录奖励失败");
	}
}

/**
 * 批量更新固定矿机 可领取利息和剩余产出金额
 */
private void w3Order1UpdateReward(List<W3MiningPackageOrderBo> w3MiningPackageOrderBos) {
	// SQL语句：更新total_reward和unclaimed_reward字段
	String sql = "UPDATE t_w3_mining_package_order SET day_reward = ?, " +
		"have_fsn_multiplied_value = have_fsn_multiplied_value + ?, status = ?  WHERE id = ?";

	int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {
			W3MiningPackageOrderBo stake = w3MiningPackageOrderBos.get(i);
			ps.setBigDecimal(1, stake.getHaveFsnMultipliedValue()); // total_reward增加的值
			//剩余可领取金额
			ps.setBigDecimal(2, stake.getHaveFsnMultipliedValue().negate()); // unclaimed_reward增加的值
			ps.setLong(3, stake.getStatus()); // 记录ID
			ps.setLong(4, stake.getId()); // 记录ID
		}

		@Override
		public int getBatchSize() {
			return w3MiningPackageOrderBos.size();
		}
	});

	// 检查更新结果
	if (ArrayUtil.contains(results, 0)) {
		TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		log.error("批量更新质押记录奖励失败");
		throw new ServiceException("批量更新质押记录奖励失败");
	}
}

/**
 * 补偿基金订单赎回本期的时候.t+1时间到了但是还没有执行发放本金任务
 */
@Override
@Transactional(rollbackFor = Exception.class)
public void compensateUnpaidPrincipalOrders() {
//	List<MiningPackageOrder> packageOrderList = miningPackageOrderService.lambdaQuery()
//		.eq(MiningPackageOrder::getSourceType, 0)
//		.eq(MiningPackageOrder::getReturnedBizStatus, 2)
//		.ge(MiningPackageOrder::getReturnedTime, DateUtil.offsetMinute(new Date(), 15))
//		.list();
//	if (CollectionUtil.isNotEmpty(packageOrderList)) {
//		for (MiningPackageOrder miningPackageOrder : packageOrderList) {
//			RedissonDelayOrder delayOrder = new RedissonDelayOrder(miningPackageOrder.getOrderNo(), null, SysConstant.TWO,
//				null, RedisConstant.StreamMsgConstant.DELAY_ORDER_TIMEOUT_QUEUE);
//			SpringUtils.getBean(StoreOrderAutoServiceImpl.class).hanlerOrder(delayOrder);
//			ThreadUtil.sleep(2000);
//		}
//	}
}


/**
 * 批量关闭矿机订单
 */
private void batchCloseMiningOrder(List<ReleaseMiningBo> releaseMiningBoList) {
	// SQL语句：更新total_reward和unclaimed_reward字段
	String sql = "UPDATE t_mining_package_order SET available_amount = available_amount + released_amount + ?, " +
		" total_released_amount = total_released_amount + ?, status = 2, released_amount = 0, close_amount = ?  WHERE id = ? and status =0 ";

	int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {
			ReleaseMiningBo stake = releaseMiningBoList.get(i);
			ps.setBigDecimal(1, stake.getReward()); // 记录reward
			ps.setBigDecimal(2, stake.getReward()); // 记录reward
			ps.setBigDecimal(3, stake.getClosePrice()); // 记录reward
			ps.setLong(4, stake.getId()); // 记录ID
		}

		@Override
		public int getBatchSize() {
			return releaseMiningBoList.size();
		}
	});

	// 检查更新结果
	if (ArrayUtil.contains(results, 0)) {
		TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		log.error("批量更新已经释放奖励的记录失败");
		throw new ServiceException("批量更新已经释放奖励的记录失败");
	}
}

/**
 * 批量更新可领取奖励的记录
 */
private void batchUpdateAvailableAmount(List<ReleaseMiningBo> releaseMiningBoList) {
	// SQL语句：更新total_reward和unclaimed_reward字段
	String sql = "UPDATE t_mining_package_order SET available_amount = available_amount + released_amount + ?, " +
		" total_released_amount = total_released_amount + ?, released_amount = 0, status = 1, close_amount = ?  WHERE id = ? and status = 0 ";

	int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {
			ReleaseMiningBo stake = releaseMiningBoList.get(i);
			ps.setBigDecimal(1, stake.getReward()); // 记录reward
			ps.setBigDecimal(2, stake.getReward()); // 记录reward
			ps.setBigDecimal(3, stake.getClosePrice()); // 记录reward
			ps.setLong(4, stake.getId()); // 记录ID
		}

		@Override
		public int getBatchSize() {
			return releaseMiningBoList.size();
		}
	});

	// 检查更新结果
	if (ArrayUtil.contains(results, 0)) {
		TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		log.error("批量更新已经释放奖励的记录失败");
		throw new ServiceException("批量更新已经释放奖励的记录失败");
	}
}

/**
 * 批量更新已经释放奖励的记录
 */
private void batchUpdateReleaseMining(List<ReleaseMiningBo> releaseMiningBoList) {
	// SQL语句：更新total_reward和unclaimed_reward字段
	String sql = "UPDATE t_mining_package_order SET released_amount = released_amount + ?, " +
		" total_released_amount = total_released_amount + ?, close_amount = ?  WHERE id = ?";

	int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {
			ReleaseMiningBo stake = releaseMiningBoList.get(i);
			ps.setBigDecimal(1, stake.getReward()); // 记录reward
			ps.setBigDecimal(2, stake.getReward()); // 记录reward
			ps.setBigDecimal(3, stake.getClosePrice()); // 记录reward
			ps.setLong(4, stake.getId()); // 记录ID
		}

		@Override
		public int getBatchSize() {
			return releaseMiningBoList.size();
		}
	});

	// 检查更新结果
	if (ArrayUtil.contains(results, 0)) {
		TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		log.error("批量更新已经释放奖励的记录失败");
		throw new ServiceException("批量更新已经释放奖励的记录失败");
	}
}


/**
 * 对usdt资产增加
 *
 * @param userMoneyList
 */
private void bachUpdateMoneyValid1(List<UserMoney> userMoneyList) {
	int[] ints = jdbcTemplate.batchUpdate(SQL_VALID_NUM1, new BatchPreparedStatementSetter() {
		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {

			ps.setTimestamp(1, new java.sql.Timestamp(userMoneyList.get(i).getUpdateTime().getTime()));
			ps.setString(2, userMoneyList.get(i).getGtId());
			ps.setBigDecimal(3, userMoneyList.get(i).getValidNum1());
			ps.setString(4, userMoneyList.get(i).getSourceCode());
			ps.setInt(5, userMoneyList.get(i).getSourceType());
			ps.setLong(6, userMoneyList.get(i).getSourceId());
			ps.setLong(7, userMoneyList.get(i).getId());
		}

		@Override
		public int getBatchSize() {
			return userMoneyList.size();
		}
	});
	if (ArrayUtil.contains(ints, 0)) {
		TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		log.error("结算更新回滚了");
		throw new ServiceException("更新资产结算更新回滚了");
	}
}

	/**
	 * 对产出DFC资产增加
	 *
	 * @param userMoneyList
	 */
	private void bachUpdateMoneyValid5(List<UserMoney> userMoneyList) {
		int[] ints = jdbcTemplate.batchUpdate(SQL_VALID_NUM5, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
			ps.setTimestamp(1, new java.sql.Timestamp(userMoneyList.get(i).getUpdateTime().getTime()));
			ps.setString(2, userMoneyList.get(i).getGtId());
			ps.setBigDecimal(3, userMoneyList.get(i).getValidNum5());
			ps.setString(4, userMoneyList.get(i).getSourceCode());
			ps.setInt(5, userMoneyList.get(i).getSourceType());
			ps.setLong(6, userMoneyList.get(i).getSourceId());
			ps.setLong(7, userMoneyList.get(i).getId());
		}

		@Override
		public int getBatchSize() {
			return userMoneyList.size();
		}
	});
	if (ArrayUtil.contains(ints, 0)) {
		TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		log.error("结算更新回滚了");
		throw new ServiceException("更新资产结算更新回滚了");
	}
}

/**
 * 对fsn资产增加
 *
 * @param userMoneyList
 */
private void bachUpdateMoneyValid2(List<UserMoney> userMoneyList) {
	int[] ints = jdbcTemplate.batchUpdate(SQL_VALID_NUM2, new BatchPreparedStatementSetter() {
		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {

			ps.setTimestamp(1, new java.sql.Timestamp(userMoneyList.get(i).getUpdateTime().getTime()));
			ps.setString(2, userMoneyList.get(i).getGtId());
			ps.setBigDecimal(3, userMoneyList.get(i).getValidNum2());
			ps.setString(4, userMoneyList.get(i).getSourceCode());
			ps.setInt(5, userMoneyList.get(i).getSourceType());
			ps.setLong(6, userMoneyList.get(i).getSourceId());
			ps.setLong(7, userMoneyList.get(i).getId());
		}

		@Override
		public int getBatchSize() {
			return userMoneyList.size();
		}
	});
	if (ArrayUtil.contains(ints, 0)) {
		TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		log.error("结算更新回滚了");
		throw new ServiceException("更新资产结算更新回滚了");
	}
}


/**
 * 对fsn资产增加
 *
 * @param userMoneyList
 */
private void bachUpdateMoneyValid6(List<UserMoney> userMoneyList) {
	int[] ints = jdbcTemplate.batchUpdate(SQL_VALID_NUM6, new BatchPreparedStatementSetter() {
		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {

			ps.setTimestamp(1, new java.sql.Timestamp(userMoneyList.get(i).getUpdateTime().getTime()));
			ps.setString(2, userMoneyList.get(i).getGtId());
			ps.setBigDecimal(3, userMoneyList.get(i).getValidNum6());
			ps.setString(4, userMoneyList.get(i).getSourceCode());
			ps.setInt(5, userMoneyList.get(i).getSourceType());
			ps.setLong(6, userMoneyList.get(i).getSourceId());
			ps.setLong(7, userMoneyList.get(i).getId());
		}

		@Override
		public int getBatchSize() {
			return userMoneyList.size();
		}
	});
	if (ArrayUtil.contains(ints, 0)) {
		TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		log.error("结算更新回滚了");
		throw new ServiceException("更新资产结算更新回滚了");
	}
}

	/**
	 * 对fsn资产增加
	 *
	 * @param userMoneyList
	 */
	private void bachUpdateMoneyValid7(List<UserMoney> userMoneyList) {
		int[] ints = jdbcTemplate.batchUpdate(SQL_VALID_NUM7, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {

				ps.setTimestamp(1, new java.sql.Timestamp(userMoneyList.get(i).getUpdateTime().getTime()));
				ps.setString(2, userMoneyList.get(i).getGtId());
				ps.setBigDecimal(3, userMoneyList.get(i).getValidNum7());
				ps.setString(4, userMoneyList.get(i).getSourceCode());
				ps.setInt(5, userMoneyList.get(i).getSourceType());
				ps.setLong(6, userMoneyList.get(i).getSourceId());
				ps.setLong(7, userMoneyList.get(i).getId());
			}

			@Override
			public int getBatchSize() {
				return userMoneyList.size();
			}
		});
		if (ArrayUtil.contains(ints, 0)) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			log.error("结算更新回滚了");
			throw new ServiceException("更新资产结算更新回滚了");
		}
	}


/**
 * 对fsn资产增加
 *
 * @param userMoneyList
 */
private void bachUpdateMoneyValid3(List<UserMoney> userMoneyList) {
	int[] ints = jdbcTemplate.batchUpdate(SQL_VALID_NUM3, new BatchPreparedStatementSetter() {
		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {

			ps.setTimestamp(1, new java.sql.Timestamp(userMoneyList.get(i).getUpdateTime().getTime()));
			ps.setString(2, userMoneyList.get(i).getGtId());
			ps.setBigDecimal(3, userMoneyList.get(i).getValidNum3());
			ps.setString(4, userMoneyList.get(i).getSourceCode());
			ps.setInt(5, userMoneyList.get(i).getSourceType());
			ps.setLong(6, userMoneyList.get(i).getSourceId());
			ps.setLong(7, userMoneyList.get(i).getId());
		}

		@Override
		public int getBatchSize() {
			return userMoneyList.size();
		}
	});
	if (ArrayUtil.contains(ints, 0)) {
		TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		log.error("结算更新回滚了");
		throw new ServiceException("更新资产结算更新回滚了");
	}
}

/**
 * 返回相差几秒，如果当前时间晚于结束时间则返回固定的10秒
 *
 * @param current 当前时间
 * @param endTime 结束时间
 * @return 相差的秒数
 */
public Long getEndTime(Date current, Date endTime) {
	if (current == null || endTime == null) {
		throw new IllegalArgumentException("时间参数不能为空");
	}

	if (current.after(endTime)) {
		return 10L; // 当前时间晚于结束时间时返回10秒
	} else {
		// 计算时间差（毫秒）
		long diffMillis = endTime.getTime() - current.getTime();
		// 转换为秒
		return diffMillis / 1000;
	}
}


/**
 * 根据字段键值构建动态SQL
 */
private String buildDynamicUpdateSql(String fieldKey) {
	StringBuilder sql = new StringBuilder("UPDATE t_user_income_summary SET ");
	boolean hasField = false;

	if (fieldKey.contains("0")) {
		sql.append("source_type21_balance0 = source_type21_balance0 + ?");
		hasField = true;
	}
	if (fieldKey.contains("1")) {
		if (hasField) sql.append(", ");
		sql.append("source_type21_balance1 = source_type21_balance1 + ?");
		hasField = true;
	}
	if (fieldKey.contains("3")) {
		if (hasField) sql.append(", ");
		sql.append("source_type23_balance = source_type23_balance + ?");
		hasField = true;
	}
	if (fieldKey.contains("4")) {
		if (hasField) sql.append(", ");
		sql.append("source_type24_balance = source_type24_balance + ?");
		hasField = true;
	}
	if (fieldKey.contains("5")) {
		if (hasField) sql.append(", ");
		sql.append("source_type25_balance = source_type25_balance + ?");
		hasField = true;
	}

	sql.append(" WHERE user_id = ?");
	return sql.toString();
}
}
