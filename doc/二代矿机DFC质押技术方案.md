# 二代矿机 DFC 质押技术方案

## 1. 总体方案

在现有 `t_stake_product`、`t_stake_order`、`t_stake_release_bucket` 上扩展币种、库存、释放比例、释放天数和退本字段，不新建 DFC 专用业务表。DFC 产品支持后台页面配置立即释放比例、线性释放比例、线性释放天数；OORT 后台页面不开放这些配置，但允许通过数据库维护，订单按产品配置快照执行。

资金资产对应关系：

| 场景 | 资产字段 |
| --- | --- |
| DFC 本金扣款 | `t_user_money.valid_num2` |
| DFC 本金退还 | `t_user_money.valid_num2` |
| DFC 产出释放 | `t_user_money.valid_num5` |
| OORT 本金扣款和产出 | 沿用 `valid_num3` |

## 2. 数据库变更

DDL 已写入：

```text
xms/a-doc/script-DDL/V1.3/DDL.sql
xms/a-doc/script-DDL/V1.7/DDL.sql
```

核心新增字段：

- `t_stake_product.coin_type`
- `t_stake_product.reward_coin_type`
- `t_stake_product.available_stock`
- `t_stake_product.immediate_ratio`
- `t_stake_product.linear_ratio`
- `t_stake_product.linear_days`
- `t_stake_order.product_id`
- `t_stake_order.coin_type`
- `t_stake_order.reward_coin_type`
- `t_stake_order.stake_amount`
- `t_stake_order.immediate_ratio`
- `t_stake_order.linear_ratio`
- `t_stake_order.linear_days`
- `t_stake_order.principal_refund_status`
- `t_stake_order.principal_refund_time`
- `t_stake_release_bucket.coin_type`

DFC 产品由增量 SQL 初始化/修正为 `coin_type=2,reward_coin_type=5`，默认 `immediate_ratio=25,linear_ratio=75,linear_days=365`；历史 OORT 数据依赖新增字段默认值兼容。

## 3. 常量与字典

新增奖励记录来源：

| 值 | 含义 |
| --- | --- |
| 27 | DFC 质押立即释放 |
| 28 | DFC 质押线性释放 |
| 29 | DFC 质押本金退还 |

新增钱包流水来源：

| 值 | 含义 |
| --- | --- |
| 33 | DFC 质押扣款 |
| 34 | DFC 质押立即释放 |
| 35 | DFC 质押线性释放 |
| 36 | DFC 质押本金退还 |

新增后台字典：

- `biz_stake_coin_type`：`2=DFC`，`3=OORT`
- `biz_stake_reward_coin_type`：`3=OORT`，`5=产出DFC`
- `biz_principal_refund_status`：`0=未退还`，`1=已退还`

## 4. App 接口

`GET /api/stake/stakeInfo` 新增可选参数 `coinType`：

- 不传：默认 OORT
- `2`：查询 DFC 上架产品
- `3`：查询 OORT 上架产品

`POST /api/stake/stakeOrder` 继续复用原请求体，`id` 作为产品 ID 查询 `t_stake_product`。后端按产品币种分支：

- DFC：校验库存，扣 `valid_num2`，订单记录 `rewardCoinType=5`，并快照产品的立即释放比例、线性释放比例、线性释放天数，不处理额外 OORT 授权。
- OORT：保持现有扣 `valid_num3` 和额外 USDT 等值 OORT 逻辑，同时快照数据库产品上的释放比例和线性释放天数。

我的质押相关接口新增可选 `coinType` 参数，不传默认 OORT。

## 5. 定时任务

任务入口仍为：

```java
AsyncTaskServiceImpl.distributePtbInterest101()
```

处理顺序：

1. 先处理历史线性释放桶。
2. 扣减产出中质押订单的 `have_days`。
3. 对订单按快照计算当日产出。
4. 按订单快照比例立即释放，按 `reward_coin_type` 入账。
5. 按订单快照比例生成线性释放桶，桶记录 `coin_type`。
6. 若订单到期，先完成最后一天产出，再关闭订单。
7. DFC 到期订单退还本金到 `valid_num2`，并写钱包流水与奖励记录。

退本使用 `principal_refund_status=0` 作为幂等条件，避免重复退还。

## 6. 后台

产品管理页扩展币种、库存和释放天数字段。后端保存/修改时校验：

- 每份质押数量大于 0
- 每日产出大于 0
- 有效期大于 0
- DFC 产品库存不能为空且不能小于 0
- DFC 产品立即释放比例、线性释放比例不能为空且必须大于 0，二者之和不能超过 100
- DFC 产品线性释放天数必须大于 0
- OORT 产品继续要求额外质押 USDT 等值金额大于 0
- OORT 产品不开放页面释放比例配置，后台编辑时保留数据库原值或默认 25/75

订单管理页扩展展示产品 ID、币种、质押本金、释放天数和退本状态。

## 7. 验收点

- DFC 下单成功扣可用 DFC，扣库存，生成 DFC 质押订单。
- DFC 库存不足时下单失败，钱包和库存不变。
- DFC 可用余额不足时下单失败。
- DFC 每日产出按订单快照立即释放比例进入产出 DFC，按订单快照线性释放比例进入线性释放桶。
- DFC 释放桶释放时进入产出 DFC。
- DFC 到期当天先产出最后一天收益，再退本金到可用 DFC。
- OORT 下单、产出、线性释放保持兼容。
