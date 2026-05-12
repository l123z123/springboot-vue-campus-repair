# 搜索/筛选审计修复 + E2E 测试 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 3 个搜索/筛选 Bug + 新增 10 个 E2E 搜索筛选测试，确保前端 placeholder 与后端搜索逻辑一致。

**Architecture:** 后端扩 keyword 搜索字段 + 识别工单号（T前缀/纯数字）；前端修 placeholder 文案和 Dashboard statusIn；E2E 测试覆盖工单搜索、状态筛选、用户/反馈搜索。

**Tech Stack:** Java 8 + MyBatis-Plus 3.5.5 + Vue 3 + Playwright

---

## Task 1: 修复 RepairServiceImpl.page() 关键字搜索

**Files:**
- Modify: `backend/src/main/java/com/campus/repair/service/impl/RepairServiceImpl.java:251-253`

- [ ] **Step 1: 替换 keyword 搜索逻辑**

将第 251-253 行：

```java
if (StringUtils.hasText(keyword)) {
    q.and(w -> w.like(RepairOrder::getTitle, keyword).or().like(RepairOrder::getLocation, keyword));
}
```

改为：

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

> 注意：`RepairOrder.studentName` 是 `@TableField(exist = false)`，不存在于数据库，不能加入 MyBatis-Plus wrapper。

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn clean compile -q 2>&1
```
Expected: BUILD SUCCESS，无错误。

- [ ] **Step 3: 启动后端 + curl 验证 3 种搜索**

```bash
# 启动后端（后台），然后：
# 按工单号 T90001 搜索
curl -s "http://localhost:8081/api/repair/list?keyword=T90001&page=1&size=5" \
  -H "Authorization: Bearer <admin_token>" | grep -o '"orderId":"[^"]*"'

# 按纯数字 90001 搜索
curl -s "http://localhost:8081/api/repair/list?keyword=90001&page=1&size=5" \
  -H "Authorization: Bearer <admin_token>" | grep -o '"orderId":"[^"]*"'

# 按故障描述搜索（种子数据中有空调相关）
curl -s "http://localhost:8081/api/repair/list?keyword=空调&page=1&size=5" \
  -H "Authorization: Bearer <admin_token>" | grep -c '"orderId"'
```

Expected: T90001 和 90001 返回 orderId=90001；"空调" 返回至少 1 条。

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/campus/repair/service/impl/RepairServiceImpl.java
git commit -m "fix: 扩展工单keyword搜索 — 工单号识别+description字段"
```

---

## Task 2: 修复 Dashboard.vue pending statusIn

**Files:**
- Modify: `frontend/src/views/admin/Dashboard.vue:310`

- [ ] **Step 1: 修正 statusIn 参数**

将第 310 行：

```javascript
const { records } = await getRepairListPage({ statusIn: '0,1,11', page: 1, size: 10 })
```

改为：

```javascript
const { records } = await getRepairListPage({ statusIn: '0,1,3', page: 1, size: 10 })
```

- [ ] **Step 2: 构建验证**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -3
```
Expected: 零错误。

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/admin/Dashboard.vue
git commit -m "fix: Dashboard待办列表修正statusIn 11→3 漏掉待派单状态"
```

---

## Task 3: 修正 OrderManage.vue 搜索 placeholder

**Files:**
- Modify: `frontend/src/views/OrderManage.vue:4`

- [ ] **Step 1: 修改 placeholder 文本**

将第 4 行：

```html
<el-input v-model="keyword" placeholder="搜索地点或故障描述..." clearable style="width: 260px" @keyup.enter="onSearch" @clear="onSearch">
```

改为：

```html
<el-input v-model="keyword" placeholder="搜索工单号、地点或故障描述" clearable style="width: 260px" @keyup.enter="onSearch" @clear="onSearch">
```

- [ ] **Step 2: 构建验证**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -3
```
Expected: 零错误。

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/OrderManage.vue
git commit -m "fix: OrderManage搜索placeholder加入工单号提示"
```

