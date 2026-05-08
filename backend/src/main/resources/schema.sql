-- 校园报修系统 - 建表脚本 (MySQL 8.4, utf8mb4)
-- 使用库名与 application.yml 一致: campus_repair

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- sys_user (与实体 SysUser 对齐: user_id 主键)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL COMMENT '主键',
  `username` varchar(50) NOT NULL COMMENT '学号/工号',
  `password` varchar(100) NOT NULL COMMENT 'BCrypt 加密',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像 URL',
  `department` varchar(50) DEFAULT NULL COMMENT '院系',
  `role` tinyint NOT NULL DEFAULT '0' COMMENT '0-学生 1-维修员 2-管理员',
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱(小写，唯一)',
  `real_name` varchar(50) DEFAULT NULL,
  `signature` varchar(255) DEFAULT NULL COMMENT '个性签名',
  `gender` tinyint DEFAULT '0' COMMENT '0-未知 1-男 2-女',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像链接',
  `status` tinyint DEFAULT '1' COMMENT '0-禁用 1-正常',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除 0-未删 1-已删',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_sys_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- ----------------------------
-- repair_order (与实体 RepairOrder 对齐)
-- ----------------------------
DROP TABLE IF EXISTS `repair_order`;
CREATE TABLE `repair_order` (
  `order_id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '报修人 ID',
  `repairman_id` bigint DEFAULT NULL COMMENT '维修员 ID',
  `title` varchar(100) DEFAULT NULL COMMENT '标题',
  `description` text COMMENT '描述',
  `location` varchar(100) DEFAULT NULL COMMENT '地点',
  `phone_number` varchar(20) DEFAULT NULL COMMENT '报修人手机号',
  `campus` varchar(20) NOT NULL DEFAULT '' COMMENT '校区',
  `area` varchar(50) NOT NULL DEFAULT '' COMMENT '校区内具体区域',
  `category` varchar(50) NOT NULL DEFAULT '' COMMENT '报修种类编码',
  `urgency` tinyint NOT NULL DEFAULT '1' COMMENT '1-低 2-中 3-高',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '派单版状态码：0待审核 1审核中 3待派单 4已派单 5维修中 6维修完成 7学生确认 8已评价 9已拒绝 10已取消',
  `images` text COMMENT '图片 URL 列表 JSON',
  `remark` text COMMENT '备注',
  `is_urgent` tinyint(1) DEFAULT 0 COMMENT '是否一键急诊',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `dispatch_time` datetime DEFAULT NULL COMMENT '派单时间',
  `start_time` datetime DEFAULT NULL COMMENT '开始维修时间',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认完成时间',
  `completed_images` text COMMENT '维修完成照片',
  `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_repairman_id` (`repairman_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报修单表';

-- ----------------------------
-- repair_evaluation 评价表（与实体 RepairEvaluation 一致）
-- ----------------------------
DROP TABLE IF EXISTS `repair_evaluation`;
CREATE TABLE `repair_evaluation` (
  `eval_id` bigint NOT NULL COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '工单ID，一单一评',
  `score` int DEFAULT NULL COMMENT '评分1-5',
  `comment` varchar(500) DEFAULT NULL,
  `is_anonymous` tinyint DEFAULT '0' COMMENT '0-实名 1-匿名',
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`eval_id`),
  UNIQUE KEY `uk_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价表';

-- ----------------------------
-- order_log
-- ----------------------------
DROP TABLE IF EXISTS `order_log`;
CREATE TABLE `order_log` (
  `id` bigint NOT NULL COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '工单 ID',
  `operator_id` bigint NOT NULL COMMENT '操作人 ID',
  `action` varchar(50) NOT NULL COMMENT '动作类型',
  `content` text COMMENT '内容',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';

-- ----------------------------
-- operation_log (AOP 操作日志)
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id` bigint NOT NULL,
  `operator_id` bigint DEFAULT NULL,
  `action` varchar(50) NOT NULL,
  `ip` varchar(50) DEFAULT NULL,
  `params` text,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AOP 操作日志';

-- ----------------------------
-- chat_message 聊天消息表
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '关联工单ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `content` text COMMENT '消息内容',
  `images` text COMMENT '图片URL列表JSON',
  `is_read` tinyint(1) DEFAULT 0 COMMENT '是否已读',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- ----------------------------
-- sys_feedback 意见反馈表
-- ----------------------------
DROP TABLE IF EXISTS `sys_feedback`;
CREATE TABLE `sys_feedback` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint DEFAULT NULL COMMENT '提交人ID',
  `content` varchar(500) NOT NULL COMMENT '反馈内容',
  `contact_info` varchar(50) DEFAULT NULL COMMENT '联系方式(手机/邮箱)',
  `status` tinyint DEFAULT 0 COMMENT '状态: 0-待处理, 1-已处理',
  `reply` varchar(500) DEFAULT NULL COMMENT '管理员回复',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统反馈表';

-- ----------------------------
-- sys_notice 公告表
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `id` bigint NOT NULL COMMENT '主键',
  `title` varchar(100) NOT NULL COMMENT '公告标题',
  `content` varchar(500) NOT NULL COMMENT '公告内容',
  `author` varchar(50) DEFAULT '后勤管理处' COMMENT '发布者',
  `pinned` tinyint DEFAULT '0' COMMENT '0-普通 1-置顶',
  `create_time` datetime DEFAULT NULL COMMENT '发布时间',
  PRIMARY KEY (`id`),
  KEY `idx_pinned_time` (`pinned`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统公告表';

SET FOREIGN_KEY_CHECKS = 1;
