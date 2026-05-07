package com.xms.common.constant;

/**
 *
  * @ClassName: ConstantType
  * @Description: 常量类
  *
  * @date 2023年5月23日 下午3:26:14
  *
 */
public class ConstantType {

	public static String DYNAMIC_USER_RECHARGE_TYPE = "t_recharge_coin_type";

	//开关 1-否 2-是
	public class open_or_close{
		public static final int type_1 = 1;
		public static final int type_2 = 2;
	}

	//状态(1.正常 2.冻结)
	public class user_info_status{
		public static final int type_1 = 1;
		public static final int type_2 = 2;
	}

	//等级(0.无 1.V1 2.V2 3.V3 4.V4 5.V5 6.V6)
	public class user_info_game_level{
		public static final int type_0 = 0;
		public static final int type_1 = 1;
		public static final int type_2 = 2;
		public static final int type_3 = 3;
		public static final int type_4 = 4;
		public static final int type_5 = 5;
		public static final int type_6 = 6;
	}

	//奖励等级(1.V1 2.V2 3.V3 4.V4 5.V5 6.V6)
	public class user_level_reward_level{
		public static final int type_1 = 1;
		public static final int type_2 = 2;
		public static final int type_3 = 3;
		public static final int type_4 = 4;
		public static final int type_5 = 5;
		public static final int type_6 = 6;
	}

	//币种1:USDT,2:DFC,3:OORT,4:锁定USDT,5:产出DFC,6:代理分红收益,7:运营收益
	public class user_money_coin_type {
		public static final int type_1 = 1;
		public static final int type_2 = 2;
		public static final int type_3 = 3;
		public static final int type_4 = 4;
		public static final int type_5 = 5;
		public static final int type_6 = 6;
		public static final int type_7 = 7;
		public static final int type_8 = 8;
		public static final int type_9 = 9;
	}

	/**
	 *
	 * 1:矿机收益,2:平台管理费,3:服务中心费,4:矿机管理费奖励直推,5:矿机管理费间推奖励,
	 * 6:管理费极差奖励,7:管理费全国代理平级奖,8:oort质押立即释放,9:oort线性释放,
	 * 10:购买矿机直推奖励,11:购买矿机间推奖励,12:市代均分奖励,13:省代均分奖励,
	 * 14:全国代均分奖励,15:质押直推奖励,16:质押间推奖励
	 *
	 */
	public class xms_reward_record_source_type{
		public static final int type_1 = 1;
		public static final int type_2 = 2;
		public static final int type_3 = 3;
		public static final int type_4 = 4;
		public static final int type_5 = 5;
		public static final int type_6 = 6;
		public static final int type_7 = 7;
		public static final int type_8 = 8;
		public static final int type_9 = 9;
		public static final int type_10 = 10;
		public static final int type_11 = 11;
		public static final int type_12 = 12;
		public static final int type_13 = 13;
		public static final int type_14 = 14;
		public static final int type_15 = 15;
		public static final int type_16 = 16;
		public static final int type_17 = 17;
		public static final int type_18 = 18;
		public static final int type_19 = 19;
		public static final int type_20 = 20;
		public static final int type_21 = 21;
		public static final int type_22 = 22;
		public static final int type_23 = 23;
		public static final int type_24 = 24;
		public static final int type_25 = 25;
		public static final int type_26 = 26;
	}

	/**
	 * 奖金业务类型 1.购买卡包,2:直推奖励(算力),3:间推奖励(算力),4:升级补算力,5:购买卡包赠送,6:升级卡包赠送
	 *
	 */
	public class xms_reward_record_business_type{
		public static final int type_1 = 1;
		public static final int type_2 = 2;
		public static final int type_3 = 3;
		public static final int type_4 = 4;
		public static final int type_5 = 5;
		public static final int type_6 = 6;
		public static final int type_7 = 7;
	}

	/**
	 * 奖金币种类型 1:算力,2:usdt,3:BDAI
	 */
	public class reward_record_coin_type{
		public static final int type_1 = 1;
		public static final int type_2 = 2;
		public static final int type_3 = 3;
	}

	/**
	 * 1.充值,2:转账扣款,3:转账接收,4:提现,5:提现驳回,6:购买矿机,7:df划转,8:购买矿机直推奖励,9:购买矿机间推奖励
	 * 10:市代均分奖励,11:省代均分奖励,12:全国代均分奖励,13:质押,14:质押直推奖励,15:质押间推奖励,
	 * 16:矿机奖励,17:服务中心费,18:管理费直推奖励,19:管理费间推奖励,20:管理费极差奖励,21:质押扣款(oort),
	 * 22:oort立即释放,23:oort线性释放,24:管理费平级奖励,25:管理费全国代理平级奖,26:闪兑扣除,27:闪兑增加,99:系统扣除
	 * 28:平台拨扣
	 */
	public class user_money_log_source_type{
		public static final int type_99 = 99;
		public static final int type_1 = 1;
		public static final int type_2 = 2;
		public static final int type_3 = 3;
		public static final int type_4 = 4;
		public static final int type_5 = 5;
		public static final int type_6 = 6;
		public static final int type_7 = 7;
		public static final int type_8 = 8;
		public static final int type_9 = 9;
		public static final int type_10 = 10;
		public static final int type_11 = 11;
		public static final int type_12 = 12;
		public static final int type_13 = 13;
		public static final int type_14 = 14;
		public static final int type_15 = 15;
		public static final int type_16 = 16;
		public static final int type_17 = 17;
		public static final int type_18 = 18;
		public static final int type_19 = 19;

		public static final int type_20 = 20;
		public static final int type_21 = 21;
		public static final int type_22 = 22;
		public static final int type_23 = 23;
		public static final int type_24 = 24;
		public static final int type_25 = 25;
		public static final int type_26 = 26;
		public static final int type_27 = 27;
		public static final int type_28 = 28;
		public static final int type_29 = 29;
		public static final int type_30 = 30;
		public static final int type_32 = 32;
		public static final int type_44 = 44;
	}

	//状态(0.待审核,1.审核成功,2.审核驳回,3.提现成功,4.打款失败)
	public class withdrawal_status{
		public static final int type_0 = 0;
		public static final int type_1 = 1;
		public static final int type_2 = 2;
		public static final int type_3 = 3;
		public static final int type_4 = 4;
	}
}
