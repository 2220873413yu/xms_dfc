package com.xms.dao.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollectionUtil;
import com.xms.common.config.redis.lock.RedisLock;
import com.xms.common.constant.RedisConstant;
import com.xms.common.exception.ServiceException;
import com.xms.common.result.ResponseCode;
import com.xms.dao.entity.domain.UserInfo;
import com.xms.dao.service.UserInfoService;
import com.xms.dao.service.impl.XmsDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xms.dao.mapper.MiningPackageOrderMapper;
import com.xms.dao.domain.MiningPackageOrder;
import com.xms.dao.service.IMiningPackageOrderService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 矿机订单Service业务层处理
 *
 * @author xms
 * @date 2026-02-23
 */
@Service
public class MiningPackageOrderServiceImpl extends XmsDataServiceImpl<MiningPackageOrderMapper, MiningPackageOrder> implements IMiningPackageOrderService
{

	@Autowired
	private UserInfoService userInfoService;
	/**
	 * 下架矿机功能
	 * @param req
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisLock(value = RedisConstant.LockConstant.XMS_BUY_MINING_APPLY, param = "#userId")
	public int disableStakeOrder(MiningPackageOrder req,Long userId) {
		MiningPackageOrder miningPackageOrder = lambdaQuery()
			.eq(MiningPackageOrder::getId, req.getId())
			.one();
		if(miningPackageOrder.getStatus() == 4){
			throw new ServiceException("订单已下架");
		}

		//扣减业绩
		UserInfo userInfo = userInfoService.lambdaQuery()
			.eq(UserInfo::getUserId, miningPackageOrder.getUserId())
			.one();

		//添加自身业绩
		boolean update = userInfoService.lambdaUpdate()
			.eq(UserInfo::getUserId, userInfo.getUserId())
			.setSql("performance = performance - 1 ")
			.update();
		if(!update){
			throw new ServiceException(ResponseCode.CODE_1002);
		}

		//直推、团队、小区业绩
		List<Long> parentIds = userInfo.getParentIds();
		if(CollectionUtil.isNotEmpty(parentIds)){
			update = userInfoService.lambdaUpdate()
				.eq(UserInfo::getUserId, userInfo.getInviteUserId())
				.setSql("sub_performance = sub_performance - 1")
				.update();
			if(!update){
				throw new ServiceException(ResponseCode.CODE_1002);
			}

			update = userInfoService.lambdaUpdate()
				.in(UserInfo::getUserId, parentIds)
				.setSql("umbrella_performance = umbrella_performance - 1")
				.update();
			if(!update){
				throw new ServiceException(ResponseCode.CODE_1002);
			}
		}
		//计算小区业绩
		parentIds.add(userInfo.getUserId());
		calculateCommunityPerformance(parentIds);

		boolean update1 = lambdaUpdate()
			.eq(MiningPackageOrder::getId, req.getId())
			.eq(MiningPackageOrder::getStatus, miningPackageOrder.getStatus())
			.set(MiningPackageOrder::getStatus, 4)
			.update();
		if(!update1){
			throw new ServiceException("订单状态已经被修改了");
		}

		Long count = lambdaQuery()
			.eq(MiningPackageOrder::getUserId, miningPackageOrder.getUserId())
			.in(MiningPackageOrder::getStatus, 0, 1, 2, 3)
			.count();

		if(count<=0){
			userInfoService.lambdaUpdate()
				.eq(UserInfo::getUserId, miningPackageOrder.getUserId())
				.set(UserInfo::getIsValid,0)
				.update();
		}
		return 1;
	}

	/**
	 * 管理后台也有计算等级的地方
	 * 计算小区业绩
	 * @param parentIds
	 */
	private void calculateCommunityPerformance(List<Long> parentIds) {
		// 小区业绩（对所有上级计算：去掉最大直推线）
		Map<Long, BigDecimal> userCommunityPerformanceMap = userInfoService.lambdaQuery()
			.in(UserInfo::getUserId, parentIds)
			.select(UserInfo::getUserId,UserInfo::getCommunityPerformance)
			.list().stream().collect(Collectors.toMap(UserInfo::getUserId, UserInfo::getCommunityPerformance, (k1, k2) -> k2));
		if (CollectionUtil.isNotEmpty(parentIds)) {
			for (Long parentId : parentIds) {
				List<UserInfo> children = userInfoService.lambdaQuery()
					.eq(UserInfo::getInviteUserId, parentId)
					.select(UserInfo::getUserId,UserInfo::getUmbrellaPerformance,
						UserInfo::getPerformance)
					.list();
				if (CollectionUtil.isEmpty(children) || children.size() <= 1) {
					if(userCommunityPerformanceMap.get(parentId).compareTo(BigDecimal.ZERO)!=0){
						userInfoService.lambdaUpdate()
							.eq(UserInfo::getUserId, parentId)
							.set(UserInfo::getCommunityPerformance, BigDecimal.ZERO)
							.update();
					}
					continue;
				}
				BigDecimal totalChildPerformance = BigDecimal.ZERO;
				BigDecimal maxChildPerformance = BigDecimal.ZERO;
				for (UserInfo child : children) {
					BigDecimal childUmbrella = child.getUmbrellaPerformance();
					BigDecimal performance = child.getPerformance();
					childUmbrella = childUmbrella.add(performance);

					totalChildPerformance = totalChildPerformance.add(childUmbrella);
					if (childUmbrella.compareTo(maxChildPerformance) > 0) {
						maxChildPerformance = childUmbrella;
					}
				}
				BigDecimal communityPerformance = totalChildPerformance.subtract(maxChildPerformance);
				if (communityPerformance.compareTo(BigDecimal.ZERO) < 0) {
					communityPerformance = BigDecimal.ZERO;
				}

				if(userCommunityPerformanceMap.get(parentId).compareTo(communityPerformance)!=0){
					userInfoService.lambdaUpdate()
						.eq(UserInfo::getUserId, parentId)
						.set(UserInfo::getCommunityPerformance, communityPerformance)
						.update();
				}
			}
		}
	}


