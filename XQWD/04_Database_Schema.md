# 数据库设计与规范文档 (Database Schema Specification)

> **与现网实现同步（重要）**  
> 实际建表、字段名与状态码以仓库根目录 **`backend/src/main/resources/schema.sql`** 为准；工单状态码与业务含义以 **`XQWD/01_Requirements.md`** 及枚举 **`RepairOrderStatus`** / **`repair_order.status`（派单版）** 为准。  
> 本文档第 2 节中部分示例（如 `order_no`、`student_id`、简化的 6 档状态）为早期草稿，**不等同**于当前 `schema.sql`；若存在冲突，**以 schema.sql 与实体类为准**。  
> **用户表**已包含 **`email`**（唯一，小写存储）、`signature`、`gender`、`avatar_url` 与 `uk_phone`，支撑邮箱验证码注册/登录和个人资料展示。

## 1. 设计原则
- **稳定性优先**: 所有字段必须有明确的 `NOT NULL` 约束（除非业务允许为空）。
- **类型严谨**: 
  - 金额/分数：严禁使用 `FLOAT`/`DOUBLE`，必须使用 `DECIMAL(M, D)`。
  - 状态/类型：使用 `TINYINT` 配合枚举注释，禁止使用 `VARCHAR` 存状态。
  - 时间：统一使用 `DATETIME` 或 `TIMESTAMP`，默认值 `CURRENT_TIMESTAMP`。
  - 主键：统一使用 `BIGINT` (雪花算法 ID)，禁止使用 `INT` (防止数据量大了溢出)。
- **字符集**: 统一 `utf8mb4`，排序规则 `utf8mb4_general_ci`。
- **命名规范**: 
  - 表名：`小写_下划线` (如 `repair_order`)。
  - 字段名：`小写_下划线` (如 `create_time`)。
  - 逻辑删除：所有表必须包含 `is_deleted` (TINYINT, 0-正常, 1-删除)。

## 2. 核心表结构定义 (必填字段)

### 2.1 用户表 (`sys_user`)
| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明/约束 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `user_id` | BIGINT | 20 | YES | - | 主键，雪花算法 |
| `username` | VARCHAR | 50 | YES | - | 唯一索引，登录账号 |
| `password` | VARCHAR | 100 | YES | - | BCrypt 加密存储 |
| `role` | TINYINT | 4 | YES | 0 | 0-学生, 1-维修工, 2-管理员 |
| `phone` | VARCHAR | 20 | YES | - | 唯一索引，用于通知 |
| `real_name`| VARCHAR | 50 | YES | - | 真实姓名 |
| `status` | TINYINT | 4 | YES | 1 | 0-禁用, 1-启用 |
| `is_deleted`| TINYINT | 4 | YES | 0 | 逻辑删除标记 |
| `create_time`| DATETIME | - | YES | NOW() | 创建时间 |
| `update_time`| DATETIME | - | YES | NOW() | 更新时间 (自动更新) |

### 2.2 报修工单表 (`repair_order`) - **核心表**
| 字段名 | 类型 | 长度 | 必填 | 默认值 | 说明/约束 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `order_id` | BIGINT | 20 | YES | - | 主键 |
| `order_no` | VARCHAR | 32 | YES | - | 业务单号 (YYYYMM+6位序列)，唯一索引 |
| `student_id` | BIGINT | 20 | YES | - | 外键 -> sys_user.user_id |
| `worker_id` | BIGINT | 20 | NO | NULL | 外键 -> sys_user.user_id (管理员派单后才有值) |
| `fault_type` | TINYINT | 4 | YES | - | 1-水电, 2-家具, 3-网络, 4-其他 (字典映射) |
| `location` | VARCHAR | 200 | YES | - | 详细位置 (校区-楼-房间) |
| `description`| TEXT | - | YES | - | 故障描述 |
| `images` | TEXT | - | NO | NULL | 图片路径逗号分隔 (最多9张) |
| `status` | TINYINT | 4 | YES | 0 | **派单版状态机核心**: 0-待审核, 1-审核中, 3-待派单, 4-已派单, 5-维修中, 6-维修完成, 7-学生确认, 8-已评价, 9-已拒绝, 10-已取消 |
| `audit_remark`| VARCHAR | 255 | NO | NULL | 审核驳回原因 |
| `dispatch_time` | DATETIME | - | NO | NULL | 管理员派单时间 |
| `complete_time`| DATETIME | - | NO | NULL | 完工时间 |
| `version` | INT | 11 | YES | 0 | **乐观锁版本号** (防止并发更新状态冲突) |
| `is_deleted` | TINYINT | 4 | YES | 0 | 逻辑删除 |
| *索引* | - | - | - | - | 索引: `idx_status_worker` (status, worker_id), `idx_student` (student_id) |

### 2.3 评价表 (`repair_evaluation`)
| 字段名 | 类型 | 长度 | 必填 | 说明/约束 |
| :--- | :--- | :--- | :--- | :--- |
| `eval_id` | BIGINT | 20 | YES | 主键 |
| `order_id` | BIGINT | 20 | YES | 唯一索引 (一单一评) |
| `score` | TINYINT | 4 | YES | 1-5 分 |
| `comment` | VARCHAR | 500 | NO | 评论内容 |
| `is_anonymous`| TINYINT | 4 | YES | 0-实名, 1-匿名 |

## 3. MyBatis-Plus 配置要求
- 启用 **自动填充功能** (MetaObjectHandler) 处理 `create_time`, `update_time`, `is_deleted`。
- 启用 **逻辑删除插件** (LogicDeleteInterceptor)。
- 启用 **乐观锁插件** (OptimisticLockerInterceptor) 用于 `version` 字段。
- 实体类必须使用 `@TableName`, `@TableId`, `@TableLogic`, `@Version` 注解。

## 4. 给 Cursor 的指令
"请根据上述规范，生成完整的 MySQL DDL 建表语句。同时，生成对应的 Java Entity 类，确保包含所有 Lombok 注解、MP 注解以及正确的字段类型映射（如 BigDecimal/LocalDateTime）。"