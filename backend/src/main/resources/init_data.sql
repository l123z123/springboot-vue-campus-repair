-- 测试账号 (密码均为 123456 的 BCrypt 哈希)
-- 若登录失败，可在启动后使用 BCryptPasswordEncoder.encode("123456") 生成新哈希并更新此处

SET NAMES utf8mb4;

-- 学生账号（real_name 全项目唯一；phone / email 满足唯一约束；演示邮箱可收验证码或使用你自行替换为真实 QQ 邮箱）
INSERT INTO `sys_user` (`user_id`, `username`, `password`, `nickname`, `department`, `role`, `phone`, `email`, `real_name`, `status`, `is_deleted`, `create_time`, `update_time`)
VALUES (1, '20260001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '子涵', '计算机学院', 0, '19900000013', 'student20260001@campusrepair.demo', '周子涵', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `nickname` = VALUES(`nickname`), `department` = VALUES(`department`), `phone` = VALUES(`phone`), `email` = VALUES(`email`), `real_name` = VALUES(`real_name`), `update_time` = NOW();

-- 维修员账号
INSERT INTO `sys_user` (`user_id`, `username`, `password`, `nickname`, `department`, `role`, `phone`, `email`, `real_name`, `status`, `is_deleted`, `create_time`, `update_time`)
VALUES (2, 'worker01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '老陈', '后勤保障部', 1, '19900000012', 'worker01@campusrepair.demo', '陈立军', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `nickname` = VALUES(`nickname`), `department` = VALUES(`department`), `phone` = VALUES(`phone`), `email` = VALUES(`email`), `real_name` = VALUES(`real_name`), `update_time` = NOW();

-- 管理员账号
INSERT INTO `sys_user` (`user_id`, `username`, `password`, `nickname`, `department`, `role`, `phone`, `email`, `real_name`, `status`, `is_deleted`, `create_time`, `update_time`)
VALUES (3, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '文轩', '校务处', 2, '19900000011', 'admin@campusrepair.demo', '马文轩', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `nickname` = VALUES(`nickname`), `department` = VALUES(`department`), `phone` = VALUES(`phone`), `email` = VALUES(`email`), `real_name` = VALUES(`real_name`), `update_time` = NOW();
