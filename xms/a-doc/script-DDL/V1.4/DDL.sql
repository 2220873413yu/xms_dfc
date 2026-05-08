-- 质押订单增加 DFC 下单价格快照

ALTER TABLE `t_stake_order`
    ADD COLUMN `dfc_price_usdt` DECIMAL(30,8) NOT NULL DEFAULT 0.00000000 COMMENT '下单时DFC价格(USDT)' AFTER `oort_price_usdt`;
