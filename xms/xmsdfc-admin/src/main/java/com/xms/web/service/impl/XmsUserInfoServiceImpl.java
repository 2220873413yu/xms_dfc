package com.xms.web.service.impl;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.StrUtil;
import com.xms.common.config.redis.XmsRedis;
import com.xms.common.config.redis.delayqueue.config.RedissonTemplate;
import com.xms.common.constant.ConstantStatic;
import com.xms.common.core.domain.AjaxResult;
import com.xms.common.core.domain.entity.SysDictData;
import com.xms.common.exception.ServiceException;
import com.xms.common.utils.CollectionUtil;
import com.xms.common.utils.StringUtils;
import com.xms.common.utils.TreeBuildUtils;
import com.xms.common.utils.googleUtil.GoogleAuthenticator;
import com.xms.common.utils.sign.Md5Utils;
import com.xms.common.utils.uuid.IDUtils;
import com.xms.dao.entity.bo.TeamUsersBo;
import com.xms.dao.entity.bo.UserInfoReqBo;
import com.xms.dao.entity.domain.UserInfo;
import com.xms.dao.mapper.UserInfoMapper;
import com.xms.dao.service.UserInfoService;
import com.xms.system.service.ISysUserService;
import com.xms.web.service.XmsUserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户信息Service业务层处理
 *
 * @date 2023-07-28
 */
@Service
@Slf4j
public class XmsUserInfoServiceImpl implements XmsUserInfoService {
	@Autowired
	private XmsRedis xmsRedis;

	@Autowired
	private ISysUserService sysUserService;

	@Autowired
	private UserInfoService userInfoService;

	@Autowired
	private RedissonTemplate redissonTemplate;

	@Autowired
	private UserInfoMapper userInfoMapper;

	/**
	 * 查询用户信息列表
	 *
	 * @param userInfo 用户信息
	 * @return 用户信息
	 */
	@Override
	public List<UserInfo> selectUserInfoList(UserInfo userInfo) {
		return userInfoMapper.selectUserInfoList(userInfo);
	}

	/**
	 * 查询下级团队用户
	 *
	 * @param userInfo 用户信息
	 * @return 用户信息集合
	 */
	@Override
	public List<UserInfo> selectChildUserInfoList(UserInfo userInfo) {
		return userInfoMapper.selectChildUserInfoList(userInfo);
	}

	/**
	 * 查询下级团队用户
	 *
	 * @param userId
	 * @return
	 */
	@Override
	public List<Long> getChildUserIdList(Long userId) {
		return userInfoMapper.getChildUserIdList(userId);
	}

	/**
	 * 统计今团队用户数
	 * @param userId
	 * @return
	 */
	@Override
	public List<TeamUsersBo> countTodayNewTeamUsers(Long userId) {
		return userInfoMapper.countTodayNewTeamUsers(userId);
	}


