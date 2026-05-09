-- 质押释放比例增量脚本
-- 适配当前线上表结构：t_stake_product/t_stake_order 已有 linear_days，但没有 immediate_ratio、linear_ratio。
-- 规则：DFC产品允许后台页面配置立即释放比例、线性释放比例、线性释放天数；OORT可通过数据库维护配置，但页面不开放。

ALTER TABLE `t_stake_product`
    ADD COLUMN `immediate_ratio` DECIMAL(10,4) NOT NULL DEFAULT 25.0000 COMMENT '立即释放比例，单位%' AFTER `day_reward`,
    ADD COLUMN `linear_ratio` DECIMAL(10,4) NOT NULL DEFAULT 75.0000 COMMENT '线性释放比例，单位%' AFTER `immediate_ratio`;

ALTER TABLE `t_stake_order`
    ADD COLUMN `immediate_ratio` DECIMAL(10,4) NOT NULL DEFAULT 25.0000 COMMENT '立即释放比例快照，单位%' AFTER `day_reward`,
    ADD COLUMN `linear_ratio` DECIMAL(10,4) NOT NULL DEFAULT 75.0000 COMMENT '线性释放比例快照，单位%' AFTER `immediate_ratio`;

UPDATE `t_stake_product`
SET `immediate_ratio` = 25.0000,
    `linear_ratio` = 75.0000,
    `linear_days` = 365
WHERE `coin_type` = 2;

UPDATE `t_stake_product`
SET `immediate_ratio` = 25.0000,
    `linear_ratio` = 75.0000
WHERE `coin_type` = 3;

UPDATE `t_stake_order`
SET `immediate_ratio` = 25.0000,
    `linear_ratio` = 75.0000;
