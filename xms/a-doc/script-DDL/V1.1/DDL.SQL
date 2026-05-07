


-- 提现配置添加 6:代理分红收益,7:运营收益
INSERT INTO `t_withdrawal_config` ( `coin_type`, `coin_code`, `withdraw_open`, `min_withdraw_amount`, `fee_ratio`, `daily_free_audit_count`, `withdraw_limit`, `create_time`, `update_time`, `remark`) VALUES ( 6, '代理分红收益', 1, 0.00000000, 0.0000, 0, 0.00000000, '2026-03-16 17:42:44', NULL, NULL);
INSERT INTO `t_withdrawal_config` ( `coin_type`, `coin_code`, `withdraw_open`, `min_withdraw_amount`, `fee_ratio`, `daily_free_audit_count`, `withdraw_limit`, `create_time`, `update_time`, `remark`) VALUES ( 7, '运营收益', 1, 0.00000000, 0.0000, 0, 0.00000000, '2026-03-16 17:42:51', NULL, NULL);

-- 1:USDT,2:DFC,3:OORT,5:产出DFC,6:代理分红收益,7:运营收益

-- 资产类型添加
-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('闪兑配置', '2305', '1', 'swapConfig', 'xms/swapConfig/index', 1, 0, 'C', '0', '0', 'xms:swapConfig:list', 'log', 'admin', sysdate(), '', null, '闪兑配置菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('闪兑配置查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'xms:swapConfig:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('闪兑配置新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'xms:swapConfig:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('闪兑配置修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'xms:swapConfig:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('闪兑配置删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'xms:swapConfig:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('闪兑配置导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'xms:swapConfig:export',       '#', 'admin', sysdate(), '', null, '');


-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('闪兑记录', '2305', '1', 'swapRecord', 'xms/swapRecord/index', 1, 0, 'C', '0', '0', 'xms:swapRecord:list', 'log', 'admin', sysdate(), '', null, '闪兑记录菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('闪兑记录查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'xms:swapRecord:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('闪兑记录新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'xms:swapRecord:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('闪兑记录修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'xms:swapRecord:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('闪兑记录删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'xms:swapRecord:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('闪兑记录导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'xms:swapRecord:export',       '#', 'admin', sysdate(), '', null, '');


