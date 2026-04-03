-- =============================================
-- P0.4 并发登录控制 - 系统配置
-- =============================================

INSERT INTO sys_config (config_key, config_value, config_name, config_type, description)
VALUES ('login.max-sessions', '1', '最大并发会话数', 1, '同一账号允许的最大同时在线数')
ON DUPLICATE KEY UPDATE config_key = config_key;

INSERT INTO sys_config (config_key, config_value, config_name, config_type, description)
VALUES ('login.conflict-policy', 'kick_old', '会话冲突策略', 1, '会话数超限时的处理策略：kick_old=踢旧 reject_new=拒新')
ON DUPLICATE KEY UPDATE config_key = config_key;
