# 代码质量与防御性编程规范 (Code Quality & Stability Rules)

## 1. 核心目标
- **零空指针 (NPE)**: 杜绝 `NullPointerException`。
- **数据一致性**: 确保数据库操作要么全成功，要么全失败。
- **参数安全**: 拒绝非法输入进入业务逻辑层。
- **可维护性**: 代码逻辑清晰，异常处理明确。

## 2. 后端开发铁律 (Backend Rules)

### 2.1 事务管理 (@Transactional)
- **规则**: 任何涉及**写操作** (Insert/Update/Delete) 或多步业务逻辑的方法，**必须**添加 `@Transactional(rollbackFor = Exception.class)`。
- **场景**: 
  - 审核、派单、开始维修、完工等状态流转必须在同一事务。
  - 完工操作 (改订单状态 + 插入评价表) 必须在同一事务。
- **禁忌**: 禁止在事务内进行耗时操作 (如 HTTP 请求、大文件 IO)，若有需异步处理。

### 2.2 参数校验 (Validation)
- **规则**: Controller 层接收参数必须使用 `@Valid` 或 `@Validated`。
- **实现**: DTO 类字段上加注解 (`@NotNull`, `@NotBlank`, `@Min`, `@Max`, `@Pattern`)。
- **示例**: 
  ```java
  public Result dispatchOrder(@Valid @RequestBody DispatchDTO dto) { ... }
  // DTO 中: @NotNull(message = "订单ID不能为空") private Long orderId;