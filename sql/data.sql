-- =============================================
-- 初始数据
-- =============================================
USE `admin_db`;
SET NAMES utf8mb4;

-- 默认部门
INSERT INTO `sys_department` (`id`, `parent_id`, `dept_name`, `sort_order`, `leader`, `phone`, `email`, `status`, `created_by`) VALUES
(1, 0, '科大讯飞', 1, '管理员', '13800000001', 'admin@iflytek.com', 1, 'system'),
(2, 1, '研发部', 1, NULL, NULL, NULL, 1, 'system'),
(3, 1, '产品部', 2, NULL, NULL, NULL, 1, 'system'),
(4, 1, '运营部', 3, NULL, NULL, NULL, 1, 'system'),
(5, 2, '前端组', 1, NULL, NULL, NULL, 1, 'system'),
(6, 2, '后端组', 2, NULL, NULL, NULL, 1, 'system');

-- 默认用户 (admin/admin123, user/user123)
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `gender`, `status`, `created_by`) VALUES
(1, 'admin', '$2a$10$IiyKTE4vaC4V3kz.aTb.nuuWIuYGU8o1jpa/6HAXfszPoLsIoRl2u', '超级管理员', 'admin@iflytek.com', '13800000001', 1, 1, 'system'),
(2, 'user', '$2a$10$gtMpEYBgLedX.xq6wp8H/es/rSZph9zfF2Uz3k.P9jQDl7PpOhXrq', '普通用户', 'user@iflytek.com', '13800000002', 1, 1, 'system');

-- 默认角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `sort_order`, `status`, `description`, `created_by`) VALUES
(1, '超级管理员', 'admin', 1, 1, '拥有系统所有权限', 'system'),
(2, '普通用户', 'user', 2, 1, '普通用户权限', 'system');

-- 用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2);

-- 菜单数据
-- 一级: 仪表盘
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `icon`, `sort_order`, `permission`, `visible`, `status`) VALUES
(1, 0, '仪表盘', 2, '/dashboard', 'dashboard/index', 'Monitor', 1, NULL, 1, 1);

-- 一级: 系统管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `icon`, `sort_order`, `permission`, `visible`, `status`) VALUES
(2, 0, '系统管理', 1, '/system', NULL, 'Setting', 2, NULL, 1, 1);

-- 二级: 用户管理
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `icon`, `sort_order`, `permission`, `visible`, `status`) VALUES
(201, 2, '用户管理', 2, '/system/user', 'system/user/index', 'User', 1, 'sys:user:list', 1, 1),
(202, 2, '角色管理', 2, '/system/role', 'system/role/index', 'UserFilled', 2, 'sys:role:list', 1, 1),
(203, 2, '菜单管理', 2, '/system/menu', 'system/menu/index', 'Menu', 3, 'sys:menu:list', 1, 1),
(204, 2, '系统配置', 2, '/system/config', 'system/config/index', 'Tools', 4, 'sys:config:list', 1, 1),
(205, 2, '日志管理', 1, '/system/log', NULL, 'Document', 5, NULL, 1, 1),
(206, 2, '部门管理', 2, '/system/dept', 'system/dept/index', 'OfficeBuilding', 6, 'sys:dept:list', 1, 1),
(207, 2, '字典管理', 2, '/system/dict', 'system/dict/index', 'Collection', 7, 'sys:dict:list', 1, 1),
(208, 2, '在线用户', 2, '/system/online', 'system/online/index', 'Connection', 8, 'sys:online:list', 1, 1),
(209, 2, '文件管理', 2, '/system/file', 'system/file/index', 'FolderOpened', 9, 'sys:file:list', 1, 1),
(210, 2, '通知公告', 2, '/system/notice', 'system/notice/index', 'Bell', 10, 'sys:notice:list', 1, 1),
(211, 2, '定时任务', 2, '/system/job', 'system/job/index', 'Timer', 11, 'sys:job:list', 1, 1),
(212, 2, '系统监控', 2, '/system/monitor', 'system/monitor/index', 'DataLine', 12, 'sys:monitor:list', 1, 1);

-- 三级: 日志子菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `icon`, `sort_order`, `permission`, `visible`, `status`) VALUES
(2051, 205, '操作日志', 2, '/system/log/operation', 'system/log/operation', 'EditPen', 1, 'sys:log:list', 1, 1),
(2052, 205, '登录日志', 2, '/system/log/login', 'system/log/login', 'Key', 2, 'sys:log:list', 1, 1);

