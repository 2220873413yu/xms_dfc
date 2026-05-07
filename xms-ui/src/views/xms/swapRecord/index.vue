<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryForm" :inline="true" :model="queryParams" label-width="68px" size="small">
      <el-form-item label="闪兑单号" label-width="120px" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          clearable
          placeholder="请输入闪兑单号"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>

      <el-form-item label="钱包地址" label-width="120px" prop="address">
        <el-input
          v-model="queryParams.userAccount"
          clearable
          placeholder="请输入钱包地址"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>

      <el-form-item label="用户ID" label-width="120px" prop="userId">
        <el-input
          v-model="queryParams.userId"
          clearable
          oninput="if(isNaN(value)) { value = null } else { value = value.replace('.', '') }"

          placeholder="请输入用户id"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="源币种类型" label-width="120px" prop="sourceCoinType">
        <el-select v-model="queryParams.sourceCoinType" clearable placeholder="请选择源币种类型">
          <el-option
            v-for="dict in dict.type.t_user_money_log_coin_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
<!--      <el-form-item label="源币种编码" prop="sourceCoinCode">
        <el-input
          v-model="queryParams.sourceCoinCode"
          placeholder="请输入源币种编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="目标币种类型 1:USDT" prop="targetCoinType">
        <el-select v-model="queryParams.targetCoinType" placeholder="请选择目标币种类型 1:USDT" clearable>
          <el-option
            v-for="dict in dict.type.t_user_money_log_coin_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="目标币种编码" prop="targetCoinCode">
        <el-input
          v-model="queryParams.targetCoinCode"
          placeholder="请输入目标币种编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="闪兑源数量" prop="sourceAmount">
        <el-input
          v-model="queryParams.sourceAmount"
          placeholder="请输入闪兑源数量"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="闪兑价格快照" prop="swapPrice">
        <el-input
          v-model="queryParams.swapPrice"
          placeholder="请输入闪兑价格快照"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手续费率快照" prop="feeRatio">
        <el-input
          v-model="queryParams.feeRatio"
          placeholder="请输入手续费率快照"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手续费金额" prop="feeAmount">
        <el-input
          v-model="queryParams.feeAmount"
          placeholder="请输入手续费金额"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="实际到账USDT" prop="targetNetAmount">
        <el-input
          v-model="queryParams.targetNetAmount"
          placeholder="请输入实际到账USDT"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>-->
      <el-form-item label="创建时间" label-width="120px">
        <el-date-picker
          v-model="daterangeCreateTime"
          end-placeholder="结束日期"
          range-separator="-"
          start-placeholder="开始日期"
          style="width: 240px"
          type="datetimerange"
          value-format="yyyy-MM-dd"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button icon="el-icon-search" size="mini" type="primary" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
