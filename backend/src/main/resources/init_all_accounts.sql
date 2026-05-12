-- =====================================================
-- 在已有 init_data（1=学生 2=维修 3=管理员）基础上【补充】更多测试账号
-- 请先执行：backend/src/main/resources/schema.sql
-- 再执行：backend/src/main/resources/init_data.sql
-- 最后可执行本文件
-- 密码均为 123456
-- 执行：mysql -h 127.0.0.1 -P 3307 -u root -p123695487q campus_repair < init_all_accounts.sql
-- =====================================================

SET NAMES utf8mb4;
USE campus_repair;

SET @pwd := '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi';

-- 不占用 1/2/3，避免与 init_data 冲突（若你库 user_id 已自洽，以 ON DUPLICATE KEY 更新同 username 为准）
-- 手机号全库唯一（uk_phone）：使用 13811/13911 段，避免与旧数据 13800/13900 撞号
-- 与界面展示一致：真实中文姓名 + 部门；可配合 sql/fix_user_display_data.sql
INSERT INTO sys_user (user_id, username, password, nickname, department, role, phone, email, real_name, status, is_deleted, create_time, update_time) VALUES
  (4, 'repair02', @pwd, '李师傅', '后勤保障部', 1, '13811000002', 'repair02@campusrepair.demo', '李建国', 1, 0, NOW(), NOW()),
  (5, 'repair03', @pwd, '大伟', '后勤保障部', 1, '13811000003', 'repair03@campusrepair.demo', '孙大伟', 1, 0, NOW(), NOW()),
  (6, 'repair04', @pwd, '小许', '后勤保障部', 1, '13811000004', 'repair04@campusrepair.demo', '许文博', 1, 0, NOW(), NOW()),
  (7, 'repair01', @pwd, '刘师傅', '后勤保障部', 1, '13811000001', 'repair01@campusrepair.demo', '刘建华', 1, 0, NOW(), NOW()),
  (100, 'stu001', @pwd, '张浩', '计算机学院', 0, '13911000001', 'stu001@campusrepair.demo', '张浩', 1, 0, NOW(), NOW()),
  (101, 'stu002', @pwd, '思琪', '外国语学院', 0, '13911000002', 'stu002@campusrepair.demo', '李思琪', 1, 0, NOW(), NOW()),
  (102, 'stu003', @pwd, '梓晨', '机电工程学院', 0, '13911000003', 'stu003@campusrepair.demo', '王梓晨', 1, 0, NOW(), NOW()),
  (103, 'stu004', @pwd, '心怡', '经济管理学院', 0, '13911000004', 'stu004@campusrepair.demo', '赵心怡', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  nickname = VALUES(nickname),
  department = VALUES(department),
  real_name = VALUES(real_name),
  phone = VALUES(phone),
  email = VALUES(email),
  update_time = NOW();

SELECT '补充账号已写入/更新：repair01、repair02-04、stu001-004（中文姓名/部门）' AS message;
SELECT user_id, username, role, real_name FROM sys_user ORDER BY user_id;