-- 按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `icon`, `sort_order`, `permission`, `visible`, `status`) VALUES
-- 用户管理按钮
(2011, 201, '新增用户', 3, NULL, NULL, NULL, 1, 'sys:user:add', 1, 1),
(2012, 201, '编辑用户', 3, NULL, NULL, NULL, 2, 'sys:user:edit', 1, 1),
(2013, 201, '删除用户', 3, NULL, NULL, NULL, 3, 'sys:user:delete', 1, 1),
(2014, 201, '导出用户', 3, NULL, NULL, NULL, 4, 'sys:user:export', 1, 1),
-- 角色管理按钮
(2021, 202, '新增角色', 3, NULL, NULL, NULL, 1, 'sys:role:add', 1, 1),
(2022, 202, '编辑角色', 3, NULL, NULL, NULL, 2, 'sys:role:edit', 1, 1),
(2023, 202, '删除角色', 3, NULL, NULL, NULL, 3, 'sys:role:delete', 1, 1),
-- 菜单管理按钮
(2031, 203, '新增菜单', 3, NULL, NULL, NULL, 1, 'sys:menu:add', 1, 1),
(2032, 203, '编辑菜单', 3, NULL, NULL, NULL, 2, 'sys:menu:edit', 1, 1),
(2033, 203, '删除菜单', 3, NULL, NULL, NULL, 3, 'sys:menu:delete', 1, 1),
-- 系统配置按钮
(2041, 204, '编辑配置', 3, NULL, NULL, NULL, 1, 'sys:config:edit', 1, 1),
-- 部门管理按钮
(2061, 206, '新增部门', 3, NULL, NULL, NULL, 1, 'sys:dept:add', 1, 1),
(2062, 206, '编辑部门', 3, NULL, NULL, NULL, 2, 'sys:dept:edit', 1, 1),
(2063, 206, '删除部门', 3, NULL, NULL, NULL, 3, 'sys:dept:delete', 1, 1),
-- 字典管理按钮
(2071, 207, '新增字典', 3, NULL, NULL, NULL, 1, 'sys:dict:add', 1, 1),
(2072, 207, '编辑字典', 3, NULL, NULL, NULL, 2, 'sys:dict:edit', 1, 1),
(2073, 207, '删除字典', 3, NULL, NULL, NULL, 3, 'sys:dict:delete', 1, 1),
-- 在线用户按钮
(2081, 208, '强制踢出', 3, NULL, NULL, NULL, 1, 'sys:online:kick', 1, 1),
-- 文件管理按钮
(2091, 209, '上传文件', 3, NULL, NULL, NULL, 1, 'sys:file:upload', 1, 1),
(2092, 209, '删除文件', 3, NULL, NULL, NULL, 2, 'sys:file:delete', 1, 1),
-- 通知公告按钮
(2101, 210, '新增通知', 3, NULL, NULL, NULL, 1, 'sys:notice:add', 1, 1),
(2102, 210, '编辑通知', 3, NULL, NULL, NULL, 2, 'sys:notice:edit', 1, 1),
(2103, 210, '删除通知', 3, NULL, NULL, NULL, 3, 'sys:notice:delete', 1, 1),
-- 定时任务按钮
(2111, 211, '新增任务', 3, NULL, NULL, NULL, 1, 'sys:job:add', 1, 1),
(2112, 211, '编辑任务', 3, NULL, NULL, NULL, 2, 'sys:job:edit', 1, 1),
(2113, 211, '删除任务', 3, NULL, NULL, NULL, 3, 'sys:job:delete', 1, 1),
(2114, 211, '执行任务', 3, NULL, NULL, NULL, 4, 'sys:job:run', 1, 1);

-- 角色菜单关联 - 超级管理员拥有所有菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `sys_menu`;

-- 角色菜单关联 - 普通用户拥有部分菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 1),     -- 仪表盘
(2, 2),     -- 系统管理目录
(2, 201),   -- 用户管理(查看)
(2, 204);   -- 系统配置(查看)

-- 字典类型
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `description`, `created_by`) VALUES
(1, '性别', 'sys_gender', 1, '用户性别', 'system'),
(2, '状态', 'sys_status', 1, '通用状态', 'system'),
(3, '菜单类型', 'sys_menu_type', 1, '菜单类型', 'system');

-- 字典数据
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `sort_order`, `status`, `created_by`) VALUES
('sys_gender', '未知', '0', 1, 1, 'system'),
('sys_gender', '男', '1', 2, 1, 'system'),
('sys_gender', '女', '2', 3, 1, 'system'),
('sys_status', '禁用', '0', 1, 1, 'system'),
('sys_status', '启用', '1', 2, 1, 'system'),
('sys_menu_type', '目录', '1', 1, 1, 'system'),
('sys_menu_type', '菜单', '2', 2, 1, 'system'),
('sys_menu_type', '按钮', '3', 3, 1, 'system');

-- 系统配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `config_name`, `config_type`, `description`) VALUES
('sys.name', '后台管理系统', '系统名称', 1, '系统显示名称'),
('sys.version', '1.0.0', '系统版本', 1, '当前系统版本号'),
('sys.domain', 'https://admin.example.com', '系统域名', 1, '系统访问域名'),
('sys.email', 'admin@example.com', '联系邮箱', 1, '系统联系邮箱'),
('sys.phone', '13800138000', '联系电话', 1, '系统联系电话'),
('sys.password.strength', '2', '密码强度', 1, '密码强度要求(1低/2中/3高)'),
('sys.login.limit', 'true', '登录限制', 1, '是否启用登录失败限制'),
('sys.login.timeout', '30', '登录超时', 1, '登录超时时间(分钟)'),
('sys.login.maxRetry', '5', '登录最大重试次数', 1, '登录失败最大重试次数'),
('sys.login.lockTime', '10', '登录锁定时间(分钟)', 1, '登录失败后锁定时间'),
('sys.notify.email', 'true', '邮件通知', 2, '是否启用邮件通知'),
('sys.notify.sms', 'false', '短信通知', 2, '是否启用短信通知'),
('sys.notify.system', 'true', '系统通知', 2, '是否启用系统内通知'),
('sys.log.level', 'info', '日志级别', 1, '系统日志级别'),
('sys.backup', 'true', '数据备份', 1, '是否启用自动数据备份'),
('sys.performance', 'true', '性能监控', 1, '是否启用性能监控');
