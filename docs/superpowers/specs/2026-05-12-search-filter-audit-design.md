# 搜索/筛选功能审计修复 + E2E 测试

> 状态：已批准 | 日期：2026-05-12

## 审计发现的 Bug

| # | 严重度 | 页面 | 问题 |
|---|:-----:|------|------|
| 1 | 严重 | Admin 工单管理 | placeholder 承诺搜"工单号/报修人/故障描述"，后端只搜 `title`+`location` |
| 2 | 高 | Student 工单列表 | placeholder 承诺搜"故障描述"，但 `description` 未被搜索 |
| 3 | 中 | Admin 仪表盘 | 待办列表 `statusIn: '0,1,11'`，状态码 `11` 不存在，漏掉 `3`（待派单） |

## 修复方案

### 1. RepairServiceImpl.page() — 扩展 keyword 搜索（Bug 1+2）

```java
if (StringUtils.hasText(keyword)) {
    String kw = keyword.trim();
    // 识别工单号：T90001 或纯数字 90001 → order_id 精确匹配
    Long numericId = null;
    if (kw.toUpperCase().startsWith("T")) {
        try { numericId = Long.parseLong(kw.substring(1)); } catch (NumberFormatException ignored) {}
    } else {
        try { numericId = Long.parseLong(kw); } catch (NumberFormatException ignored) {}
    }
    // studentName 是 @TableField(exist=false)，不是数据库字段，不能用 MyBatis-Plus 直接搜
    if (numericId != null) {
        q.and(w -> w.eq(RepairOrder::getOrderId, numericId)
                      .or().like(RepairOrder::getTitle, kw)
                      .or().like(RepairOrder::getLocation, kw)
                      .or().like(RepairOrder::getDescription, kw));
    } else {
        q.and(w -> w.like(RepairOrder::getTitle, kw)
                      .or().like(RepairOrder::getLocation, kw)
                      .or().like(RepairOrder::getDescription, kw));
    }
}
```

### 2. OrderManage.vue — placeholder 修正

```
原: "搜索地点或故障描述..."
改: "搜索工单号、地点或故障描述"
```

Tickets.vue 的 placeholder/hint 已有 `"工单号/报修人/故障描述"` 和 `"支持按工单号、报修人、描述搜索"`，需去掉"报修人"（studentName 非 DB 字段，无法实现）。

### 3. Dashboard.vue — 修正 statusIn（Bug 3）

```javascript
// 第 310 行
statusIn: '0,1,3'  // 原为 '0,1,11'，11 不存在，3(待派单)被遗漏
```

## E2E 测试设计

新增 `tests/e2e/specs/search-filter.spec.js`：

| # | 用例 | 操作 | 断言 |
|---|------|------|------|
| 1 | 按工单号搜（T前缀） | 搜索 `T90001` | 结果包含工单 90001 |
| 2 | 按纯数字搜 | 搜索 `90001` | 结果包含工单 90001 |
| 3 | 按故障描述搜 | 搜索"空调" | 结果列表非空，匹配 title/description |
| 4 | 按地点搜 | 搜索"教学楼" | 结果列表非空，全部 location 包含"教学楼" |
| 5 | Admin 状态筛选 5 tab | 逐一选"全部/待办/在办/完工/结束" | 每条结果 status 在 tab 范围内 |
| 6 | Student 状态筛选 5 tab | 同上 | 同上 |
| 7 | Dashboard 待办列表 | 访问 admin dashboard | 待办工单全部 status IN (0,1,3)，无其他 |
| 8 | 用户管理搜 username | 搜 `admin` | 结果包含 admin 用户 |
| 9 | 用户管理搜 realName | 搜"子涵" | 结果包含周子涵 |
| 10 | 反馈管理搜 content | 搜"反馈" | 结果非空 |

## 改动文件

| 文件 | 类型 |
|------|------|
| `RepairServiceImpl.java` | Bug 修复 |
| `OrderManage.vue` | 文案修正 |
| `Tickets.vue` | 文案修正 |
| `Dashboard.vue` | Bug 修复 |
| `tests/e2e/specs/search-filter.spec.js` | 新增测试 |

## 不在范围

- ticketNo 持久化到数据库（当前仅前端合成）
- 报修人名字 JOIN 搜索（RepairOrder 已有 studentName 冗余字段）
- Staff 工单页加搜索框
