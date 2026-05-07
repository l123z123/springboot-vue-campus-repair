-- 若你已有历史库、未执行过含 repair_evaluation 的完整 schema，可单独执行本文件补表（不删数据）
-- 新环境请直接执行 backend/src/main/resources/schema.sql

SET NAMES utf8mb4;
USE campus_repair;

CREATE TABLE IF NOT EXISTS `repair_evaluation` (
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
