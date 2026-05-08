<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryForm" :inline="true" :model="queryParams" label-width="90px" size="small">
      <el-form-item label="用户ID" prop="userId">
        <el-input v-model="queryParams.userId" clearable placeholder="请输入用户ID" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="订单号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" clearable placeholder="请输入订单号" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="质押币种" prop="coinType">
        <el-select v-model="queryParams.coinType" clearable placeholder="请选择">
          <el-option v-for="dict in dict.type.biz_stake_coin_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择">
          <el-option v-for="dict in dict.type.t_stake_order_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker v-model="daterangeCreateTime" end-placeholder="结束日期" range-separator="-" start-placeholder="开始日期" style="width: 240px" type="datetimerange" value-format="yyyy-MM-dd" />
      </el-form-item>
      <el-form-item>
        <el-button icon="el-icon-search" size="mini" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button v-hasPermi="['xms:stakeOrder:export']" icon="el-icon-download" plain size="mini" type="warning" @click="handleExport">导出</el-button>
      </el-col>
      <right-toolbar :show-search.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="stakeOrderList">
      <el-table-column v-if="false" align="center" label="ID" prop="id" />
      <el-table-column align="center" label="用户ID" prop="userId" width="90" />
      <el-table-column align="center" label="产品ID" prop="productId" width="90" />
      <el-table-column align="center" label="订单号" prop="orderNo" width="180" />
      <el-table-column align="center" label="质押币种" prop="coinType" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.biz_stake_coin_type" :value="scope.row.coinType" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="产出币种" prop="rewardCoinType" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.biz_stake_reward_coin_type" :value="scope.row.rewardCoinType" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="购买份数" prop="quantity" />
      <el-table-column align="center" label="质押本金" prop="stakeAmount" width="120" />
      <el-table-column align="center" label="质押OORT" prop="stakeOortAmount" width="120" />
      <el-table-column align="center" label="额外USDT" prop="extraValueUsdt" width="110" />
      <el-table-column align="center" label="下单价格" width="140">
        <template slot-scope="scope">
          <span>{{ orderPrice(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="每日产出" prop="dayReward" width="110" />
      <el-table-column align="center" label="理论总产出" prop="totalYieldTarget" width="120" />
      <el-table-column align="center" label="已产出" prop="yieldedAmount" width="110" />
      <el-table-column align="center" label="订单状态" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.t_stake_order_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="退本状态" prop="principalRefundStatus">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.biz_principal_refund_status" :value="scope.row.principalRefundStatus" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="退本时间" prop="principalRefundTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.principalRefundTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="有效期" prop="validDays" />
      <el-table-column align="center" label="剩余期" prop="haveDays" />
      <el-table-column align="center" label="创建时间" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :limit.sync="queryParams.pageSize" :page.sync="queryParams.pageNum" :total="total" @pagination="getList" />
  </div>
</template>

<script>
import { listStakeOrder } from '@/api/xms/stakeOrder'

export default {
  name: 'StakeOrder',
  dicts: ['t_stake_order_status', 'biz_stake_coin_type', 'biz_stake_reward_coin_type', 'biz_principal_refund_status'],
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      stakeOrderList: [],
      daterangeCreateTime: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        orderNo: null,
        coinType: null,
        status: null,
        createTime: null
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      this.queryParams.params = {}
      if (this.daterangeCreateTime && this.daterangeCreateTime.length) {
        this.queryParams.params.beginCreateTime = this.daterangeCreateTime[0]
        this.queryParams.params.endCreateTime = this.daterangeCreateTime[1]
      }
      listStakeOrder(this.queryParams).then(response => {
        this.stakeOrderList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.daterangeCreateTime = []
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleExport() {
      this.download('xms/stakeOrder/export', { ...this.queryParams }, `stakeOrder_${new Date().getTime()}.xlsx`)
    },
    orderPrice(row) {
      const isDfc = Number(row.coinType) === 2
      const price = isDfc ? row.dfcPriceUsdt : row.oortPriceUsdt
      const symbol = isDfc ? 'DFC' : 'OORT'
      return price || price === 0 ? `${price} ${symbol}/USDT` : '-'
    }
  }
}
</script>
