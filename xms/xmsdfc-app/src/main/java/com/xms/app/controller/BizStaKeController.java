package com.xms.app.controller;

import com.xms.app.entity.dto.MyReleaseBucketListDto;
import com.xms.app.entity.dto.MyStakeInfoDto;
import com.xms.app.entity.dto.MyStakeInfoListDto;
import com.xms.app.entity.dto.StakeInfoDTO;
import com.xms.app.entity.vo.CreateStakeOrderVo;
import com.xms.app.service.BizMiningService;
import com.xms.app.service.BizStakeService;
import com.xms.common.annotation.RepeatSubmit;
import com.xms.common.core.domain.api.ResultPista;
import com.xms.common.utils.SecurityUtils;
import com.xms.dao.service.XmsCommonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 质押相关 前端控制器
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
	 * @param coinType 质押币种：2=DFC，3=OORT；不传默认OORT
	 * @return
	 * @throws Exception
	 */
	@ApiOperation(value = "获取质押信息")
	@GetMapping(value = "/stakeInfo")
	public ResultPista<StakeInfoDTO> stakeInfo(Integer coinType) throws Exception {
		return ResultPista.data(bizStakeService.stakeInfo(coinType));
	}

	/**
	 * 发起质押，下单时会记录对应币种价格快照：DFC记录dfcPriceUsdt，OORT记录oortPriceUsdt
	 * @param req 质押下单参数，id为质押套餐ID，num为购买份数
	 * @return
	 * @throws Exception
	 */
	@ApiOperation(value = "发起质押")
	@PostMapping(value = "/stakeOrder")
	@RepeatSubmit
	public ResultPista stakeOrder(@Valid @RequestBody CreateStakeOrderVo req) throws Exception {
//		ResultPista resultPista = xmsCommonServiceImpl.checkMineSettleTime(1);
//		if (!resultPista.isSuccess()) {
//			throw new ServiceException(resultPista.getMsg());
//		}
		return bizStakeService.stakeOrder(req, SecurityUtils.getLoginAppUser().getUserId());
	}

	/**
	 * 我的质押信息 今日产出、总锁仓、未到期质押份数、今日锁仓释放数
	 * @param coinType 质押币种：2=DFC，3=OORT；不传默认OORT
	 * @return
	 * @throws Exception
	 */
	@ApiOperation(value = "我的质押信息")
	@GetMapping(value = "/myStakeInfo")
	public ResultPista<MyStakeInfoDto> myStakeInfo(Integer coinType) throws Exception {
		return ResultPista.data(bizStakeService.myStakeInfo(coinType));
	}

	/**
	 * 质押订单记录列表
	 * @param lastId 最后一条记录ID
	 * @param coinType 质押币种：2=DFC，3=OORT；不传默认OORT
	 * @return
	 * @throws Exception
	 */
	@ApiOperation(value = "质押订单记录")
	@GetMapping(value = "/myStakeInfoList")
	public ResultPista<List<MyStakeInfoListDto>> destroyOrderList(Long lastId, Integer coinType) throws Exception {
		return ResultPista.data(bizStakeService.destroyOrderList(lastId, coinType));
	}

	/**
	 * 锁仓订单记录列表
	 * @param lastId 最后一条记录ID
	 * @param coinType 质押币种：2=DFC，3=OORT；不传默认OORT
	 * @return
	 * @throws Exception
	 */
	@ApiOperation(value = "锁仓订单记录列表")
	@GetMapping(value = "/myReleaseBucketList")
	public ResultPista<List<MyReleaseBucketListDto>> myReleaseBucketList(Long lastId, Integer coinType) throws Exception {
		return ResultPista.data(bizStakeService.myReleaseBucketList(lastId, coinType));
	}
}
