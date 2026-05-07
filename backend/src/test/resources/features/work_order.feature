# backend/src/test/resources/features/work_order.feature
Feature: 工单生命周期管理

  @critical
  Scenario: 状态流转校验
    Given 工单状态为"待分配"(0)
    When 尝试更新为"已评价"(3)
    Then 系统应返回"状态流转非法"错误

  @critical
  Scenario: 学生提交紧急维修工单
    Given 学生选择"紧急"类型并上传3张图片
    And 管理员分配给技能匹配的维修工
    Then 系统应自动标记高优先级
    And 触发短信通知
    And 设置2小时超时预警
    And 限制维修工同时处理≤3单
