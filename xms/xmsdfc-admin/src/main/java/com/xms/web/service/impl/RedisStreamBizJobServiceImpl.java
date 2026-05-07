package com.xms.web.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xms.common.constant.*;
import com.xms.common.exception.ServiceException;
import com.xms.common.mq.dynamic.OrderMsgDO;
import com.xms.common.result.ResponseCode;
import com.xms.common.utils.uuid.IDUtils;
import com.xms.dao.domain.*;
import com.xms.dao.entity.bo.ChangeLevelUserBo;
import com.xms.dao.entity.domain.UserInfo;
import com.xms.dao.entity.domain.UserMoney;
import com.xms.dao.entity.vo.ParentUserTaskVo;
import com.xms.dao.service.*;
import com.xms.dao.service.impl.UserInfoServiceImpl;
import com.xms.web.service.IRedisStreamBizJobService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: renengadePISTA
 * @createDate: 2023/8/28
 */
@Service
@AllArgsConstructor
@Slf4j
public class RedisStreamBizJobServiceImpl implements IRedisStreamBizJobService {
	private static final String SQL_VALID_NUM3 = "UPDATE t_user_money SET update_time=?,gt_id=?,valid_num3=valid_num3+?,source_code=?,source_type=?,source_id=? WHERE id=? ";

	private static final String SQL_VALID_NUM1 = "UPDATE t_user_money SET update_time=?,gt_id=?,valid_num1=valid_num1+?,source_code=?,source_type=?,source_id=? WHERE id=? ";
	private static final String SQL_VALID_NUM2 = "UPDATE t_user_money SET update_time=?,gt_id=?,valid_num2=valid_num2+?,source_code=?,source_type=?,source_id=? WHERE id=? ";


	@Autowired
	private UserInfoService userInfoService;

	@Autowired
	private IUserLevelConfigService userLevelConfigService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserWalletService userWalletServiceImpl;

	@Autowired
	private IRewardRecordService rewardRecordService;

	@Autowired
	private IMiningPackageOrderService miningPackageOrderService;

	@Autowired
	private IMiningRewardConfigService miningRewardConfigService;


	@Override
	@Transactional(rollbackFor = Exception.class)
	public Integer handlerDynamicOrderSettlement(List req) {
		List<OrderMsgDO> ids = BeanUtil.copyToList(req, OrderMsgDO.class);
		log.debug("需要处理的矿机订单 orders:{}", ids);
		if(CollectionUtil.isNotEmpty(ids)) {
			OrderMsgDO orderMsgDO = ids.get(0);
			if(orderMsgDO.getBizType().equals(1)){
				//处理购买矿机的订单任务
				Integer x = handleBizType1(orderMsgDO);
				if (x != null) return x;
			}else if(orderMsgDO.getBizType().equals(2)){
				//处理用户之前没有在dapp注册但是购买了节点的数据
				handleBizType2(orderMsgDO);
			}else if(orderMsgDO.getBizType().equals(3)){
				//处理领取了空投 需要更改订单状态、计算等级、修改订单状态
				handleBizType3(orderMsgDO);
			}


		}
		return 1;
	}

	/**
	 * 处理领取了空投 需要更改订单状态、计算等级、修改订单状态
	 * @param orderMsgDO
	 */
	private void handleBizType3(OrderMsgDO orderMsgDO) {
	}

	/**
	 * 处理用户之前没有在dapp注册但是购买了节点的数据
	 * @param orderMsgDO
	 */
	private void handleBizType2(OrderMsgDO orderMsgDO) {
	}

