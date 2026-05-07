================================================================================
校园报修 - 样例 / 测试数据 说明（与当前派单版状态机一致）
================================================================================

一、根目录下 SQL 已收敛
  - 已删除：早期 insert_* / seed_demo_data / 根目录旧 schema 与 init_data 等，避免与 backend 实现不一致或重复执行。
  - 正式建表与账号请始终使用 backend\src\main\resources\ 下文件。

二、推荐执行顺序（演示 / 录屏 / 造数）
  1) 建表：backend\src\main\resources\schema.sql
  2) 若是旧库升级：执行 sql\migration_add_sys_user_email.sql（补 email/signature/gender/avatar_url/uk_phone，脚本可重复执行）
  3) 仅极旧库缺评价表时：migration_repair_evaluation_table.sql
  4) 基础账号：backend\src\main\resources\init_data.sql
     （学号 20260001、维修工 worker01、管理员 admin，密码 123456）
  5) 多账号（可选，便于多学生/多维修工分散数据）：init_all_accounts.sql
  6) 若用户管理/工单列表出现姓名乱码或「用户+数字」：执行 sql\fix_user_display_data.sql
  7) 清理并发测试残留：sql\delete_concurrent_test_repair_orders.sql
  8) 样例 50 条工单 + 8 条评价 + 12 条聊天：sql\seed_repair_orders_50.sql
  9) 需要重新生成样例时：python scripts\gen_seed_50.py
  10) 修改后端或前端布局后请重启 Spring Boot / 刷新 Vite 页面

三、样例数据内容
  - 工单主键 90001～90050，覆盖派单版状态 0/1/3/4/5/6/7/8/9/10（与 RepairServiceImpl 一致，含 3/4 待派/已派）。
  - 评价与工单 90029～90038 一一对应（1～5 分 + 短评）。
  - 报修人姓名与账号对应：@s1=周子涵(20260001)、@s2=张浩(stu001)、@s3=李思琪(stu002)、@s4=王梓晨(stu003)、@s5=赵心怡(stu004)。
  - 图片列为 JSON 空数组 []，避免外网占位图不可达；需要展示图可手工改某单 images 字段。

四、与 docker-compose
  - MySQL 端口 3307:3306、库名 campus_repair、root 密码见 docker-compose.yml
  - 本机执行 SQL 时连接 127.0.0.1:3307 即可
  - Windows 命令行中文乱码：先 chcp 65001，或加参数：
      mysql --default-character-set=utf8mb4 -h 127.0.0.1 -P 3307 -u root -p campus_repair < sql\某脚本.sql
  - 若报 Column 'user_id' cannot be null：先确保已导入 backend\init_data.sql（或库内至少有一名学生 role=0），再执行 sql\seed_repair_orders_50.sql
  - 若报 Duplicate entry '13xxxxxxxx' for key 'sys_user.uk_phone'：库中对 phone 唯一；init_all_accounts 与 fix_user_display_data 已改为分段唯一号（13811/13911/19900000011 起）。勿多人共用同一手机号。若曾导入旧版脚本，可先查重复：SELECT user_id,username,phone FROM sys_user WHERE phone IN (SELECT phone FROM sys_user GROUP BY phone HAVING COUNT(*)>1);
  - root 密码与 docker-compose 中 MYSQL_ROOT_PASSWORD 一致（默认可为 123695487q）；1045 即密码错误（与 compose 中不一致或手误）。
  - 管理端出现大量「并发测试-…」标题工单：为 RepairServiceConcurrentTest 写入的测试数据。可执行 sql\delete_concurrent_test_repair_orders.sql 一次清理；此后跑该测试类会在用例结束自动删（见 JdbcTemplate @AfterEach）。
  - 邮箱验证码：QQ SMTP 配在 backend\src\main\resources\application.yml 的 spring.mail 下；Redis 未启动时邮箱验证码会使用进程内内存降级，正式验收仍建议启动 Redis。

五、init_all_accounts.sql
  - 在 init_data 基础上【补充】 repair02～04、stu001～004，不整表 DELETE。

================================================================================
