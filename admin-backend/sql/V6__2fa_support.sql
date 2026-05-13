-- =============================================
-- P0.1 双因素认证（2FA）
-- =============================================

CREATE TABLE IF NOT EXISTS `sys_user_2fa` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
    `secret_key`   VARCHAR(64)  NOT NULL COMMENT 'TOTP 密钥(Base32)',
    `enabled`      TINYINT      DEFAULT 0 COMMENT '0=未启用 1=已启用',
    `backup_codes` TEXT         DEFAULT NULL COMMENT 'JSON 数组，一次性恢复码',
    `created_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户双因素认证表';

-- 系统配置：2FA 强制策略（默认不强制）
INSERT INTO sys_config (config_key, config_value, config_name, config_type, description)
VALUES ('security.2fa.enforced', 'false', '强制开启2FA', 1, '是否强制所有用户开启双因素认证')
ON DUPLICATE KEY UPDATE config_key = config_key;
