-- 二代矿机 DFC 质押
-- 说明：DFC 产品数据由后台手工配置，本脚本只补充表结构和字典。

ALTER TABLE `t_stake_product`
    ADD COLUMN `coin_type` INT NOT NULL DEFAULT 3 COMMENT '质押币种 2:DFC 3:OORT' AFTER `name`,
    ADD COLUMN `reward_coin_type` INT NOT NULL DEFAULT 3 COMMENT '产出币种 3:OORT 5:产出DFC' AFTER `coin_type`,
    ADD COLUMN `available_stock` INT DEFAULT NULL COMMENT '可用库存份数，NULL表示不限制库存' AFTER `sales`,
    ADD COLUMN `immediate_ratio` DECIMAL(10,4) NOT NULL DEFAULT 25.0000 COMMENT '立即释放比例，单位%' AFTER `day_reward`,
    ADD COLUMN `linear_ratio` DECIMAL(10,4) NOT NULL DEFAULT 75.0000 COMMENT '线性释放比例，单位%' AFTER `immediate_ratio`,
    ADD COLUMN `linear_days` INT NOT NULL DEFAULT 270 COMMENT '线性释放天数' AFTER `linear_ratio`;

ALTER TABLE `t_stake_order`
    ADD COLUMN `product_id` BIGINT DEFAULT NULL COMMENT '质押产品ID' AFTER `user_id`,
    ADD COLUMN `coin_type` INT NOT NULL DEFAULT 3 COMMENT '质押币种 2:DFC 3:OORT' AFTER `product_id`,
    ADD COLUMN `reward_coin_type` INT NOT NULL DEFAULT 3 COMMENT '产出币种 3:OORT 5:产出DFC' AFTER `coin_type`,
    ADD COLUMN `stake_amount` DECIMAL(30,8) NOT NULL DEFAULT 0.00000000 COMMENT '实际质押本金数量' AFTER `quantity`,
    ADD COLUMN `immediate_ratio` DECIMAL(10,4) NOT NULL DEFAULT 25.0000 COMMENT '立即释放比例，单位%' AFTER `day_reward`,
    ADD COLUMN `linear_ratio` DECIMAL(10,4) NOT NULL DEFAULT 75.0000 COMMENT '线性释放比例，单位%' AFTER `immediate_ratio`,
    ADD COLUMN `linear_days` INT NOT NULL DEFAULT 270 COMMENT '线性释放天数' AFTER `linear_ratio`,
    ADD COLUMN `principal_refund_status` TINYINT NOT NULL DEFAULT 0 COMMENT '本金退还状态 0:未退还 1:已退还' AFTER `status`,
    ADD COLUMN `principal_refund_time` DATETIME DEFAULT NULL COMMENT '本金退还时间' AFTER `principal_refund_status`;

ALTER TABLE `t_stake_release_bucket`
    ADD COLUMN `coin_type` INT NOT NULL DEFAULT 3 COMMENT '释放币种 3:OORT 5:产出DFC' AFTER `user_id`;

INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
SELECT '质押币种', 'biz_stake_coin_type', '0', 'admin', NOW(), '二代矿机质押币种'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'biz_stake_coin_type');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 1, 'DFC', '2', 'biz_stake_coin_type', 'primary', 'N', '0', 'admin', NOW(), 'DFC'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'biz_stake_coin_type' AND `dict_value` = '2');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 2, 'OORT', '3', 'biz_stake_coin_type', 'success', 'N', '0', 'admin', NOW(), 'OORT'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'biz_stake_coin_type' AND `dict_value` = '3');

INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
SELECT '质押产出币种', 'biz_stake_reward_coin_type', '0', 'admin', NOW(), '二代矿机产出币种'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'biz_stake_reward_coin_type');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 1, 'OORT', '3', 'biz_stake_reward_coin_type', 'success', 'N', '0', 'admin', NOW(), 'OORT'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'biz_stake_reward_coin_type' AND `dict_value` = '3');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 2, '产出DFC', '5', 'biz_stake_reward_coin_type', 'primary', 'N', '0', 'admin', NOW(), '产出DFC'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'biz_stake_reward_coin_type' AND `dict_value` = '5');

INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `remark`)
SELECT '本金退还状态', 'biz_principal_refund_status', '0', 'admin', NOW(), '质押订单本金退还状态'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_type` WHERE `dict_type` = 'biz_principal_refund_status');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 1, '未退还', '0', 'biz_principal_refund_status', 'warning', 'N', '0', 'admin', NOW(), '未退还'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'biz_principal_refund_status' AND `dict_value` = '0');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 2, '已退还', '1', 'biz_principal_refund_status', 'success', 'N', '0', 'admin', NOW(), '已退还'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'biz_principal_refund_status' AND `dict_value` = '1');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 33, 'DFC质押扣款', '33', 't_user_money_log_source_type', 'warning', 'N', '0', 'admin', NOW(), 'DFC质押扣款'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 't_user_money_log_source_type' AND `dict_value` = '33');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 34, 'DFC质押立即释放', '34', 't_user_money_log_source_type', 'success', 'N', '0', 'admin', NOW(), 'DFC质押立即释放'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 't_user_money_log_source_type' AND `dict_value` = '34');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 35, 'DFC质押线性释放', '35', 't_user_money_log_source_type', 'success', 'N', '0', 'admin', NOW(), 'DFC质押线性释放'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 't_user_money_log_source_type' AND `dict_value` = '35');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 36, 'DFC质押本金退还', '36', 't_user_money_log_source_type', 'info', 'N', '0', 'admin', NOW(), 'DFC质押本金退还'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 't_user_money_log_source_type' AND `dict_value` = '36');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 27, 'DFC质押立即释放', '27', 'reward_record_source_type', 'success', 'N', '0', 'admin', NOW(), 'DFC质押立即释放'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'reward_record_source_type' AND `dict_value` = '27');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 28, 'DFC质押线性释放', '28', 'reward_record_source_type', 'success', 'N', '0', 'admin', NOW(), 'DFC质押线性释放'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'reward_record_source_type' AND `dict_value` = '28');

INSERT INTO `sys_dict_data` (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
SELECT 29, 'DFC质押本金退还', '29', 'reward_record_source_type', 'info', 'N', '0', 'admin', NOW(), 'DFC质押本金退还'
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_data` WHERE `dict_type` = 'reward_record_source_type' AND `dict_value` = '29');