INSERT INTO `sys_dict_data` ( `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES ( 26, '闪兑扣除', '26', 't_user_money_log_source_type', NULL, 'primary', 'N', '0', 'admin', '2026-03-16 20:38:37', 'admin', '2026-03-16 20:39:18', NULL);
INSERT INTO `sys_dict_data` ( `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES ( 27, '闪兑增加', '27', 't_user_money_log_source_type', NULL, 'success', 'N', '0', 'admin', '2026-03-16 20:38:50', 'admin', '2026-03-16 20:39:02', NULL);



CREATE TABLE `t_swap_config` (
                                 `id` int NOT NULL AUTO_INCREMENT COMMENT '主键id',
                                 `source_coin_type` tinyint NOT NULL COMMENT '源币种类型 5:产出DFC,6:代理分红收益,7:运营收益',
                                 `source_coin_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci NOT NULL COMMENT '源币种编码',
                                 `target_coin_type` tinyint NOT NULL DEFAULT '1' COMMENT '目标币种类型 1:USDT',
                                 `target_coin_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci NOT NULL DEFAULT 'USDT' COMMENT '目标币种编码',
                                 `swap_open` tinyint NOT NULL DEFAULT '1' COMMENT '闪兑开关(1:开,2:关)',
                                 `swap_price` decimal(20,8) NOT NULL DEFAULT '0.00000000' COMMENT '闪兑价格(1个源币种可兑换多少USDT)',
                                 `fee_ratio` decimal(10,4) NOT NULL DEFAULT '0.0000' COMMENT '手续费率(例如:5表示5%)',
                                 `min_swap_amount` decimal(20,8) unsigned NOT NULL DEFAULT '0.00000000' COMMENT '最小闪兑数量',
                                 `max_swap_amount` decimal(20,8) unsigned NOT NULL DEFAULT '0.00000000' COMMENT '单笔最大闪兑数量,0表示不限制',
                                 `daily_swap_limit` decimal(20,8) unsigned NOT NULL DEFAULT '0.00000000' COMMENT '单日闪兑额度,0表示不限制',
                                 `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序值',
                                 `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_time` timestamp NULL DEFAULT NULL COMMENT '修改时间',
                                 `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`id`) USING BTREE,
                                 UNIQUE KEY `uk_source_target_coin` (`source_coin_type`,`target_coin_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=DYNAMIC COMMENT='闪兑配置表';


CREATE TABLE `t_swap_record` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键id',
                                 `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci NOT NULL COMMENT '闪兑单号',
                                 `user_id` bigint NOT NULL COMMENT '用户id',
                                 `source_coin_type` tinyint NOT NULL COMMENT '源币种类型 5:产出DFC,6:代理分红收益,7:运营收益',
                                 `target_coin_type` tinyint NOT NULL DEFAULT '1' COMMENT '目标币种类型 1:USDT',
                                 `target_coin_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci NOT NULL DEFAULT 'USDT' COMMENT '目标币种编码',
                                 `source_amount` decimal(20,8) unsigned NOT NULL DEFAULT '0.00000000' COMMENT '闪兑源数量',
                                 `swap_price` decimal(20,8) NOT NULL DEFAULT '0.00000000' COMMENT '闪兑价格快照',
                                 `fee_ratio` decimal(10,4) NOT NULL DEFAULT '0.0000' COMMENT '手续费率快照',
                                 `fee_amount` decimal(20,8) unsigned NOT NULL DEFAULT '0.00000000' COMMENT '手续费金额',
                                 `target_net_amount` decimal(20,8) unsigned NOT NULL DEFAULT '0.00000000' COMMENT '实际到账USDT',
                                 `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_time` timestamp NULL DEFAULT NULL COMMENT '修改时间',
                                 `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_520_ci DEFAULT NULL COMMENT '备注',
                                 PRIMARY KEY (`id`) USING BTREE,
                                 UNIQUE KEY `uk_order_no` (`order_no`) USING BTREE,
                                 KEY `idx_user_id_create_time` (`user_id`,`create_time`) USING BTREE,
                                 KEY `idx_status_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_520_ci ROW_FORMAT=DYNAMIC COMMENT='闪兑记录表';


-- 订单状态添加
INSERT INTO `sys_dict_data` ( `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES ( 4, '下架', '4', 't_mining_package_order_status', NULL, 'danger', 'N', '0', 'admin', '2026-03-17 14:38:42', '', NULL, NULL);


-- 闪兑配置
INSERT INTO `xmsdfc`.`t_swap_config` (`id`, `source_coin_type`, `source_coin_code`, `target_coin_type`, `target_coin_code`, `swap_open`, `swap_price`, `fee_ratio`, `min_swap_amount`, `max_swap_amount`, `daily_swap_limit`, `sort_order`, `create_time`, `update_time`, `remark`) VALUES (1, 5, '产出DFC', 1, 'USDT', 0, 5.00000000, 15.0000, 30.00000000, 0.00000000, 0.00000000, 0, '2026-03-16 18:26:36', '2026-03-18 16:00:29', NULL);
INSERT INTO `xmsdfc`.`t_swap_config` (`id`, `source_coin_type`, `source_coin_code`, `target_coin_type`, `target_coin_code`, `swap_open`, `swap_price`, `fee_ratio`, `min_swap_amount`, `max_swap_amount`, `daily_swap_limit`, `sort_order`, `create_time`, `update_time`, `remark`) VALUES (2, 6, '代理分红收益', 1, 'USDT', 0, 4.00000000, 10.0000, 20.00000000, 0.00000000, 0.00000000, 0, '2026-03-16 18:27:04', '2026-03-18 16:04:30', NULL);
INSERT INTO `xmsdfc`.`t_swap_config` (`id`, `source_coin_type`, `source_coin_code`, `target_coin_type`, `target_coin_code`, `swap_open`, `swap_price`, `fee_ratio`, `min_swap_amount`, `max_swap_amount`, `daily_swap_limit`, `sort_order`, `create_time`, `update_time`, `remark`) VALUES (3, 7, '运营收益', 1, 'USDT', 0, 2.00000000, 10.0000, 10.00000000, 0.00000000, 0.00000000, 0, '2026-03-16 18:27:16', '2026-03-18 16:04:13', NULL);
