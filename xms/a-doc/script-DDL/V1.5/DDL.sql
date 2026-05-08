-- 固定释放比例后移除质押比例配置字段

ALTER TABLE `t_stake_product`
    DROP COLUMN `immediate_ratio`,
    DROP COLUMN `linear_ratio`;

ALTER TABLE `t_stake_order`
    DROP COLUMN `immediate_ratio`,
    DROP COLUMN `linear_ratio`;
