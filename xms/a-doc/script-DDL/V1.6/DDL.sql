-- DFC质押产品初始化与币种修正
-- 规则：DFC质押本金扣/退 valid_num2，产出进入 valid_num5，因此DFC产品必须为 coin_type=2、reward_coin_type=5。

INSERT INTO `t_stake_product`
    (`name`, `coin_type`, `reward_coin_type`, `sales`, `available_stock`, `stake_unit_amount`,
     `extra_stake_value_usdt`, `day_reward`, `linear_days`, `valid_days`, `status`, `create_time`, `remark`)
SELECT 'DFC质押', 2, 5, 0, 0, 10000.000000, 0.000000, 27.390000, 365, 365, 0, NOW(), 'DFC质押初始化，默认不上架'
WHERE NOT EXISTS (
    SELECT 1 FROM `t_stake_product` WHERE `coin_type` = 2
);

UPDATE `t_stake_product`
SET `reward_coin_type` = 5,
    `extra_stake_value_usdt` = 0.000000,
    `linear_days` = 365,
    `valid_days` = 365
WHERE `coin_type` = 2;

UPDATE `t_stake_product`
SET `reward_coin_type` = 3
WHERE `coin_type` = 3;
