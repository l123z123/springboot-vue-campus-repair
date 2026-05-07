-- 学生注册邮箱 + 个人资料字段（旧库可重复执行）
SET NAMES utf8mb4;
USE campus_repair;

-- MySQL：多行 NULL 不违反 UNIQUE；以下用 information_schema 做幂等判断，避免列/索引已存在时报错中断。
SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'email') = 0,
  'ALTER TABLE sys_user ADD COLUMN email VARCHAR(100) NULL COMMENT ''邮箱(小写存储，唯一)'' AFTER phone',
  'SELECT ''email column exists'' AS message'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_sys_user_email') = 0,
  'CREATE UNIQUE INDEX uk_sys_user_email ON sys_user (email)',
  'SELECT ''uk_sys_user_email exists'' AS message'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'signature') = 0,
  'ALTER TABLE sys_user ADD COLUMN signature VARCHAR(255) NULL COMMENT ''个性签名'' AFTER real_name',
  'SELECT ''signature column exists'' AS message'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'gender') = 0,
  'ALTER TABLE sys_user ADD COLUMN gender TINYINT DEFAULT 0 COMMENT ''0-未知 1-男 2-女'' AFTER signature',
  'SELECT ''gender column exists'' AS message'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'avatar_url') = 0,
  'ALTER TABLE sys_user ADD COLUMN avatar_url VARCHAR(255) NULL COMMENT ''头像链接'' AFTER gender',
  'SELECT ''avatar_url column exists'' AS message'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl := IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_phone') = 0,
  'CREATE UNIQUE INDEX uk_phone ON sys_user (phone)',
  'SELECT ''uk_phone exists'' AS message'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
