package com.xms.app.service;

import com.xms.app.entity.dto.MyReleaseBucketListDto;
import com.xms.app.entity.dto.MyStakeInfoDto;
import com.xms.app.entity.dto.MyStakeInfoListDto;
import com.xms.app.entity.dto.StakeInfoDTO;
import com.xms.app.entity.vo.CreateStakeOrderVo;
import com.xms.common.core.domain.api.ResultPista;
import jakarta.validation.Valid;

import java.util.List;

public interface BizStakeService {

	/**
	 * 获取质押信息 没有上架的话可能为空
	 * @return
	 */
	StakeInfoDTO stakeInfo();

	/**
	 * 质押
	 * @return
	 */
	ResultPista stakeOrder(CreateStakeOrderVo req, Long userId);

	/**
	 * 我的质押信息
	 * @return
	 */
	MyStakeInfoDto myStakeInfo();

	/**
	 * 质押订单列表
	 * @param lastId
	 * @return
	 */
	List<MyStakeInfoListDto> destroyOrderList(Long lastId);

	/**
	 * 锁仓订单列表
	 * @param lastId
	 * @return
	 */
	List<MyReleaseBucketListDto> myReleaseBucketList(Long lastId);
}
