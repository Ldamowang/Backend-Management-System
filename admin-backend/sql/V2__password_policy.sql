-- V2: Password policy migration
-- Add password_changed_time to sys_user
-- Create sys_password_history table
-- Insert default password policy configs

-- 1. Add password_changed_time column to sys_user
ALTER TABLE sys_user ADD COLUMN password_changed_time DATETIME NULL COMMENT '密码最后修改时间' AFTER last_login_ip;

-- 2. Create password history table
CREATE TABLE IF NOT EXISTS sys_password_history (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  password VARCHAR(200) NOT NULL COMMENT '历史密码(加密)',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  INDEX idx_user_id (user_id),
  INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码历史记录表';

-- 3. Insert default password policy configs
INSERT INTO sys_config (config_key, config_value, config_name, config_type, description) VALUES
('sys.password.minLength', '8', '密码最小长度', 1, '密码最小长度要求'),
('sys.password.maxLength', '32', '密码最大长度', 1, '密码最大长度限制'),
('sys.password.requireUppercase', 'true', '要求大写字母', 1, '密码是否需要包含大写字母'),
('sys.password.requireLowercase', 'true', '要求小写字母', 1, '密码是否需要包含小写字母'),
('sys.password.requireDigit', 'true', '要求数字', 1, '密码是否需要包含数字'),
('sys.password.requireSpecial', 'false', '要求特殊字符', 1, '密码是否需要包含特殊字符'),
('sys.password.expireDays', '90', '密码过期天数', 1, '密码过期天数(0表示永不过期)'),
('sys.password.historyCount', '5', '历史密码检查数量', 1, '不能与最近N次密码重复');
