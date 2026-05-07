<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryForm" :inline="true" :model="queryParams" label-width="68px" size="small">
      <el-form-item label="源币种类型" prop="sourceCoinType">
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
      <el-form-item label="闪兑是否打开 0:否,1:是" prop="swapOpen">
        <el-input
          v-model="queryParams.swapOpen"
          placeholder="请输入闪兑是否打开 0:否,1:是"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="闪兑价格(1个源币种可兑换多少USDT)" prop="swapPrice">
        <el-input
          v-model="queryParams.swapPrice"
          placeholder="请输入闪兑价格(1个源币种可兑换多少USDT)"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手续费率(例如:5表示5%)" prop="feeRatio">
        <el-input
          v-model="queryParams.feeRatio"
          placeholder="请输入手续费率(例如:5表示5%)"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="最小闪兑数量" prop="minSwapAmount">
        <el-input
          v-model="queryParams.minSwapAmount"
          placeholder="请输入最小闪兑数量"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="单笔最大闪兑数量,0表示不限制" prop="maxSwapAmount">
        <el-input
          v-model="queryParams.maxSwapAmount"
          placeholder="请输入单笔最大闪兑数量,0表示不限制"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="单日闪兑额度,0表示不限制" prop="dailySwapLimit">
        <el-input
          v-model="queryParams.dailySwapLimit"
          placeholder="请输入单日闪兑额度,0表示不限制"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="排序值" prop="sortOrder">
        <el-input
          v-model="queryParams.sortOrder"
          placeholder="请输入排序值"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>-->
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
          v-hasPermi="['xms:swapConfig:add']"
        >新增</el-button>
      </el-col>-->
      <el-col :span="1.5">
        <el-button
          v-hasPermi="['xms:swapConfig:edit']"
          :disabled="single"
          icon="el-icon-edit"
          plain
          size="mini"
          type="success"
          @click="handleUpdate"
        >修改</el-button>
      </el-col>
<!--      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['xms:swapConfig:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['xms:swapConfig:export']"
        >导出</el-button>
      </el-col>-->
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="swapConfigList" @selection-change="handleSelectionChange">
      <el-table-column align="center" type="selection" width="55" />
      <el-table-column v-if="false" align="center" label="主键id" prop="id"/>
      <el-table-column align="center" label="源币种类型" prop="sourceCoinType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.t_user_money_log_coin_type" :value="scope.row.sourceCoinType"/>
        </template>
      </el-table-column>
<!--      <el-table-column label="源币种编码" align="center" prop="sourceCoinCode" />-->
      <el-table-column align="center" label="目标币种类型" prop="targetCoinType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.t_user_money_log_coin_type" :value="scope.row.targetCoinType"/>
        </template>
      </el-table-column>
      <el-table-column align="center" label="闪兑是否打开" prop="swapOpen">
      <template slot-scope="scope">
        <dict-tag :options="dict.type.t_user_info_is_valid" :value="scope.row.swapOpen"/>
      </template>
      </el-table-column>
      <el-table-column align="center" label="闪兑价格" prop="swapPrice" />
      <el-table-column align="center" label="手续费率" prop="feeRatio" >
        <template slot-scope="scope">
          {{scope.row.feeRatio}}%
        </template>
      </el-table-column>
      <el-table-column align="center" label="最小闪兑数量" prop="minSwapAmount" />
<!--      <el-table-column label="单笔最大闪兑数量,0表示不限制" align="center" prop="maxSwapAmount" />
      <el-table-column label="单日闪兑额度,0表示不限制" align="center" prop="dailySwapLimit" />-->
<!--      <el-table-column label="排序值" align="center" prop="sortOrder" />
      <el-table-column label="备注" align="center" prop="remark" />-->
      <el-table-column align="center" class-name="small-padding fixed-width" label="操作">
        <template slot-scope="scope">
          <el-button
            v-hasPermi="['xms:swapConfig:edit']"
            icon="el-icon-edit"
            size="mini"
            type="text"
            @click="handleUpdate(scope.row)"
          >修改</el-button>
<!--          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['xms:swapConfig:remove']"
          >删除</el-button>-->
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :limit.sync="queryParams.pageSize"
      :page.sync="queryParams.pageNum"
      :total="total"
      @pagination="getList"
    />

    <!-- 添加或修改闪兑配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" append-to-body width="500px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="源币种类型" prop="sourceCoinType">
          <el-select v-model="form.sourceCoinType" placeholder="请选择源币种类型">
            <el-option
              v-for="dict in dict.type.t_user_money_log_coin_type"
              :key="dict.value"
              :disabled="true"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="目标币种类型" prop="targetCoinType">
          <el-select v-model="form.targetCoinType" placeholder="请选择目标币种类型">
            <el-option
              v-for="dict in dict.type.t_user_money_log_coin_type"
              :key="dict.value"
              :disabled="true"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="闪兑是否打开" prop="swapOpen">
          <el-select v-model="form.swapOpen" placeholder="请选择是否启用">
            <el-option
              v-for="dict in dict.type.t_user_info_is_valid"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="闪兑价格" prop="swapPrice">
          <el-input
            v-model="form.swapPrice"
            placeholder="请输入闪兑价格"
            @input="form.swapPrice = limitToTwoDecimals(form.swapPrice)"
          />
        </el-form-item>

        <el-form-item label="手续费率" prop="feeRatio">
          <el-input
            v-model="form.feeRatio"
            placeholder="请输入手续费率(例如:5表示5%)"
            @input="form.feeRatio = limitToTwoDecimals(form.feeRatio)"
          />
          <div class="form-tip">以百分比为单位，例如：1 表示 1%</div>
        </el-form-item>

        <el-form-item label="最小闪兑数量" prop="minSwapAmount">
          <el-input v-model="form.minSwapAmount"
                    oninput="if(isNaN(value)) { value = null } else { value = value.replace('.', '') }"
                    placeholder="请输入最小闪兑数量" />
        </el-form-item>
<!--        <el-form-item label="单笔最大闪兑数量,0表示不限制" prop="maxSwapAmount">
          <el-input v-model="form.maxSwapAmount" placeholder="请输入单笔最大闪兑数量,0表示不限制" />
        </el-form-item>
        <el-form-item label="单日闪兑额度,0表示不限制" prop="dailySwapLimit">
          <el-input v-model="form.dailySwapLimit" placeholder="请输入单日闪兑额度,0表示不限制" />
        </el-form-item>
        <el-form-item label="排序值" prop="sortOrder">
          <el-input v-model="form.sortOrder" placeholder="请输入排序值" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>-->
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listSwapConfig, getSwapConfig, delSwapConfig, addSwapConfig, updateSwapConfig } from "@/api/xms/swapConfig";