	/**
	 * 处理购买矿机的订单任务
	 * @param orderMsgDO
	 * @return
	 */
	@Nullable
	private Integer handleBizType1(OrderMsgDO orderMsgDO) {
		//正常购买矿机回调处理
		MiningPackageOrder queryOrder = miningPackageOrderService.lambdaQuery()
			.eq(MiningPackageOrder::getId, orderMsgDO.getId())
			.eq(MiningPackageOrder::getBizStatus,0)
			.one();
		if(queryOrder==null){
			log.info("矿机订单已经处理 订单id:{}", orderMsgDO.getId());
			return 1;
		}
		//订单奖励
		BigDecimal orderReward = queryOrder.getPayType() == 1?queryOrder.getOrderValueUsdt():queryOrder.getPayValidNum2();
		Integer rewardCoinType = queryOrder.getPayType() == 1?ConstantType.user_money_coin_type.type_4:ConstantType.user_money_coin_type.type_2;
		//查询直推、间推比例
		//1:直推,2:间推,3:市代理,4:省代理,5:全国代理
		Map<Integer, BigDecimal> rewardMap = miningRewardConfigService.lambdaQuery()
			.list().stream()
			.map(config->{
				config.setRewardRatio(config.getRewardRatio().divide(SysConstant.BAIFENBI, ConstantStatic.newScale, ConstantStatic.roundingModeNew));
				return config;
			})
			.collect(Collectors.toMap(MiningRewardConfig::getRewardLevel, MiningRewardConfig::getRewardRatio, (k1, k2) -> k2));
		UserInfo userInfo = userInfoService.lambdaQuery()
			.eq(UserInfo::getUserId, queryOrder.getUserId())
			.one();
		if(userInfo.getInviteUserId()!=null){
			// 按邀请链向上查找最近两个有效上级：第一个有效用户发直推，第二个有效用户发间推
			List<ParentUserTaskVo> validParentUsers = userInfoService.getValidParentUserTaskVo(userInfo.getUserId(), 2);
			if(CollectionUtil.isNotEmpty(validParentUsers)){
				ParentUserTaskVo directUser = validParentUsers.get(0);
				// 最近的有效上级作为直推奖励接收人
				grantInviteReward(directUser.getUserId(), rewardMap.get(1), orderReward, queryOrder, userInfo.getUserId(),
					rewardCoinType, ConstantType.user_money_log_source_type.type_8,
					ConstantType.xms_reward_record_source_type.type_10);
				if(validParentUsers.size() > 1){
					ParentUserTaskVo indirectUser = validParentUsers.get(1);
					// 在直推上级之上继续找到的下一个有效用户作为间推奖励接收人
					grantInviteReward(indirectUser.getUserId(), rewardMap.get(2), orderReward, queryOrder, userInfo.getUserId(),
						rewardCoinType, ConstantType.user_money_log_source_type.type_9,
						ConstantType.xms_reward_record_source_type.type_11);
				}
			}
		}

		//查询市代、省代、全国代人数
		List<UserInfo> userInfoList = userInfoService.list(new QueryWrapper<UserInfo>()
			.select("user_id", "is_valid","GREATEST(game_level, min_game_level) AS game_level")
			// 虚拟等级：取 game_level 与 min_game_level 的较大值
			.apply("GREATEST(game_level, min_game_level) IN (3,4,5)"));
		if(CollectionUtil.isNotEmpty(userInfoList)){
			Map<Integer, List<UserInfo>> levelMap = userInfoList.stream()
			.collect(Collectors.groupingBy(UserInfo::getGameLevel));
			List<UserMoney> userMoneyValidNum1List = new ArrayList<>(userInfoList.size()>1000?1000:userInfoList.size());
			List<RewardRecord> rewardRecordList = new ArrayList<>(1000);
			RewardRecord rewardRecordEntity = null;
			UserMoney entity = null;
			int stakeCount1 = 0;
			int batchSize = 1000;
		List<UserInfo> cityAgents = levelMap.getOrDefault(3, Collections.emptyList());
		List<UserInfo> provinceAgents = levelMap.getOrDefault(4, Collections.emptyList());
		List<UserInfo> nationAgents = levelMap.getOrDefault(5, Collections.emptyList());
		if(CollectionUtil.isNotEmpty(cityAgents)){
			BigDecimal cityAgentsReward = rewardMap.get(3).multiply(orderReward)
				.setScale(ConstantStatic.newScale,ConstantStatic.roundingModeNew);
			if(cityAgentsReward.compareTo(BigDecimal.ZERO)>0){
				//奖励大于0才发放
				BigDecimal userReward = cityAgentsReward.divide
					(new BigDecimal(cityAgents.size()), ConstantStatic.newScale, ConstantStatic.roundingModeNew);
				if(userReward.compareTo(BigDecimal.ZERO)>0){
					for (UserInfo cityAgent : cityAgents) {
						if(cityAgent.getIsValid() == 1){
							entity = new UserMoney();
							entity.setId(cityAgent.getUserId());
							entity.setValidNum1(userReward);
							entity.setGtId(IDUtils.getSnowflakeStr());
							entity.setSourceCode(queryOrder.getOrderNo());
							entity.setSourceId(queryOrder.getUserId());
							entity.setSourceType(ConstantType.user_money_log_source_type.type_10);
							entity.setUpdateTime(new Date());
							userMoneyValidNum1List.add(entity);
							stakeCount1++;
							if (stakeCount1 >= batchSize) {
								bachUpdateMoneyValid1(userMoneyValidNum1List, rewardCoinType);
								userMoneyValidNum1List.clear();
								log.info("更新成功");
								stakeCount1 = 0;
							}

							rewardRecordEntity = new RewardRecord();
							rewardRecordEntity.setOrderCode(IDUtils.getSnowflakeStr());
							rewardRecordEntity.setUserId(cityAgent.getUserId());
							rewardRecordEntity.setAmount(userReward);
							rewardRecordEntity.setSourceType(ConstantType.xms_reward_record_source_type.type_12);
							rewardRecordEntity.setCoinType(rewardCoinType);
							rewardRecordEntity.setSourceUserId(userInfo.getUserId());
							rewardRecordEntity.setSourceOrderCode(queryOrder.getOrderNo());
							rewardRecordEntity.setGtId(IDUtils.getSnowflakeStr());
							rewardRecordList.add(rewardRecordEntity);
						}
					}
				}
			}

		}

			if(CollectionUtil.isNotEmpty(provinceAgents)){
				BigDecimal provinceAgentsReward = rewardMap.get(4).multiply(orderReward)
					.setScale(ConstantStatic.newScale,ConstantStatic.roundingModeNew);
				if(provinceAgentsReward.compareTo(BigDecimal.ZERO)>0){
					//奖励大于0才发放
					BigDecimal userReward = provinceAgentsReward.divide
						(new BigDecimal(provinceAgents.size()), ConstantStatic.newScale, ConstantStatic.roundingModeNew);
					if(userReward.compareTo(BigDecimal.ZERO)>0){
						for (UserInfo provinceAgent : provinceAgents) {
							if(provinceAgent.getIsValid() == 1){
								entity = new UserMoney();
								entity.setId(provinceAgent.getUserId());
								entity.setValidNum1(userReward);
								entity.setGtId(IDUtils.getSnowflakeStr());
								entity.setSourceCode(queryOrder.getOrderNo());
								entity.setSourceId(queryOrder.getUserId());
								entity.setSourceType(ConstantType.user_money_log_source_type.type_11);
								entity.setUpdateTime(new Date());
								userMoneyValidNum1List.add(entity);
								stakeCount1++;
								if (stakeCount1 >= batchSize) {
									bachUpdateMoneyValid1(userMoneyValidNum1List, rewardCoinType);
									userMoneyValidNum1List.clear();
									log.info("更新成功");
									stakeCount1 = 0;
								}

								rewardRecordEntity = new RewardRecord();
								rewardRecordEntity.setOrderCode(IDUtils.getSnowflakeStr());
								rewardRecordEntity.setUserId(provinceAgent.getUserId());
								rewardRecordEntity.setAmount(userReward);
								rewardRecordEntity.setSourceType(ConstantType.xms_reward_record_source_type.type_13);
								rewardRecordEntity.setCoinType(rewardCoinType);
								rewardRecordEntity.setSourceUserId(userInfo.getUserId());
								rewardRecordEntity.setSourceOrderCode(queryOrder.getOrderNo());
								rewardRecordEntity.setGtId(IDUtils.getSnowflakeStr());
								rewardRecordList.add(rewardRecordEntity);
							}

						}
					}
				}


			}

			if(CollectionUtil.isNotEmpty(nationAgents)){
				BigDecimal nationAgentsReward = rewardMap.get(5).multiply(orderReward)
					.setScale(ConstantStatic.newScale,ConstantStatic.roundingModeNew);
				if(nationAgentsReward.compareTo(BigDecimal.ZERO)>0){
					BigDecimal userReward = nationAgentsReward.divide
						(new BigDecimal(nationAgents.size()), ConstantStatic.newScale, ConstantStatic.roundingModeNew);
					if(userReward.compareTo(BigDecimal.ZERO)>0){
						for (UserInfo nationAgent : nationAgents) {
							if(nationAgent.getIsValid() == 1){
								entity = new UserMoney();
								entity.setId(nationAgent.getUserId());
								entity.setValidNum1(userReward);
								entity.setGtId(IDUtils.getSnowflakeStr());
								entity.setSourceCode(queryOrder.getOrderNo());
								entity.setSourceId(queryOrder.getUserId());
								entity.setSourceType(ConstantType.user_money_log_source_type.type_12);
								entity.setUpdateTime(new Date());
								userMoneyValidNum1List.add(entity);
								stakeCount1++;
								if (stakeCount1 >= batchSize) {
									bachUpdateMoneyValid1(userMoneyValidNum1List, rewardCoinType);
									userMoneyValidNum1List.clear();
									log.info("更新成功");
									stakeCount1 = 0;
								}

								rewardRecordEntity = new RewardRecord();
								rewardRecordEntity.setOrderCode(IDUtils.getSnowflakeStr());
								rewardRecordEntity.setUserId(nationAgent.getUserId());
								rewardRecordEntity.setAmount(userReward);
								rewardRecordEntity.setSourceType(ConstantType.xms_reward_record_source_type.type_14);
								rewardRecordEntity.setCoinType(rewardCoinType);
								rewardRecordEntity.setSourceUserId(userInfo.getUserId());
								rewardRecordEntity.setSourceOrderCode(queryOrder.getOrderNo());
								rewardRecordEntity.setGtId(IDUtils.getSnowflakeStr());
								rewardRecordList.add(rewardRecordEntity);
							}
						}
					}
				}
			}

			//修改资产
			if (CollectionUtil.isNotEmpty(userMoneyValidNum1List)) {
				bachUpdateMoneyValid1(userMoneyValidNum1List, rewardCoinType);
			}

			//插入v1资产
			if (CollectionUtil.isNotEmpty(rewardRecordList)) {
				rewardRecordService.saveBatch(rewardRecordList);
			}
		}

		boolean update = miningPackageOrderService.lambdaUpdate()
			.eq(MiningPackageOrder::getId, queryOrder.getId())
			.eq(MiningPackageOrder::getBizStatus, 0)
			.set(MiningPackageOrder::getBizStatus, 1)
			.set(MiningPackageOrder::getUpdateTime, new Date())
			.update();
		if(!update){
			throw new ServiceException("更新矿机订单信息失败");
		}

		//处理等级
		List<Long> parentIds = userInfo.getParentIds();
		parentIds.add(userInfo.getUserId());
		parentIds.addLast(userInfo.getUserId());
		List<UserInfo> parentUserInfoList = userInfoService.lambdaQuery()
			.in(UserInfo::getUserId, parentIds)
			.orderByAsc(UserInfo::getUserId)
			.list();

		List<UserLevelConfig> userLevelConfigList = userLevelConfigService.lambdaQuery()
			.gt(UserLevelConfig::getLevel,0)
			.orderByAsc(UserLevelConfig::getLevel)
			.list();
		for (UserInfo info : parentUserInfoList) {
			callUserLevel(info, userLevelConfigList);
		}
		return null;
	}

