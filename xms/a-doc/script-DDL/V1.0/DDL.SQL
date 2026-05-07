
-- t_swap_order
ALTER TABLE `t_swap_order`
    ADD COLUMN `available_amount` decimal(20, 6) NULL DEFAULT 0 COMMENT '可用的额度' AFTER `swap_amount`;


-- 系统参数
INSERT INTO `t_sys_para` (`sys_para_id`, `para_code`, `para_value`, `para_desc`, `visible`, `create_time`, `update_time`, `active_flag`, `remark`) VALUES (147, 'biz_swap_order_effective_ratio', '100', 'swap订单生效额度比例 例如：100 填写100 表示swap订单生效额度为swap订单额度的100%', '0', '2026-01-15 16:05:15', NULL, 1, NULL);

-- 提现配置表
CREATE TABLE `t_withdrawal_config` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `coin_type` int NOT NULL COMMENT '币种 1:USDT,2:DFC,3:OORT',
    `coin_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci NOT NULL COMMENT '币种编码',
    `withdraw_open` tinyint NOT NULL DEFAULT 1 COMMENT '提现开关(1:开,2:关)',
    `min_withdraw_amount` decimal(20,8) unsigned NOT NULL DEFAULT 0.00000000 COMMENT '最小提现金额',
    `fee_ratio` decimal(10,4) NOT NULL DEFAULT 0.0000 COMMENT '手续费率(例如:5表示5%)',
    `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` timestamp NULL DEFAULT NULL COMMENT '修改时间',
    `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_coin_type` (`coin_type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=DYNAMIC COMMENT='提现配置表';

-- 提现配置初始化(3条)
INSERT INTO `t_withdrawal_config` (`coin_type`, `coin_code`, `withdraw_open`, `min_withdraw_amount`, `fee_ratio`, `create_time`, `remark`) VALUES
  (1, 'USDT', 1, 15.00000000, 5.0000, NOW(), 'USDT提现配置'),
  (2, 'DFC', 1, 15.00000000, 5.0000, NOW(), 'DFC提现配置'),
  (3, 'OORT', 1, 15.00000000, 5.0000, NOW(), 'OORT提现配置');

-- 提现配置表添加字段
ALTER TABLE `t_withdrawal_config`
    ADD COLUMN `daily_free_audit_count` int NOT NULL DEFAULT 0 COMMENT '单日免审核次数' AFTER `fee_ratio`,
    ADD COLUMN `withdraw_limit` decimal(20,8) unsigned NOT NULL DEFAULT 0.00000000 COMMENT '提现额度' AFTER `daily_free_audit_count`;