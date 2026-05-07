-- 已有库场景：为演示三账号补齐绑定邮箱（与 init_data 一致），便于「忘记密码」走 QQ 邮箱验证码流程。
-- 请将下列邮箱改为你能收件的真实地址（须全库 uk_sys_user_email 唯一），再执行本脚本。
-- mysql -h 127.0.0.1 -P 3307 -u root -p campus_repair < sql/patch_demo_account_emails.sql

SET NAMES utf8mb4;
USE campus_repair;

UPDATE sys_user SET email = 'student20260001@campusrepair.demo' WHERE username = '20260001' AND (email IS NULL OR email = '');
UPDATE sys_user SET email = 'worker01@campusrepair.demo' WHERE username = 'worker01' AND (email IS NULL OR email = '');
UPDATE sys_user SET email = 'admin@campusrepair.demo' WHERE username = 'admin' AND (email IS NULL OR email = '');

SELECT user_id, username, role, email FROM sys_user WHERE username IN ('admin', 'worker01', '20260001');
