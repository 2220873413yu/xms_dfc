<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryForm" :inline="true" :model="queryParams" label-width="68px" size="small">
      <el-form-item label="用户ID" label-width="120px" prop="userId">
        <el-input
          v-model="queryParams.userId"
          clearable
          placeholder="请输入用户ID"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="质押订单号" label-width="120px" prop="orderNo">
        <el-input
          v-model="queryParams.orderNo"
          clearable
          placeholder="请输入质押订单号"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
<!--      <el-form-item label="购买份数" prop="quantity">
        <el-input
          v-model="queryParams.quantity"
          placeholder="请输入购买份数"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="固定质押OORT数量=stake_unit_amount*quantity" prop="stakeOortAmount">
        <el-input
          v-model="queryParams.stakeOortAmount"
          placeholder="请输入固定质押OORT数量=stake_unit_amount*quantity"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="额外质押USDT等值=extra_stake_value_usdt*quantity" prop="extraValueUsdt">
        <el-input
          v-model="queryParams.extraValueUsdt"
          placeholder="请输入额外质押USDT等值=extra_stake_value_usdt*quantity"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="下单时OORT价格(USDT)" prop="oortPriceUsdt">
        <el-input
          v-model="queryParams.oortPriceUsdt"
          placeholder="请输入下单时OORT价格(USDT)"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="额外质押折算OORT数量=extra_value_usdt/oort_price_usdt" prop="extraStakeOortAmount">
        <el-input
          v-model="queryParams.extraStakeOortAmount"
          placeholder="请输入额外质押折算OORT数量=extra_value_usdt/oort_price_usdt"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="每日产出OORT=product.day_reward*quantity" prop="dayReward">
        <el-input
          v-model="queryParams.dayReward"
          placeholder="请输入每日产出OORT=product.day_reward*quantity"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="理论总产出OORT=day_reward*valid_days" prop="totalYieldTarget">
        <el-input
          v-model="queryParams.totalYieldTarget"
          placeholder="请输入理论总产出OORT=day_reward*valid_days"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="已产出OORT(累计)" prop="yieldedAmount">
        <el-input
          v-model="queryParams.yieldedAmount"
          placeholder="请输入已产出OORT(累计)"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>-->
      <el-form-item label="订单状态" label-width="120px" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择状态">
          <el-option
            v-for="dict in dict.type.t_stake_order_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
<!--      <el-form-item label="有效期天数" prop="validDays">
        <el-input
          v-model="queryParams.validDays"
          placeholder="请输入有效期天数"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="剩余有效期天数" prop="haveDays">
        <el-input
          v-model="queryParams.haveDays"
          placeholder="请输入剩余有效期天数"
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
      <!--     <el-col :span="1.5">
       <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['xms:stakeOrder:add']"
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
            v-hasPermi="['xms:stakeOrder:edit']"
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
            v-hasPermi="['xms:stakeOrder:remove']"
          >删除</el-button>
        </el-col>-->
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['xms:stakeOrder:export']"
          icon="el-icon-download"
          plain
          size="mini"
          type="warning"
          @click="handleExport"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="stakeOrderList" @selection-change="handleSelectionChange">
