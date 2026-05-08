<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryForm" :inline="true" :model="queryParams" label-width="90px" size="small">
      <el-form-item label="质押币种" prop="coinType">
        <el-select v-model="queryParams.coinType" clearable placeholder="请选择">
          <el-option v-for="dict in dict.type.biz_stake_coin_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否上架" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择">
          <el-option v-for="dict in dict.type.t_user_info_is_valid" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button icon="el-icon-search" size="mini" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :show-search.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="stakeProductList">
      <el-table-column v-if="false" align="center" label="ID" prop="id" />
      <el-table-column align="center" label="质押币种" prop="coinType" width="100">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.biz_stake_coin_type" :value="scope.row.coinType" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="销量" prop="sales" />
      <el-table-column align="center" label="可用库存" prop="availableStock" />
      <el-table-column align="center" label="质押数量" prop="stakeUnitAmount" />
      <el-table-column align="center" label="额外质押USDT" prop="extraStakeValueUsdt" width="130" />
      <el-table-column align="center" label="每天产出" prop="dayReward" />
      <el-table-column align="center" label="订单有效期" prop="validDays" />
      <el-table-column align="center" label="是否上架" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.t_user_info_is_valid" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column align="center" label="创建时间" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" class-name="small-padding fixed-width" label="操作" width="100">
        <template slot-scope="scope">
          <el-button v-hasPermi="['xms:stakeProduct:edit']" icon="el-icon-edit" size="mini" type="text" @click="handleUpdate(scope.row)">修改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :limit.sync="queryParams.pageSize" :page.sync="queryParams.pageNum" :total="total" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" append-to-body width="520px">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="质押数量" prop="stakeUnitAmount">
          <el-input v-model="form.stakeUnitAmount" @input="value => handleDecimalInput('stakeUnitAmount', value)" />
        </el-form-item>
        <el-form-item v-if="!isDfcProduct" label="额外质押USDT" prop="extraStakeValueUsdt">
          <el-input v-model="form.extraStakeValueUsdt" @input="value => handleDecimalInput('extraStakeValueUsdt', value)" />
        </el-form-item>
        <el-form-item v-if="isDfcProduct" label="可用库存" prop="availableStock">
          <el-input-number v-model="form.availableStock" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="每天产出" prop="dayReward">
          <el-input v-model="form.dayReward" @input="value => handleDecimalInput('dayReward', value)" />
        </el-form-item>
        <el-form-item label="订单有效期" prop="validDays">
          <el-input-number v-model="form.validDays" :disabled="true" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="是否上架" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.t_user_info_is_valid" :key="dict.value" :label="parseInt(dict.value)">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确定</el-button>
        <el-button @click="cancel">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listStakeProduct, getStakeProduct, updateStakeProduct } from '@/api/xms/stakeProduct'

export default {
  name: 'StakeProduct',
  dicts: ['t_user_info_is_valid', 'biz_stake_coin_type'],
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      stakeProductList: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        coinType: null,
        status: null
      },
      form: {},
      rules: {
        stakeUnitAmount: [{ required: true, message: '请输入质押数量', trigger: 'blur' }],
        extraStakeValueUsdt: [{ required: true, message: '请输入额外质押USDT', trigger: 'blur' }],
        availableStock: [{ required: true, message: '请输入可用库存', trigger: 'blur' }],
        dayReward: [{ required: true, message: '请输入每天产出', trigger: 'blur' }],
        validDays: [{ required: true, message: '请输入订单有效期', trigger: 'blur' }],
        status: [{ required: true, message: '请选择是否上架', trigger: 'change' }]
      }
    }
  },
  computed: {
    isDfcProduct() {
      return parseInt(this.form.coinType) === 2
    }
  },
  created() {
    this.getList()
  },
  methods: {
    handleDecimalInput(field, value) {
      this.form[field] = this.normalizeDecimal(value)
    },
    normalizeDecimal(value) {
      if (value === null || value === undefined) return ''
      const raw = String(value).replace(/[^\d.]/g, '')
      if (!raw) return ''
      const parts = raw.split('.')
      const intPart = (parts[0] || '0').replace(/^0+(?=\d)/, '') || '0'
      const decimalPart = (parts[1] || '').slice(0, 8)
      return raw.endsWith('.') ? `${intPart}.` : decimalPart ? `${intPart}.${decimalPart}` : intPart
    },
    getList() {
      this.loading = true
      listStakeProduct(this.queryParams).then(response => {
        this.stakeProductList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: null,
        coinType: 3,
        rewardCoinType: 3,
        availableStock: null,
        stakeUnitAmount: null,
        extraStakeValueUsdt: '0',
        dayReward: null,
        linearDays: 270,
        validDays: null,
        status: 1
      }
      this.resetForm('form')
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleUpdate(row) {
      this.reset()
      getStakeProduct(row.id).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改质押套餐'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        if (this.isDfcProduct && (this.form.availableStock === null || this.form.availableStock === undefined || this.form.availableStock === '')) {
          this.$modal.msgError('请输入可用库存')
          return
        }
        updateStakeProduct(this.form).then(() => {
          this.$modal.msgSuccess('修改成功')
          this.open = false
          this.getList()
        })
      })
    }
  }
}
</script>