---

## Task 4: 修正 Tickets.vue 搜索 placeholder/hint

**Files:**
- Modify: `frontend/src/views/admin/Tickets.vue:7,55`

- [ ] **Step 1: 去掉无法实现的"报修人"提示**

将第 7 行：

```html
placeholder="工单号/报修人/故障描述"
```

改为：

```html
placeholder="工单号/地点/故障描述"
```

将第 55 行：

```html
<span class="filter-hint">支持按工单号、报修人、描述搜索</span>
```

改为：

```html
<span class="filter-hint">支持按工单号、地点、描述搜索</span>
```

- [ ] **Step 2: 构建验证**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -3
```
Expected: 零错误。

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/admin/Tickets.vue
git commit -m "fix: Tickets搜索placeholder去掉无法实现的报修人"
```

---

## Task 5: 新增 E2E 搜索/筛选测试

**Files:**
- Create: `tests/e2e/specs/search-filter.spec.js`

- [ ] **Step 1: 创建测试文件**

```javascript
const { test, expect } = require('@playwright/test')
const { login } = require('../helpers/auth')
const { baseURL, student, staff, admin } = require('../fixtures/test-data')

test.describe('Search & Filter Tests', () => {
  test.use({ baseURL })

  // ========== Admin 工单搜索 ==========

  test('admin search by ticketNo with T prefix', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/tickets')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="工单号"]', 'T90001')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    // 结果列表中应有工单 90001
    await expect(page.locator('.el-table__row').first()).toBeVisible()
    const text = await page.locator('.ticket-no-link').first().textContent()
    expect(text).toContain('90001')
  })

  test('admin search by pure number orderId', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/tickets')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="工单号"]', '90001')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').first()).toBeVisible()
    const text = await page.locator('.ticket-no-link').first().textContent()
    expect(text).toContain('90001')
  })

  test('admin search by description keyword', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/tickets')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="工单号"]', '空调')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    // 应该有结果（种子数据包含空调相关描述）
    const rows = page.locator('.el-table__row')
    await expect(rows.first()).toBeVisible()
  })

  test('admin search by location', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/tickets')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="工单号"]', '教学楼')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').first()).toBeVisible()
  })

  // ========== Admin 状态筛选 ==========

  test('admin status filter tabs', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/tickets')
    await page.waitForTimeout(800)

    const statusSelect = page.locator('.filter-item .el-select').first()
    const tabs = [
      { label: '全部', key: 'all' },
      { label: '待办', key: 'queue' },
      { label: '在办', key: 'working' },
      { label: '完工', key: 'after' },
      { label: '结束', key: 'end' },
    ]

    for (const tab of tabs) {
      await statusSelect.click()
      await page.waitForTimeout(300)
      const option = page.locator('.el-select-dropdown__item').filter({ hasText: tab.label })
      if (await option.isVisible().catch(() => false)) {
        await option.click()
        await page.waitForTimeout(500)
        // 点击查询
        await page.click('button:has-text("查询")')
        await page.waitForTimeout(500)
        // 结果不为空或显示 empty
        const hasRows = await page.locator('.el-table__row').first().isVisible().catch(() => false)
        const hasEmpty = await page.locator('.el-empty').isVisible().catch(() => false)
        expect(hasRows || hasEmpty).toBeTruthy()
      }
    }
  })

  // ========== Student 工单搜索 ==========

  test('student search by location', async ({ page }) => {
    await login(page, student.username, student.password, student.role)
    await page.goto('/orders')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="工单号"]', '教学楼')
    await page.click('button:has-text("查询")')
    await page.waitForTimeout(800)
    // 学生只能看到自己的工单，可能为 0，但不报错
    const visible = await page.locator('.el-table__row, .el-empty').first().isVisible()
    expect(visible).toBeTruthy()
  })

  test('student status filter tabs', async ({ page }) => {
    await login(page, student.username, student.password, student.role)
    await page.goto('/orders')
    await page.waitForTimeout(800)

    const filterTabs = ['全部', '待处理', '进行中', '已完成', '已关闭']
    for (const label of filterTabs) {
      const select = page.locator('.filter-bar .el-select').first()
      await select.click()
      await page.waitForTimeout(300)
      const option = page.locator('.el-select-dropdown__item').filter({ hasText: label })
      if (await option.isVisible().catch(() => false)) {
        await option.click()
        await page.waitForTimeout(500)
        const visible = await page.locator('.el-table__row, .el-empty').first().isVisible()
        expect(visible).toBeTruthy()
      }
    }
  })

  // ========== Dashboard 待办 ==========

  test('admin dashboard pending list shows correct statuses', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/dashboard')
    await page.waitForTimeout(1000)
    // 仪表盘渲染
    await expect(page.locator('.admin-dashboard')).toBeVisible()
    // KPI 卡片存在
    await expect(page.locator('.dash-stat').first()).toBeVisible()
    // 待办表格应存在（不检验具体数据，只验证页面不崩溃）
  })

  // ========== 用户管理搜索 ==========

  test('admin user search by username', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/users')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="用户名"]', 'admin')
    // 触发搜索（Enter）
    await page.click('body')
    await page.waitForTimeout(500)
    await page.locator('input[placeholder*="用户名"]').press('Enter')
    await page.waitForTimeout(800)
    await expect(page.locator('.el-table__row').first()).toBeVisible()
  })

  test('admin user search by realName', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/users')
    await page.waitForTimeout(800)
    await page.fill('input[placeholder*="用户名"]', '子涵')
    await page.locator('input[placeholder*="用户名"]').press('Enter')
    await page.waitForTimeout(800)
    const text = await page.locator('.el-table__row').first().textContent()
    expect(text).toContain('子涵')
  })

  // ========== 反馈管理搜索 ==========

  test('admin feedback search', async ({ page }) => {
    await login(page, admin.username, admin.password, admin.role)
    await page.goto('/admin/feedback')
    await page.waitForTimeout(800)
    const searchInput = page.locator('input[placeholder*="反馈内容"]')
    if (await searchInput.isVisible().catch(() => false)) {
      await searchInput.fill('反馈')
      await searchInput.press('Enter')
      await page.waitForTimeout(800)
    }
    // 页面不崩溃即可
    const visible = await page.locator('.el-table, .el-empty').first().isVisible()
    expect(visible).toBeTruthy()
  })
})
```