<!--      <el-table-column type="selection" width="55" align="center" />-->
      <el-table-column v-if="false" align="center" label="主键id" prop="id"/>
      <el-table-column align="center" label="用户ID" prop="userId" />
      <el-table-column align="center" label="订单号" prop="orderNo" />
      <el-table-column align="center" label="购买份数" prop="quantity" />
      <el-table-column align="center" label="固定质押OORT数量" prop="stakeOortAmount" />
      <el-table-column align="center" label="额外质押USDT价值" prop="extraValueUsdt" />
      <el-table-column align="center" label="OORT价格(USDT)" prop="oortPriceUsdt" />
      <el-table-column align="center" label="额外质押折算OORT数量" prop="extraStakeOortAmount" />
      <el-table-column align="center" label="每日产出OORT" prop="dayReward" />
      <el-table-column align="center" label="理论总产出OORT" prop="totalYieldTarget" />
      <el-table-column align="center" label="已产出OORT" prop="yieldedAmount" />
      <el-table-column align="center" label="订单状态" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.t_stake_order_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column align="center" label="有效期" prop="validDays" />
      <el-table-column align="center" label="剩余有效期" prop="haveDays" />
      <el-table-column align="center" label="创建时间" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>

      <el-table-column align="center" label="修改时间" prop="updateTime" >
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
<!--      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['xms:stakeOrder:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['xms:stakeOrder:remove']"
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

    <!-- 添加或修改质押订单对话框 -->
    <el-dialog :title="title" :visible.sync="open" append-to-body width="500px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户id" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户id" />
        </el-form-item>
        <el-form-item label="质押订单号(唯一)" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入质押订单号(唯一)" />
        </el-form-item>
        <el-form-item label="购买份数" prop="quantity">
          <el-input v-model="form.quantity" placeholder="请输入购买份数" />
        </el-form-item>
        <el-form-item label="固定质押OORT数量=stake_unit_amount*quantity" prop="stakeOortAmount">
          <el-input v-model="form.stakeOortAmount" placeholder="请输入固定质押OORT数量=stake_unit_amount*quantity" />
        </el-form-item>
        <el-form-item label="额外质押USDT等值=extra_stake_value_usdt*quantity" prop="extraValueUsdt">
          <el-input v-model="form.extraValueUsdt" placeholder="请输入额外质押USDT等值=extra_stake_value_usdt*quantity" />
        </el-form-item>
        <el-form-item label="下单时OORT价格(USDT)" prop="oortPriceUsdt">
          <el-input v-model="form.oortPriceUsdt" placeholder="请输入下单时OORT价格(USDT)" />
        </el-form-item>
        <el-form-item label="额外质押折算OORT数量=extra_value_usdt/oort_price_usdt" prop="extraStakeOortAmount">
          <el-input v-model="form.extraStakeOortAmount" placeholder="请输入额外质押折算OORT数量=extra_value_usdt/oort_price_usdt" />
        </el-form-item>
        <el-form-item label="每日产出OORT=product.day_reward*quantity" prop="dayReward">
          <el-input v-model="form.dayReward" placeholder="请输入每日产出OORT=product.day_reward*quantity" />
        </el-form-item>
        <el-form-item label="理论总产出OORT=day_reward*valid_days" prop="totalYieldTarget">
          <el-input v-model="form.totalYieldTarget" placeholder="请输入理论总产出OORT=day_reward*valid_days" />
        </el-form-item>
        <el-form-item label="已产出OORT(累计)" prop="yieldedAmount">
          <el-input v-model="form.yieldedAmount" placeholder="请输入已产出OORT(累计)" />
        </el-form-item>
        <el-form-item label="状态 0:产出中,1:已到期,2:暂停,3:取消" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态 0:产出中,1:已到期,2:暂停,3:取消">
            <el-option
              v-for="dict in dict.type.t_stake_order_status"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="有效期天数" prop="validDays">
          <el-input v-model="form.validDays" placeholder="请输入有效期天数" />
        </el-form-item>
        <el-form-item label="剩余有效期天数" prop="haveDays">
          <el-input v-model="form.haveDays" placeholder="请输入剩余有效期天数" />
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
import { listStakeOrder, getStakeOrder, delStakeOrder, addStakeOrder, updateStakeOrder } from "@/api/xms/stakeOrder";

export default {
  name: "StakeOrder",
  dicts: ['t_stake_order_status'],
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
      // 质押订单表格数据
      stakeOrderList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 剩余有效期天数时间范围
      daterangeCreateTime: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        orderNo: null,
        quantity: null,
        stakeOortAmount: null,
        extraValueUsdt: null,
        oortPriceUsdt: null,
        extraStakeOortAmount: null,
        dayReward: null,
        totalYieldTarget: null,
        yieldedAmount: null,
        status: null,
        validDays: null,
        haveDays: null,
        createTime: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        userId: [
          { required: true, message: "用户id不能为空", trigger: "blur" }
        ],
        orderNo: [
          { required: true, message: "质押订单号(唯一)不能为空", trigger: "blur" }
        ],
        quantity: [
          { required: true, message: "购买份数不能为空", trigger: "blur" }
        ],
        stakeOortAmount: [
          { required: true, message: "固定质押OORT数量=stake_unit_amount*quantity不能为空", trigger: "blur" }
        ],
        extraValueUsdt: [
          { required: true, message: "额外质押USDT等值=extra_stake_value_usdt*quantity不能为空", trigger: "blur" }
        ],
        oortPriceUsdt: [
          { required: true, message: "下单时OORT价格(USDT)不能为空", trigger: "blur" }
        ],
        extraStakeOortAmount: [
          { required: true, message: "额外质押折算OORT数量=extra_value_usdt/oort_price_usdt不能为空", trigger: "blur" }
        ],
        dayReward: [
          { required: true, message: "每日产出OORT=product.day_reward*quantity不能为空", trigger: "blur" }
        ],
        totalYieldTarget: [
          { required: true, message: "理论总产出OORT=day_reward*valid_days不能为空", trigger: "blur" }
        ],
        yieldedAmount: [
          { required: true, message: "已产出OORT(累计)不能为空", trigger: "blur" }
        ],
        status: [
          { required: true, message: "状态 0:产出中,1:已到期,2:暂停,3:取消不能为空", trigger: "change" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询质押订单列表 */
    getList() {
      this.loading = true;
      this.queryParams.params = {};
      if (null != this.daterangeCreateTime && '' != this.daterangeCreateTime) {
        this.queryParams.params["beginCreateTime"] = this.daterangeCreateTime[0];
        this.queryParams.params["endCreateTime"] = this.daterangeCreateTime[1];
      }
      listStakeOrder(this.queryParams).then(response => {
        this.stakeOrderList = response.rows;
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
        userId: null,
        orderNo: null,
        quantity: null,
        stakeOortAmount: null,
        extraValueUsdt: null,
        oortPriceUsdt: null,
        extraStakeOortAmount: null,
        dayReward: null,
        totalYieldTarget: null,
        yieldedAmount: null,
        status: null,
        validDays: null,
        haveDays: null,
        createTime: null,
        updateTime: null
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
      this.title = "添加质押订单";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getStakeOrder(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改质押订单";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateStakeOrder(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addStakeOrder(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除质押订单编号为"' + ids + '"的数据项？').then(function() {
        return delStakeOrder(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('xms/stakeOrder/export', {
        ...this.queryParams
      }, `stakeOrder_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
