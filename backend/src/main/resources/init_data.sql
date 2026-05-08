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

-- 默认公告数据
INSERT IGNORE INTO `sys_notice` (`id`, `title`, `content`, `author`, `pinned`, `create_time`) VALUES
(1, '关于北苑供水管道检修的通知', '因供水管道老化，北苑区域将于5月10日上午8:00-12:00进行停水检修。请北苑各位同学提前做好储水准备。检修期间如发现漏水等异常情况，请立即通过本系统报修。', '后勤管理处', 1, '2026-05-07 14:00:00'),
(2, '五月份校园设施集中维护计划', '本月将对全校教学楼、宿舍楼公共设施进行集中巡检维护，包括空调清洗、照明检修、门窗加固等项目。各楼栋具体维护时间将提前张贴通知，请留意公告栏。', '后勤管理处', 1, '2026-05-05 09:00:00'),
(3, '后勤维修服务范围说明', '本系统受理范围包括：水电设施维修、门窗修缮、网络故障、家具维修、电器维修等。不属于维修范围的：宿舍个人电器维修、装饰性改造、网络账号问题（请联系信息中心）。', '后勤管理处', 0, '2026-05-01 10:00:00'),
(4, '夏季空调使用及报修注意事项', '夏季来临，请合理设置空调温度（建议26℃以上）。如遇空调不制冷、漏水、异响等问题，请通过本系统提交报修，维修人员将在24小时内响应。请勿私自拆卸空调设备。', '后勤管理处', 0, '2026-04-28 15:30:00');
