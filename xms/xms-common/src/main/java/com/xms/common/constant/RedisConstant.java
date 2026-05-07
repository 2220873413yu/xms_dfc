package com.xms.common.constant;

import cn.hutool.core.util.RandomUtil;

/**
 * @createDate: 2023/7/27
 */
public interface RedisConstant {

	/**
	 * 缓存前缀
	 */
	String REDIS_PREFIX = "aleo:renegade:";

	String SEPARATOR = "_";
	/**
	 * 1小时的过期时间 秒为单位
	 */
	Long SECONDS_EXPIRE_TIME = 3600L;

	/**
	 * 10秒钟过期时间
	 */
	Long FIFTEEN_SECONDS_EXPIRE_TIME = 15L;

	/**
	 * 2天的过期时间 秒为单位
	 */
	Long TWO_DAYS_EXPIRE_TIME = 172800L;

	// 订单过期时间:30天
	Long ORDER_DELIVERY_UNPAY = 30 * 24 * 60 * 60L;

	/**
	 * 30天过期时间，单位：day
	 */
	Long DAY_EXPIRE_TIME = 30L + RandomUtil.randomLong(10L);

	/**
	 * rabbit消息前缀
	 */
	String USER_MONEY = REDIS_PREFIX + "user:money:";
	String XMS_PARAM = REDIS_PREFIX + "param:";
	/**
	 * 获取上级用户，包含自己的
	 */
	String USER_PARENT_ME_LIST = REDIS_PREFIX + "user_parent_me_list:";
	/**
	 * 获取上级用户 不包含自己，注册了不会在改变,userinfo返回体,缓存版
	 */
	String USER_PARENT_NOT_ME_LIST = REDIS_PREFIX + "user_parent_notme_list:";
	/**
	 * 获取上级用户 不包含自己，注册了不会在改变  UserRelation
	 */
	String USER_PARENT_NOME_LIST = REDIS_PREFIX + "user_parent_nome_list:";

	String XMS_USER_LEVEL_MINING = REDIS_PREFIX + "xms_user_level_mining:";
	String XMS_SYS_BANNER = REDIS_PREFIX + "sys_banner:";
	String USER_REGISTER_GROUP = REDIS_PREFIX + "user_register_group:";
	String USER_RECHARGE_GROUP = REDIS_PREFIX + "user_recharge_group:";
	String USER_WITHDRAW_GROUP = REDIS_PREFIX + "user_withdraw_group:";
	String USER_TRANSFER_GROUP = REDIS_PREFIX + "user_transfer_group:";
	String USER_ORDER_GROUP = REDIS_PREFIX + "user_order_group:";
	String USER_REWARD_GROUP = REDIS_PREFIX + "user_reward_group:";

	//获取平台币从现在到今天的数据
	String PTB_PRICE_KEY = REDIS_PREFIX + "ptb_price_key:";

	/**
	 * 获取可用的谷歌邮箱列表
	 */
	String GOOGLE_EMAIL_LIST = REDIS_PREFIX + "google:email:list";

	/**
	 * 邮箱验证码
	 */
	String CAPTCHA_SMS = REDIS_PREFIX + "user:captcha:sms:";

	/**
	 * oort价格缓存
	 */
	String CAPTCHA_OORT_PRICE = REDIS_PREFIX + "oort:price:";

	/**
	 * 分布式锁前缀名集合
	 */
	interface LockConstant {
		/**
		 * 分布式锁前缀
		 */
		String REDIS_LOCK = REDIS_PREFIX + "lock:";

		/**
		 * 处理流水日志
		 */
		String CANAL_MSG_IDEMPOTENT = REDIS_LOCK + "canal:msg:flow:idempotent";
		String USER_LOGIN = REDIS_LOCK + "login";
		String XMS_WITHDRAW_APPLY = REDIS_LOCK + "xmsWithdrawApply";


		/**
		 * 加速释放订单的时候用到的
		 */
		String XMS_RELEASE_ORDER = REDIS_LOCK + "xmsReleaseOrder";
		String XMS_TRANSFER_APPLY = REDIS_LOCK + "xmsTransferApply";
		/**
		 * 购买矿机
		 */
		String XMS_BUY_MINING_APPLY = REDIS_LOCK + "xmsBuyMiningApply";

		/**
		 * 质押
		 */
		String XMS_STAKE_APPLY = REDIS_LOCK + "xmsStakeApply";
		//领取基金收益的场景下用到
		String XMS_CLAIM_FUND_PROFIT = REDIS_LOCK + "xmsClaimFundProfit";

		//获取充值地址信息 加锁
		String XMS_RECHARGE_INFO = REDIS_LOCK + "xmsRechargeInfo";
		/**
		 * 领取矿机利息
		 */
		String XMS_CLAIM_REWARD_APPLY = REDIS_LOCK + "xmsClaimRewardApply";
		/**
		 * 领取余额宝
		 */
		String XMS_CLAIM_YU_E_BAO_REWARD_APPLY = REDIS_LOCK + "xmsClaimYuEBaoRewardApply";

		/**
		 * 领取本金
		 */
		String XMS_CLAIM_PRINCIPAL_REWARD_APPLY = REDIS_LOCK + "xmsClaimPrincipalRewardApply";

		/**
		 * 领取矿机订单奖励
		 */
		String XMS_CLAIM_MINING_REWARD_APPLY = REDIS_LOCK + "xmsClaimMiningRewardApply";
		String XMS_USER_EXCHANGE_APPLY = REDIS_LOCK + "xmsUserExchangeApply";

		String XMS_WITHDRAW_CHECK = REDIS_LOCK + "xmsWithdrawCheck:";
		String OPERATE_CONTRACT_CONFIG = REDIS_LOCK + "operateContractConfig:";
		String XMS_BUY_MINING = REDIS_LOCK + "XMS_BUY_MINING:";
	}


	/**
	 * stream Msg 消息队列名集合
	 */
	interface StreamMsgConstant {
		/**
		 * 分布式锁前缀
		 */
		String REDIS_STREAM = REDIS_PREFIX + "stream:";
		/**
		 * 死信
		 */
		String XMS_DEAD_MSG = REDIS_STREAM + "msg:dead:";

		/**
		 * canal中间件
		 */
		String CANAL_MSG = REDIS_STREAM + "canal:msg:flow";

		/**
		 * rabbit消息前缀
		 */
		String RABBIT_MQ_USER = REDIS_PREFIX + "user:";

		/**
		 * 处理购买矿机的订单数据
		 */
		String ORDER_DYNAMIC_SETTLEMENT = REDIS_STREAM + "transfer:order:dynamic:settlement";

		String DELAY_DEL_CACHE = REDIS_STREAM + "cacheDelQueue";

		String XMS_ASYNC_REWARD = REDIS_STREAM + "XMS_ASYNC_REWARD";

		/**
		 * 获取基金订单延时到账
		 */
		String  DELAY_ORDER_TIMEOUT_QUEUE = REDIS_STREAM + "DELAY_ORDER_TIMEOUT_QUEUE";
	}

	/**
	 * redis DB 处理
	 */
	interface DbConstant {
		/**
		 * redis db 锁前缀
		 */
		String REDIS_DB = REDIS_PREFIX + "db:";
	}
}