export default {
  name: "SwapConfig",
  dicts: ['t_user_money_log_coin_type','t_user_info_is_valid'],
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
      // 闪兑配置表格数据
      swapConfigList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        sourceCoinType: null,
        sourceCoinCode: null,
        targetCoinType: null,
        targetCoinCode: null,
        swapOpen: null,
        swapPrice: null,
        feeRatio: null,
        minSwapAmount: null,
        maxSwapAmount: null,
        dailySwapLimit: null,
        sortOrder: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
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
        swapOpen: [
          { required: true, message: "闪兑是否打开 0:否,1:是不能为空", trigger: "blur" }
        ],
        swapPrice: [
          { required: true, message: "闪兑价格(1个源币种可兑换多少USDT)不能为空", trigger: "blur" }
        ],
        feeRatio: [
          { required: true, message: "手续费率(例如:5表示5%)不能为空", trigger: "blur" }
        ],
        minSwapAmount: [
          { required: true, message: "最小闪兑数量不能为空", trigger: "blur" }
        ],
        maxSwapAmount: [
          { required: true, message: "单笔最大闪兑数量,0表示不限制不能为空", trigger: "blur" }
        ],
        dailySwapLimit: [
          { required: true, message: "单日闪兑额度,0表示不限制不能为空", trigger: "blur" }
        ],
        sortOrder: [
          { required: true, message: "排序值不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询闪兑配置列表 */
    getList() {
      this.loading = true;
      listSwapConfig(this.queryParams).then(response => {
        this.swapConfigList = response.rows;
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
        sourceCoinType: null,
        sourceCoinCode: null,
        targetCoinType: null,
        targetCoinCode: null,
        swapOpen: null,
        swapPrice: null,
        feeRatio: null,
        minSwapAmount: null,
        maxSwapAmount: null,
        dailySwapLimit: null,
        sortOrder: null,
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
      this.title = "添加闪兑配置";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getSwapConfig(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改闪兑配置";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateSwapConfig(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addSwapConfig(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除闪兑配置编号为"' + ids + '"的数据项？').then(function() {
        return delSwapConfig(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('xms/swapConfig/export', {
        ...this.queryParams
      }, `swapConfig_${new Date().getTime()}.xlsx`)
    },
    limitToTwoDecimals(value) {
      if (value === null || value === undefined) {
        return "";
      }
      let sanitized = String(value).replace(/[^\d.]/g, "");
      sanitized = sanitized.replace(/^\./g, "");
      sanitized = sanitized.replace(/\.{2,}/g, ".");
      sanitized = sanitized.replace(".", "$#$").replace(/\./g, "").replace("$#$", ".");
      sanitized = sanitized.replace(/^(\d+)\.(\d{0,2}).*$/, "$1.$2");
      return sanitized;
    }
  }
};
</script>