<!--      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['xms:swapRecord:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['xms:swapRecord:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['xms:swapRecord:remove']"
        >删除</el-button>
      </el-col>-->
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['xms:swapRecord:export']"
          icon="el-icon-download"
          plain
          size="mini"
          type="warning"
          @click="handleExport"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="swapRecordList" @selection-change="handleSelectionChange">
      <el-table-column v-if="false" align="center" type="selection" width="55"/>
      <el-table-column v-if="false" align="center" label="主键id" prop="id"/>
      <el-table-column align="center" label="闪兑单号" prop="orderNo" />
      <el-table-column align="center" label="用户ID" prop="userId" />
      <el-table-column align="center" label="源币种类型" prop="sourceCoinType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.t_user_money_log_coin_type" :value="scope.row.sourceCoinType"/>
        </template>
      </el-table-column>
      <el-table-column align="center" label="目标币种类型" prop="targetCoinType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.t_user_money_log_coin_type" :value="scope.row.targetCoinType"/>
        </template>
      </el-table-column>
      <el-table-column align="center" label="闪兑源数量" prop="sourceAmount" />
      <el-table-column align="center" label="闪兑价格" prop="swapPrice" />
      <el-table-column align="center" label="手续费率" prop="feeRatio" >
        <template slot-scope="scope">
          {{scope.row.feeRatio}}%
        </template>
      </el-table-column>
      <el-table-column align="center" label="手续费金额" prop="feeAmount" />
      <el-table-column align="center" label="实际到账" prop="targetNetAmount" />
      <el-table-column align="center" label="创建时间" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
<!--      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['xms:swapRecord:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['xms:swapRecord:remove']"
          >删除</el-button>
        </template>
      </el-table-column>-->
    </el-table>

    <pagination
      v-show="total>0"
      :limit.sync="queryParams.pageSize"
      :page.sync="queryParams.pageNum"
      :total="total"
      @pagination="getList"
    />

    <!-- 添加或修改闪兑记录对话框 -->
    <el-dialog :title="title" :visible.sync="open" append-to-body width="500px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="闪兑单号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入闪兑单号" />
        </el-form-item>
        <el-form-item label="用户id" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户id" />
        </el-form-item>
        <el-form-item label="源币种类型 5:产出DFC,6:代理分红收益,7:运营收益" prop="sourceCoinType">
          <el-select v-model="form.sourceCoinType" placeholder="请选择源币种类型 5:产出DFC,6:代理分红收益,7:运营收益">
            <el-option
              v-for="dict in dict.type.t_user_money_log_coin_type"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="源币种编码" prop="sourceCoinCode">
          <el-input v-model="form.sourceCoinCode" placeholder="请输入源币种编码" />
        </el-form-item>
        <el-form-item label="目标币种类型 1:USDT" prop="targetCoinType">
          <el-select v-model="form.targetCoinType" placeholder="请选择目标币种类型 1:USDT">
            <el-option
              v-for="dict in dict.type.t_user_money_log_coin_type"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="目标币种编码" prop="targetCoinCode">
          <el-input v-model="form.targetCoinCode" placeholder="请输入目标币种编码" />
        </el-form-item>
        <el-form-item label="闪兑源数量" prop="sourceAmount">
          <el-input v-model="form.sourceAmount" placeholder="请输入闪兑源数量" />
        </el-form-item>
        <el-form-item label="闪兑价格快照" prop="swapPrice">
          <el-input v-model="form.swapPrice" placeholder="请输入闪兑价格快照" />
        </el-form-item>
        <el-form-item label="手续费率快照" prop="feeRatio">
          <el-input v-model="form.feeRatio" placeholder="请输入手续费率快照" />
        </el-form-item>
        <el-form-item label="手续费金额" prop="feeAmount">
          <el-input v-model="form.feeAmount" placeholder="请输入手续费金额" />
        </el-form-item>
        <el-form-item label="实际到账USDT" prop="targetNetAmount">
          <el-input v-model="form.targetNetAmount" placeholder="请输入实际到账USDT" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listSwapRecord, getSwapRecord, delSwapRecord, addSwapRecord, updateSwapRecord } from "@/api/xms/swapRecord";

export default {
  name: "SwapRecord",
  dicts: ['t_user_money_log_coin_type'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 闪兑记录表格数据
      swapRecordList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 备注时间范围
      daterangeCreateTime: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderNo: null,
        userId: null,
        sourceCoinType: null,
        userAccount: null,
        sourceCoinCode: null,
        targetCoinType: null,
        targetCoinCode: null,
        sourceAmount: null,
        swapPrice: null,
        feeRatio: null,
        feeAmount: null,
        targetNetAmount: null,
        createTime: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        orderNo: [
          { required: true, message: "闪兑单号不能为空", trigger: "blur" }
        ],
        userId: [
          { required: true, message: "用户id不能为空", trigger: "blur" }
        ],
        sourceCoinType: [
          { required: true, message: "源币种类型 5:产出DFC,6:代理分红收益,7:运营收益不能为空", trigger: "change" }
        ],
        sourceCoinCode: [
          { required: true, message: "源币种编码不能为空", trigger: "blur" }
        ],
        targetCoinType: [
          { required: true, message: "目标币种类型 1:USDT不能为空", trigger: "change" }
        ],
        targetCoinCode: [
          { required: true, message: "目标币种编码不能为空", trigger: "blur" }
        ],
        sourceAmount: [
          { required: true, message: "闪兑源数量不能为空", trigger: "blur" }
        ],
        swapPrice: [
          { required: true, message: "闪兑价格快照不能为空", trigger: "blur" }
        ],
        feeRatio: [
          { required: true, message: "手续费率快照不能为空", trigger: "blur" }
        ],
        feeAmount: [
          { required: true, message: "手续费金额不能为空", trigger: "blur" }
        ],
        targetNetAmount: [
          { required: true, message: "实际到账USDT不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询闪兑记录列表 */
    getList() {
      this.loading = true;
      this.queryParams.params = {};
      if (null != this.daterangeCreateTime && '' != this.daterangeCreateTime) {
        this.queryParams.params["beginCreateTime"] = this.daterangeCreateTime[0];
        this.queryParams.params["endCreateTime"] = this.daterangeCreateTime[1];
      }
      listSwapRecord(this.queryParams).then(response => {
        this.swapRecordList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        orderNo: null,
        userId: null,
        sourceCoinType: null,
        sourceCoinCode: null,
        targetCoinType: null,
        targetCoinCode: null,
        sourceAmount: null,
        swapPrice: null,
        feeRatio: null,
        feeAmount: null,
        targetNetAmount: null,
        createTime: null,
        updateTime: null,
        remark: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.daterangeCreateTime = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加闪兑记录";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getSwapRecord(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改闪兑记录";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateSwapRecord(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addSwapRecord(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除闪兑记录编号为"' + ids + '"的数据项？').then(function() {
        return delSwapRecord(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('xms/swapRecord/export', {
        ...this.queryParams
      }, `swapRecord_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
