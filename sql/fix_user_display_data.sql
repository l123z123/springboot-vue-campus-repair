-- =============================================================================
-- 修正用户表「姓名/部门」乱码或占位，并补全 real_name（管理端用户管理、工单列表报修人显示）
-- 在 init_data + init_all_accounts 之后执行；或历史库单独执行本文件即可
--
-- Windows CMD 下若中文乱码：先执行 chcp 65001，再导入；或使用：
--   mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3307 -u root -p campus_repair < sql/fix_user_display_data.sql
-- =============================================================================
SET NAMES utf8mb4;
USE campus_repair;

-- 下述 phone 一账号一号，与 init_all_accounts.sql 一致，满足 uk_phone 全库唯一
UPDATE sys_user SET
  nickname = '文轩',
  real_name = '马文轩',
  department = '校务处',
  phone = '19900000011'
WHERE username = 'admin';

UPDATE sys_user SET
  nickname = '老陈',
  real_name = '陈立军',
  department = '后勤保障部',
  phone = '19900000012'
WHERE username = 'worker01';

UPDATE sys_user SET
  nickname = '子涵',
  real_name = '周子涵',
  department = '计算机学院',
  phone = '19900000013'
WHERE username = '20260001';

UPDATE sys_user SET
  nickname = '刘师傅',
  real_name = '刘建华',
  department = '后勤保障部',
  phone = '13811000001'
WHERE username = 'repair01';

UPDATE sys_user SET
  nickname = '李师傅',
  real_name = '李建国',
  department = '后勤保障部',
  phone = '13811000002'
WHERE username = 'repair02';

UPDATE sys_user SET
  nickname = '大伟',
  real_name = '孙大伟',
  department = '后勤保障部',
  phone = '13811000003'
WHERE username = 'repair03';

UPDATE sys_user SET
  nickname = '小许',
  real_name = '许文博',
  department = '后勤保障部',
  phone = '13811000004'
WHERE username = 'repair04';

UPDATE sys_user SET
  nickname = '张浩',
  real_name = '张浩',
  department = '计算机学院',
  phone = '13911000001'
WHERE username = 'stu001';

UPDATE sys_user SET
  nickname = '思琪',
  real_name = '李思琪',
  department = '外国语学院',
  phone = '13911000002'
WHERE username = 'stu002';

UPDATE sys_user SET
  nickname = '梓晨',
  real_name = '王梓晨',
  department = '机电工程学院',
  phone = '13911000003'
WHERE username = 'stu003';

UPDATE sys_user SET
  nickname = '心怡',
  real_name = '赵心怡',
  department = '经济管理学院',
  phone = '13911000004'
WHERE username = 'stu004';

SELECT user_id, username, real_name, department FROM sys_user ORDER BY user_id;