	/**
	 * 更新用戶
	 *
	 * @param req
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public AjaxResult updateUserInfo(UserInfoReqBo req) {
		UserInfo queryUserInfo = userInfoService.lambdaQuery().eq(UserInfo::getUserId, req.getUserId()).one();
		if (queryUserInfo == null) {
			throw new ServiceException("用户不存在");
		}

		UserInfo updateUser = new UserInfo();

		updateUser.setMinGameLevel(req.getMinGameLevel());
		updateUser.setUserId(req.getUserId());
		//账号状态
		updateUser.setStatus(req.getStatus());
		//提现状态
		updateUser.setWithdrawalOpenOrClose(req.getWithdrawalOpenOrClose());
		//是否服务中心角色
		updateUser.setHasServiceCenter(req.getHasServiceCenter());
		//验证码验证
		sysUserService.pubValidate(req.getAutoCode());

		int i = this.userInfoMapper.updateById(updateUser);
		String key1 = ConstantStatic.USER_STATUS + queryUserInfo.getUserId() + ":";
		xmsRedis.del(key1);
		xmsRedis.del(key1);
		redissonTemplate.sendCleanCacheWithDelay(key1);
		if (i > 0) {
			return AjaxResult.success();
		} else {
			return AjaxResult.error();
		}
	}
	/**
	 * 查询网体-树结构方法
	 * @param userId
	 * @return
	 */
	@Override
	public List<Tree<Long>> queryNetBody1(String userId) {
		UserInfo currentUser = null;
		if(StrUtil.isBlank(userId)){
			 currentUser = userInfoMapper.selectById(1000L);
		}else{
			currentUser = userInfoService.lambdaQuery()
				.eq(UserInfo::getAccount, userId)
				.one();
		}

		if (currentUser == null) {
			throw new ServiceException("查询的用户不存在");
		}
		Map<Integer, String> levelMap = this.userInfoMapper.selectDictDataByType("t_user_info_game_level")
			.stream()
			.collect(Collectors.toMap(
				data -> Integer.parseInt(data.getDictValue()),
				SysDictData::getDictLabel,
				(existing, replacement) -> existing  // 或选择一个策略来处理冲突
			));

		Map<Integer, String> nodeLevelMap = this.userInfoMapper.selectDictDataByType("t_node_plan_node_level")
			.stream()
			.collect(Collectors.toMap(
				data -> Integer.parseInt(data.getDictValue()),
				SysDictData::getDictLabel,
				(existing, replacement) -> existing  // 或选择一个策略来处理冲突
			));
		List<UserInfo> userList =  userInfoMapper.queryNetBodyChildUser(currentUser.getUserId());

		if(CollectionUtil.isEmpty(userList)){
			currentUser.setParentId(currentUser.getInviteUserId());
			userList.addFirst(currentUser);
			return TreeBuildUtils.build(userList, ((user, tree) -> {
				tree.setId(user.getUserId());
				tree.setParentId(user.getInviteUserId());
				tree.setName(user.getAccount());
				//钱包地址
				tree.putExtra("account", user.getAccount());
				//等级
				tree.putExtra("level", levelMap.get(user.getGameLevel()>user.getMinGameLevel()?user.getGameLevel():user.getMinGameLevel()));
				//是否有效用户(0.否 1.是)
				tree.putExtra("isValid", user.getIsValid());
				//是否服务中心身份 0:否,1:是
				tree.putExtra("hasServiceCenter", user.getHasServiceCenter());
				//我的业绩(购买矿机)
				tree.putExtra("performance", user.getPerformance());
				//团队业绩(购买矿机)
				tree.putExtra("umbrellaPerformance", user.getUmbrellaPerformance());
				//小区业绩(购买矿机)
				tree.putExtra("communityPerformance", user.getCommunityPerformance());
				//直推用户
				tree.putExtra("subNum", user.getSubNum());
				//团队用户数
				tree.putExtra("umbrellaNum", user.getUmbrellaNum());
			}));
		}
		userList.addFirst(currentUser);
		for (UserInfo userInfo : userList) {
			userInfo.setParentId(userInfo.getInviteUserId());
		}
		return TreeBuildUtils.build(userList, ((user, tree) -> {
			tree.setId(user.getUserId());
			tree.setParentId(user.getParentId());
			tree.setName(user.getAccount());
			//钱包地址
			tree.putExtra("account", user.getAccount());
			//等级
			tree.putExtra("level", levelMap.get(user.getGameLevel()>user.getMinGameLevel()?user.getGameLevel():user.getMinGameLevel()));
			//是否有效用户(0.否 1.是)
			tree.putExtra("isValid", user.getIsValid());
			//是否服务中心身份 0:否,1:是
			tree.putExtra("hasServiceCenter", user.getHasServiceCenter());
			//我的业绩(购买矿机)
			tree.putExtra("performance", user.getPerformance());
			//团队业绩(购买矿机)
			tree.putExtra("umbrellaPerformance", user.getUmbrellaPerformance());
			//小区业绩(购买矿机)
			tree.putExtra("communityPerformance", user.getCommunityPerformance());
			//直推用户
			tree.putExtra("subNum", user.getSubNum());
			//团队用户数
			tree.putExtra("umbrellaNum", user.getUmbrellaNum());
		}));
	}

	@Override
	public UserInfo getById(Long userId) {
		return userInfoService.lambdaQuery()
			.eq(UserInfo::getUserId, userId)
			.one();
	}

}