	private void grantInviteReward(Long rewardUserId, BigDecimal rewardRatio, BigDecimal orderReward,
								   MiningPackageOrder queryOrder, Long sourceUserId, Integer rewardCoinType,
								   Integer userMoneySourceType, Integer rewardRecordSourceType) {
		if(rewardUserId == null || rewardRatio == null){
			return;
		}
		BigDecimal rewardAmount = rewardRatio.multiply(orderReward)
			.setScale(ConstantStatic.newScale, ConstantStatic.roundingModeNew);
		if(rewardAmount.compareTo(BigDecimal.ZERO) <= 0){
			return;
		}
		int count = userWalletServiceImpl.handerUserMoney(rewardAmount, queryOrder.getOrderNo(),
			rewardUserId, sourceUserId, userMoneySourceType, rewardCoinType);
		if (count != 1) {
			throw new ServiceException(ResponseCode.CODE_1015);
		}

		RewardRecord rewardRecordEntity = new RewardRecord();
		rewardRecordEntity.setOrderCode(IDUtils.getSnowflakeStr());
		rewardRecordEntity.setUserId(rewardUserId);
		rewardRecordEntity.setAmount(rewardAmount);
		rewardRecordEntity.setSourceType(rewardRecordSourceType);
		rewardRecordEntity.setCoinType(rewardCoinType);
		rewardRecordEntity.setSourceUserId(sourceUserId);
		rewardRecordEntity.setSourceOrderCode(queryOrder.getOrderNo());
		rewardRecordEntity.setGtId(IDUtils.getSnowflakeStr());
		rewardRecordService.save(rewardRecordEntity);
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
			.select(UserInfo::getUserId,UserInfo::getGameLevel,UserInfo::getPerformance,UserInfo::getUmbrellaPerformance,
				UserInfo::getMinGameLevel)
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
					if(CollectionUtil.isEmpty(directUserList) || directUserList.size() < userLevelConfig.getRequiredLegNum()){
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
						long matchCount = childUserList.stream()
							.filter(u -> u.getGameLevel() >= legLevelMin)
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
	 * 对佣金钱包资产增加
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
	 * 对usdt资产增加
	 *
	 * @param userMoneyList
	 */
	private void bachUpdateMoneyValid1(List<UserMoney> userMoneyList,Integer coinType) {
		int[] ints = jdbcTemplate.batchUpdate(coinType == 1?SQL_VALID_NUM1:SQL_VALID_NUM2, new BatchPreparedStatementSetter() {
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
}
