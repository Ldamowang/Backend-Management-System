-- V4: Audit enhancement migration
-- Add target tracking and before/after data columns to sys_operation_log

-- 1. Add audit enhancement columns
ALTER TABLE sys_operation_log
    ADD COLUMN target_type VARCHAR(64)  NULL COMMENT '目标对象类型（如 user, role, menu）',
    ADD COLUMN target_id   BIGINT       NULL COMMENT '目标对象ID',
    ADD COLUMN before_data JSON         NULL COMMENT '变更前数据',
    ADD COLUMN after_data  JSON         NULL COMMENT '变更后数据';

-- 2. Add indexes for target-based queries
CREATE INDEX idx_oplog_target ON sys_operation_log (target_type, target_id);
CREATE INDEX idx_oplog_module ON sys_operation_log (module);
