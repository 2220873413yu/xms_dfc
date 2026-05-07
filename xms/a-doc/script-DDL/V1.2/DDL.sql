-- 放开 公告菜单
update sys_menu
set status = 0,
    visible = 0
where menu_id in (107);


-- 字典 语言字典放开
update sys_dict_data
set status = 1
where dict_code in (679,680,681);

-- 字典 修改为简体中文
UPDATE `sys_dict_data` SET `dict_sort` = 3, `dict_label` = '简体中文', `dict_value` = '3', `dict_type` = 't_biz_locale', `css_class` = NULL, `list_class` = 'warning', `is_default` = 'N', `status` = '0', `create_by` = 'admin', `create_time` = '2026-01-16 21:45:58', `update_by` = 'admin', `update_time` = '2026-01-16 21:46:34', `remark` = NULL WHERE `dict_code` = 678;