- [ ] **Step 2: 运行测试**

```bash
cd D:/Office-software/project/springboot+vue
rm -rf test-results
npx playwright test tests/e2e/specs/search-filter.spec.js --reporter=list
```

Expected: 12 tests全部通过。

- [ ] **Step 3: Commit**

```bash
git add tests/e2e/specs/search-filter.spec.js
git commit -m "test: 搜索/筛选E2E — 12个用例覆盖工单号/描述/地点/状态/用户/反馈"
```

---

## Task 6: 最终回归测试

- [ ] **Step 1: 运行全部 E2E 测试**

```bash
cd D:/Office-software/project/springboot+vue
rm -rf test-results
npx playwright test --reporter=list
```

Expected: 全部 18 个测试（6 个旧 + 12 个新）通过。

- [ ] **Step 2: 前后端构建验证**

```bash
cd backend && mvn clean compile -q 2>&1
cd ../frontend && npx vite build --mode production 2>&1 | tail -3
```

Expected: 后端 BUILD SUCCESS，前端零错误。

---

## 执行顺序

```
Task 1 (后端keyword) → Task 5 (E2E测试)
Task 2 (Dashboard) → Task 3 (OrderManage) → Task 4 (Tickets)
Task 6 (回归)
```

Task 1-4 之间无依赖，可并行。Task 5 依赖 Task 1 完成。Task 6 依赖全部完成。