	/**
     * 查询矿机订单列表
     *
     *
     * @param miningPackageOrder 矿机订单
     * @return 矿机订单
     */
    @Override
    public List<MiningPackageOrder> selectMiningPackageOrderList(MiningPackageOrder miningPackageOrder)
    {
        return baseMapper.selectMiningPackageOrderList(miningPackageOrder);
    }

	@Override
	public int processShipment(MiningPackageOrder req) {
		MiningPackageOrder miningPackageOrder = lambdaQuery()
			.eq(MiningPackageOrder::getId, req.getId())
			.one();

		if(miningPackageOrder.getStakeType()!= null && miningPackageOrder.getStakeType() == 2) {
			if(miningPackageOrder.getStatus() ==4){
				throw new ServiceException("订单已下架");
			}
			lambdaUpdate()
				.eq(MiningPackageOrder::getId, req.getId())
				.set(MiningPackageOrder::getShippingStatus,1)
				.set(MiningPackageOrder::getShippingCompany,req.getShippingCompany())
				.set(MiningPackageOrder::getTrackingNo,req.getTrackingNo())
				.update();
		}
		return 1;
	}

	@Override
	public int stopOrOpenOrder(MiningPackageOrder req) {
		MiningPackageOrder queryOrder = lambdaQuery()
			.eq(MiningPackageOrder::getId, req.getId())
			.one();
		if(queryOrder!=null){
			if(queryOrder.getStatus() ==4){
				throw new ServiceException("订单已下架");
			}
			if(queryOrder.getStatus()>1){
				//修改状态
				lambdaUpdate()
					.eq(MiningPackageOrder::getId, req.getId())
					.set(MiningPackageOrder::getStatus,req.getStatus())
					.update();
			}
		}
		return 1;
	}

	@Override
	public int updateDayReward(MiningPackageOrder req) {
		if(req.getDayReward() == null || req.getDayReward().compareTo(BigDecimal.ZERO) < 0){
			throw new ServiceException("每日收益数不能小于等于0");
		}
		MiningPackageOrder queryOrder = lambdaQuery().eq(MiningPackageOrder::getId, req.getId())
			.one();
		if(queryOrder == null || queryOrder.getStatus() ==4){
			throw new ServiceException("订单已下架");
		}
		lambdaUpdate()
			.eq(MiningPackageOrder::getId, req.getId())
			.set(MiningPackageOrder::getDayReward,req.getDayReward())
			.set(MiningPackageOrder::getUpdateTime,new Date())
			.update();
		return 1;
	}
}
