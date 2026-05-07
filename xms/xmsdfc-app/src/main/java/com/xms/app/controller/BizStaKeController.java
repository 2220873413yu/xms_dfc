package com.xms.app.controller;

import com.github.pagehelper.PageInfo;
import com.xms.app.entity.dto.*;
import com.xms.app.entity.resp.CreateOrderResp;
import com.xms.app.entity.vo.CreateMiningOrderVo;
import com.xms.app.entity.vo.CreateStakeOrderVo;
import com.xms.app.entity.vo.StakeMiningOrderVo;
import com.xms.app.service.BizMiningService;
import com.xms.app.service.BizStakeService;
import com.xms.common.annotation.RepeatSubmit;
import com.xms.common.core.domain.api.ResultPista;
import com.xms.common.utils.SecurityUtils;
import com.xms.dao.entity.dto.DestroyOrderDto;
import com.xms.dao.service.XmsCommonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 质押相关 前端控制器
 *
 *
 * @since 2023-06-12
 */
@Api(tags = "质押相关")
@RestController
@RequestMapping("/api/stake")
public class BizStaKeController {

	@Autowired
	private BizStakeService bizStakeService;

	@Autowired
	private XmsCommonService xmsCommonServiceImpl;

	@Autowired
	private BizMiningService bizMiningService;


	/**
	 * 获取质押信息 没有上架的话可能为空
	 * @return
	 * @throws Exception
	 */
	@ApiOperation(value = "获取质押信息")
	@GetMapping(value = "/stakeInfo")
	public ResultPista<StakeInfoDTO> stakeInfo()  throws Exception{
		return ResultPista.data(bizStakeService.stakeInfo());
	}


	/**
	 * 发起质押
	 * @return
	 */
	@ApiOperation(value = "发起质押")
	@PostMapping(value = "/stakeOrder")
	@RepeatSubmit
	public ResultPista stakeOrder(@Valid @RequestBody CreateStakeOrderVo req)  throws Exception{
//		ResultPista resultPista = xmsCommonServiceImpl.checkMineSettleTime(1);
//		if (!resultPista.isSuccess()) {
//			throw new ServiceException(resultPista.getMsg());
//		}
		return bizStakeService.stakeOrder(req,SecurityUtils.getLoginAppUser().getUserId());
	}


	/**
	 * 我的质押信息 今日产出、总锁仓、未到期质押份数、今日锁仓释放数
	 * @return
	 * @throws Exception
	 */
	@ApiOperation(value = "我的质押信息")
	@GetMapping(value = "/myStakeInfo")
	public ResultPista<MyStakeInfoDto> myStakeInfo()  throws Exception{
		return ResultPista.data(bizStakeService.myStakeInfo());
	}


	/**
	 * 质押订单记录列表
	 * @param lastId
	 * @return
	 * @throws Exception
	 */
	@ApiOperation(value = "质押订单记录")
	@GetMapping(value = "/myStakeInfoList")
	public ResultPista<List<MyStakeInfoListDto>> destroyOrderList(Long lastId)  throws Exception{
		return ResultPista.data(bizStakeService.destroyOrderList(lastId));
	}

	/**
	 * 锁仓订单记录列表
	 * @param lastId
	 * @return
	 * @throws Exception
	 */
	@ApiOperation(value = "锁仓订单记录列表")
	@GetMapping(value = "/myReleaseBucketList")
	public ResultPista<List<MyReleaseBucketListDto>> myReleaseBucketList(Long lastId)  throws Exception{
		return ResultPista.data(bizStakeService.myReleaseBucketList(lastId));
	}

}
