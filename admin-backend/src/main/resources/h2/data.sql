-- H2 compatible initial data

-- admin/admin123, user/user123
INSERT INTO sys_user (id, username, password, nickname, email, phone, gender, status, created_by) VALUES
(1, 'admin', '$2a$10$xn24efgu39jFsH6LcRZQFOQSHljm/a3NM1tzkrEpb81cnY.x0gi.m', '超级管理员', 'admin@iflytek.com', '13800000001', 1, 1, 'system'),
(2, 'user', '$2a$10$eXeZOyzS5nkUcrwd205QteV0XZI1XCKYmAl.bh2ZzrFyqtzZmwPzm', '普通用户', 'user@iflytek.com', '13800000002', 1, 1, 'system');

INSERT INTO sys_role (id, role_name, role_key, sort_order, status, description, created_by) VALUES
(1, '超级管理员', 'admin', 1, 1, '拥有系统所有权限', 'system'),
(2, '普通用户', 'user', 2, 1, '普通用户权限', 'system');

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2);

-- Menus
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, sort_order, permission, visible, status) VALUES
(1, 0, '仪表盘', 2, '/dashboard', 'dashboard/index', 'Monitor', 1, NULL, 1, 1),
(2, 0, '系统管理', 1, '/system', NULL, 'Setting', 2, NULL, 1, 1),
(201, 2, '用户管理', 2, '/system/user', 'system/user/index', 'User', 1, 'sys:user:list', 1, 1),
(202, 2, '角色管理', 2, '/system/role', 'system/role/index', 'UserFilled', 2, 'sys:role:list', 1, 1),
(203, 2, '菜单管理', 2, '/system/menu', 'system/menu/index', 'Menu', 3, 'sys:menu:list', 1, 1),
(204, 2, '系统配置', 2, '/system/config', 'system/config/index', 'Tools', 4, 'sys:config:list', 1, 1),
(205, 2, '日志管理', 1, '/system/log', NULL, 'Document', 5, NULL, 1, 1),
(2051, 205, '操作日志', 2, '/system/log/operation', 'system/log/operation', 'EditPen', 1, 'sys:log:list', 1, 1),
(2052, 205, '登录日志', 2, '/system/log/login', 'system/log/login', 'Key', 2, 'sys:log:list', 1, 1),
(2011, 201, '新增用户', 3, NULL, NULL, NULL, 1, 'sys:user:add', 1, 1),
(2012, 201, '编辑用户', 3, NULL, NULL, NULL, 2, 'sys:user:edit', 1, 1),
(2013, 201, '删除用户', 3, NULL, NULL, NULL, 3, 'sys:user:delete', 1, 1),
(2021, 202, '新增角色', 3, NULL, NULL, NULL, 1, 'sys:role:add', 1, 1),
(2022, 202, '编辑角色', 3, NULL, NULL, NULL, 2, 'sys:role:edit', 1, 1),
(2023, 202, '删除角色', 3, NULL, NULL, NULL, 3, 'sys:role:delete', 1, 1),
(2031, 203, '新增菜单', 3, NULL, NULL, NULL, 1, 'sys:menu:add', 1, 1),
(2032, 203, '编辑菜单', 3, NULL, NULL, NULL, 2, 'sys:menu:edit', 1, 1),
(2033, 203, '删除菜单', 3, NULL, NULL, NULL, 3, 'sys:menu:delete', 1, 1),
(2041, 204, '编辑配置', 3, NULL, NULL, NULL, 1, 'sys:config:edit', 1, 1);

-- Role-menu: admin gets all menus
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- Role-menu: user gets limited menus
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1),
(2, 2),
(2, 201),
(2, 204);

-- System config
INSERT INTO sys_config (config_key, config_value, config_name, config_type, description) VALUES
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
