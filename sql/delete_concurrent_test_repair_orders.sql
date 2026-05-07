-- =============================================================================
-- 删除「并发测试」用例产生的工单（单元测试 RepairServiceConcurrentTest 标题形如 并发测试-1005-0）
-- 会先删评价 / 操作日志 / 聊天，再删 repair_order，避免孤儿数据。
--
-- 用法（与 docker-compose 中 root 密码一致）:
--   mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3307 -u root -p campus_repair < sql/delete_concurrent_test_repair_orders.sql
-- =============================================================================
SET NAMES utf8mb4;
USE campus_repair;

DELETE e FROM repair_evaluation e
INNER JOIN repair_order r ON e.order_id = r.order_id
WHERE r.title LIKE '并发测试-%';

DELETE l FROM order_log l
INNER JOIN repair_order r ON l.order_id = r.order_id
WHERE r.title LIKE '并发测试-%';

DELETE c FROM chat_message c
INNER JOIN repair_order r ON c.order_id = r.order_id
WHERE r.title LIKE '并发测试-%';

DELETE FROM repair_order WHERE title LIKE '并发测试-%';

SELECT CONCAT('已清理并发测试工单，当前 repair_order 条数: ', COUNT(*)) AS message
FROM repair_order;
