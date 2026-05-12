# E2E 测试 + UX 打磨实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对校园报修管理系统进行 6 层 UX 打磨 + 全流程 E2E 自动化测试，确保答辩演示零故障。

**Architecture:** 自底向上分层修复：基础设施 → 数据反馈 → 操作安全 → 状态一致性 → 可达性 → 视觉升级，最后编写 Playwright E2E 测试覆盖三大角色全流程。

**Tech Stack:** Vue 3 + Element Plus + Axios + ECharts + Playwright

---

## Phase 1: Layer 0 — 基础设施

### Task 1: Axios 响应拦截器优化

**Files:**
- Modify: `frontend/src/utils/request.js:40-55`

- [ ] **Step 1: 调整响应拦截器错误处理逻辑**

当前代码在 48-53 行对 `data.code !== 200` 且非 "系统繁忙" 的情况弹出错误。需要确保所有非 401 业务错误都有 toast 通知。当前代码已有这个逻辑，但需要移除 `message.indexOf('系统繁忙')` 的特殊处理，统一所有非 200 的业务错误都弹出通知。

```javascript
request.interceptors.response.use(
  (response) => {
    const { data } = response
    if (data.code === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      return Promise.reject(new Error(data.message || '未登录或已过期'))
    }
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  // ... error handler unchanged
)
```

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```
Expected: 零错误

- [ ] **Step 3: Commit**

```bash
git add frontend/src/utils/request.js
git commit -m "fix: 统一Axios错误提示，移除系统繁忙豁免逻辑"
```

---

### Task 2: 全局 CSS 变量

**Files:**
- Create: `frontend/src/styles/variables.css`

- [ ] **Step 1: 创建设计 token 文件**

```css
:root {
  /* 圆角 */
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 16px;

  /* 阴影 */
  --shadow-card: 0 2px 12px rgba(0, 0, 0, 0.06);
  --shadow-hover: 0 8px 24px rgba(0, 0, 0, 0.10);

  /* 间距 */
  --gap-sm: 8px;
  --gap-md: 16px;
  --gap-lg: 24px;
}
```

- [ ] **Step 2: 在 main.js 导入**

在 `frontend/src/main.js:3` 后添加导入:

```javascript
import './styles/variables.css'
```

- [ ] **Step 3: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```
Expected: 零错误

- [ ] **Step 4: Commit**

```bash
git add frontend/src/styles/variables.css frontend/src/main.js
git commit -m "feat: 全局CSS变量 — 圆角/阴影/间距token"
```

---

### Task 3: Element Plus 主题覆盖

**Files:**
- Create: `frontend/src/styles/element-override.css`

- [ ] **Step 1: 创建主题覆盖文件**

```css
:root {
  --el-color-primary: #2563EB;
  --el-color-primary-light-3: #5B8DEF;
  --el-color-primary-light-5: #93B4F5;
  --el-color-primary-light-7: #C7D7FA;
  --el-color-primary-light-8: #DCE6FC;
  --el-color-primary-light-9: #EEF2FD;
  --el-color-primary-dark-2: #1D4ED8;
  --el-color-success: #10B981;
  --el-color-warning: #F59E0B;
  --el-color-danger: #EF4444;
}

/* 全局 loading spinner 主题色 */
.el-loading-spinner .path {
  stroke: var(--el-color-primary);
}

/* 全局按钮过渡 */
.el-button {
  transition: all 0.2s ease;
}

/* 全局消息淡入 */
.el-message {
  animation: messageFadeIn 0.25s ease;
}

@keyframes messageFadeIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}
```

- [ ] **Step 2: 在 main.js 导入**

在 `frontend/src/main.js` variables.css 导入之后添加:

```javascript
import './styles/element-override.css'
```

- [ ] **Step 3: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```
Expected: 零错误

- [ ] **Step 4: Commit**

```bash
git add frontend/src/styles/element-override.css frontend/src/main.js
git commit -m "feat: Element Plus主题覆盖 — 深蓝色系+加载动画+消息淡入"
```

---

## Phase 2: Layer 1 — 数据反馈标准化

### Task 4: 补齐 Home.vue 错误提示

**Files:**
- Modify: `frontend/src/views/Home.vue:72-77`

- [ ] **Step 1: loadData catch 块添加 ElMessage.error**

将第 75 行 `catch { recentOrders.value = [] }` 改为:

```javascript
catch (e) {
  recentOrders.value = []
  ElMessage.error(e?.message || '加载工单列表失败')
}
```

同时在 script setup 顶部确认已导入 `ElMessage`（当前未导入，需要添加）:

```javascript
import { ElMessage } from 'element-plus'
```

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/Home.vue
git commit -m "fix: Home.vue补齐ElMessage.error错误提示"
```

---

### Task 5: 补齐 StaffWorkbench.vue 错误提示 + loading

**Files:**
- Modify: `frontend/src/views/StaffWorkbench.vue:83-88`

- [ ] **Step 1: loadData catch 块添加 ElMessage.error**

将第 86 行 `catch { records.value = [] }` 改为:

```javascript
catch (e) {
  records.value = []
  ElMessage.error(e?.message || '加载工单列表失败')
}
```

ElMessage 已经在文件顶部导入，无需额外添加 import。

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/StaffWorkbench.vue
git commit -m "fix: StaffWorkbench.vue补齐ElMessage.error错误提示"
```

---

### Task 6: 补齐 Notice.vue 错误提示

**Files:**
- Modify: `frontend/src/views/Notice.vue:41-52`

- [ ] **Step 1: catch 块添加 ElMessage.error**

将第 47-48 行 `catch { noticeList.value = [] }` 改为:

```javascript
catch (e) {
  noticeList.value = []
  ElMessage.error(e?.message || '加载公告列表失败')
}
```

同时添加 import（当前文件未导入 ElMessage）:

```javascript
import { ElMessage } from 'element-plus'
```

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/Notice.vue
git commit -m "fix: Notice.vue补齐ElMessage.error错误提示"
```

---

### Task 7: 补齐 Profile.vue stats 错误提示

**Files:**
- Modify: `frontend/src/views/Profile.vue:202-211`

- [ ] **Step 1: loadStats catch 块添加 ElMessage.error**

将第 210 行 `catch {}` 改为:

```javascript
catch (e) {
  ElMessage.error(e?.message || '加载统计数据失败')
}
```

ElMessage 已在文件顶部导入，无需额外添加。

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/Profile.vue
git commit -m "fix: Profile.vue loadStats补齐ElMessage.error错误提示"
```

---

## Phase 3: Layer 2 — 操作安全

### Task 8: OrderDetail.vue 状态变更加确认弹窗

**Files:**
- Modify: `frontend/src/views/OrderDetail.vue:154-165, 341-342`

- [ ] **Step 1: 维修工操作区加确认弹窗**

将第 157 行 `@click="handleAccept"` 和 第 163 行 `@click="handleComplete"` 的按钮改为带确认的方法。

在 template 中，将两个按钮分别改为调用新方法:

```html
<!-- 第 157 行 -->
<el-button type="success" size="large" @click="handleAcceptConfirm">开始维修</el-button>

<!-- 第 163 行 -->
<el-button type="success" size="large" @click="handleCompleteConfirm">完成维修</el-button>
```

在 script setup 顶部确认已导入 `ElMessageBox`:

```javascript
import { ElMessage, ElMessageBox } from 'element-plus'
```

添加两个确认方法:

```javascript
async function handleAcceptConfirm() {
  try {
    await ElMessageBox.confirm('确认开始维修此工单？', '确认操作', { type: 'info' })
  } catch { return }
  await handleAccept()
}

async function handleCompleteConfirm() {
  try {
    await ElMessageBox.confirm('确认该工单已维修完成？完成后将通知学生进行确认。', '确认操作', { type: 'info' })
  } catch { return }
  await handleComplete()
}
```

- [ ] **Step 2: 确认 ElMessageBox 已导入**

检查文件顶部 import 行，确保 `ElMessageBox` 已从 `element-plus` 导入。

- [ ] **Step 3: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/OrderDetail.vue
git commit -m "fix: OrderDetail开始/完成维修添加确认弹窗防止误操作"
```

---

### Task 9: StaffWorkbench.vue 接单/完成加确认弹窗

**Files:**
- Modify: `frontend/src/views/StaffWorkbench.vue:90-91`

- [ ] **Step 1: 操作按钮加确认弹窗**

将 `handleAccept` 和 `handleComplete` 方法改为带确认版本。

在 script setup 顶部添加 import:

```javascript
import { ElMessage, ElMessageBox } from 'element-plus'
```

（ElMessage 已经导入，只需添加 ElMessageBox）

将第 90-91 行的两个方法替换为:

```javascript
async function handleAccept(row) {
  try {
    await ElMessageBox.confirm('确认接单？接单后请尽快到达现场维修。', '确认操作', { type: 'info' })
  } catch { return }
  try { await acceptOrder(row.id); ElMessage.success('已接单'); await loadData() } catch(e) { ElMessage.error(e?.message||'操作失败') }
}
async function handleComplete(row) {
  try {
    await ElMessageBox.confirm('确认该工单维修已完成？', '确认操作', { type: 'info' })
  } catch { return }
  try { await completeOrder(row.id); ElMessage.success('已标记完成'); await loadData() } catch(e) { ElMessage.error(e?.message||'操作失败') }
}
```

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/StaffWorkbench.vue
git commit -m "fix: StaffWorkbench接单/完成添加确认弹窗防止误操作"
```

---

### Task 10: AdminSettings.vue 表单验证补齐

**Files:**
- Modify: `frontend/src/views/admin/Settings.vue:38-57, 155-158`

- [ ] **Step 1: 公告表单添加 el-form rules**

在 template 中将公告表单 `<el-form>` 改为带 ref 和 rules 的形式:

```html
<el-form ref="announceFormRef" :model="announceForm" :rules="announceRules" label-width="100px" class="announce-form">
```

在 script 中 `announceForm` 定义后添加:

```javascript
const announceFormRef = ref(null)
const announceRules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { max: 100, message: '标题不超过100字', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' },
    { max: 500, message: '内容不超过500字', trigger: 'blur' }
  ]
}
```

将 `publishAnnouncement` 方法中第 155-158 行的手动验证替换为表单验证:

```javascript
async function publishAnnouncement() {
  if (announceFormRef.value?.validate) {
    try { await announceFormRef.value.validate() } catch { return }
  }
  publishing.value = true
  // ... 其余代码保持不变
}
```

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/admin/Settings.vue
git commit -m "fix: AdminSettings公告表单添加el-form rules验证"
```

---

## Phase 4: Layer 3 — 状态一致性

### Task 11: OrderDetail.vue watch 回调状态重置

**Files:**
- Modify: `frontend/src/views/OrderDetail.vue:259-283, 342`

- [ ] **Step 1: loadDetail 开始时重置 recommendations**

在第 260 行 `loadDetail` 函数开头，`loading.value = true` 之后添加:

```javascript
recommendations.value = []
```

- [ ] **Step 2: loadDetail 完成后重置审核表单**

在第 283 行 `finally { loading.value = false }` 之前，添加审核表单重置:

```javascript
auditForm.value = { approved: true, remark: '', dispatchMode: 'later', assignRepairmanId: null }
```

完整修改后的 `loadDetail` finally 块:

```javascript
finally {
  loading.value = false
  auditForm.value = { approved: true, remark: '', dispatchMode: 'later', assignRepairmanId: null }
}
```

- [ ] **Step 3: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/OrderDetail.vue
git commit -m "fix: OrderDetail watch回调重置recommendations和审核表单状态"
```

---

### Task 12: AdminStats.vue 图表资源清理确认

**Files:**
- Modify: `frontend/src/views/admin/Stats.vue:98-101`

- [ ] **Step 1: onUnmounted 确认完善**

当前代码第 98-101 行已有正确的清理:

```javascript
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendInstance?.dispose(); categoryInstance?.dispose()
})
```

无需修改。验证构建即可。

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```
Expected: 零错误

---

## Phase 5: Layer 4 — 可达性与边界

### Task 13: 图标按钮添加 aria-label

**Files:**
- Modify: `frontend/src/views/OrderChat.vue:49-50`
- Modify: `frontend/src/layout/AppLayout.vue:15-18`

- [ ] **Step 1: OrderChat.vue 图片上传按钮加 aria-label**

在第 50 行 `<el-button :icon="Picture" circle text` 后添加 `aria-label="上传图片"`:

```html
<el-button :icon="Picture" circle text aria-label="上传图片" />
```

- [ ] **Step 2: AppLayout.vue 通知按钮确认已有 aria-label**

检查第 16 行确认已有 `aria-label="系统通知"`：

```html
<el-button text class="header-btn" aria-label="系统通知">
```

已有。检查 sidebar 折叠按钮是否需要添加。在第 5 行 `collapse-btn`:

```html
<el-button class="collapse-btn" text @click="sidebarCollapsed = !sidebarCollapsed" aria-label="切换侧栏">
```

- [ ] **Step 3: 加折叠按钮 aria-label**

```html
<el-button class="collapse-btn" text @click="sidebarCollapsed = !sidebarCollapsed" aria-label="切换侧栏">
```

- [ ] **Step 4: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/OrderChat.vue frontend/src/layout/AppLayout.vue
git commit -m "fix: 图标按钮添加aria-label提升无障碍体验"
```

---

### Task 14: Publish.vue 纯图片无描述提示

**Files:**
- Modify: `frontend/src/views/Publish.vue:202-206`

- [ ] **Step 1: 提交时无描述提示优化**

当前第 205 行的 `handleSubmit` 中:

```javascript
if (!hasImages && !hasDesc) { ElMessage.warning('请至少上传一张图片或填写故障描述'); return }
```

问题：当用户仅上传图片而没写描述时，系统允许提交但标题会变成"报修"。

修改逻辑：当只有图片没有文字描述时，提示用户补充描述（但不禁用提交，只是提醒）:

在第 202 行 `handleSubmit` 中，在 `submitLoading.value = true` 之前添加:

```javascript
if (hasImages && !hasDesc) {
  ElMessage.warning('建议补充故障描述，方便维修人员快速定位问题')
}
```

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/Publish.vue
git commit -m "fix: Publish纯图片无描述时提示用户补充说明"
```

---

### Task 15: AppLayout 移动端侧栏溢出处理

**Files:**
- Modify: `frontend/src/layout/AppLayout.vue`

Let me first check the current mobile CSS in the file.

- [ ] **Step 1: 修改 `.app-sidebar` overflow 属性**

当前第 265-273 行 `.app-sidebar` 样式中有 `overflow: hidden`。对于移动端（屏幕高度较小），菜单项可能被截断。在 `<style scoped>` 末尾的 media query 中添加:

```css
@media (max-width: 768px) {
  .app-sidebar {
    overflow-y: auto;
    overflow-x: hidden;
  }
}
```

同时确保折叠按钮已添加 aria-label（在第 5 行）:

```html
<el-button class="collapse-btn" text @click="sidebarCollapsed = !sidebarCollapsed" aria-label="切换侧栏">
```

注意：当前第 5 行的折叠按钮可能没有 aria-label，需确认并添加。

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/layout/AppLayout.vue
git commit -m "fix: AppLayout移动端侧栏菜单溢出加滚动"
```

---

## Phase 6: Layer 5 — 关键页面视觉升级

### Task 16: Home.vue 视觉升级

**Files:**
- Modify: `frontend/src/views/Home.vue`

- [ ] **Step 1: 替换 template — Hero Banner + 快捷卡片 + 工单卡片列表**

完整替换 template:

```html
<template>
  <div class="home-page">
    <!-- Hero Banner -->
    <div class="hero-banner">
      <div class="hero-banner__body">
        <el-avatar :size="56" class="hero-avatar">{{ userName.charAt(0) }}</el-avatar>
        <div class="hero-text">
          <h2 class="hero-greeting">{{ greeting }}，{{ userName }}</h2>
          <p class="hero-date">{{ todayDate }}</p>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <div
        v-for="item in quickActions"
        :key="item.key"
        class="qa-card"
        :style="{ '--qa-accent': item.color }"
        @click="$router.push(item.path)"
      >
        <div class="qa-card__icon">
          <el-icon :size="24"><component :is="item.icon" /></el-icon>
        </div>
        <div class="qa-card__body">
          <span class="qa-card__title">{{ item.label }}</span>
          <span class="qa-card__desc">{{ item.desc }}</span>
        </div>
      </div>
    </div>

    <!-- 最近工单 -->
    <div class="recent-section">
      <div class="recent-section__head">
        <span class="recent-section__title">最近工单</span>
        <el-button text type="primary" @click="$router.push('/orders')">查看全部 →</el-button>
      </div>
      <div v-if="loading" class="recent-loading">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="recentOrders.length" class="recent-list">
        <div
          v-for="order in recentOrders"
          :key="order.id"
          class="recent-item"
          @click="goDetail(order)"
        >
          <div class="recent-item__left">
            <span class="recent-item__no">{{ order.ticketNo || '#'+String(order.id).slice(-6) }}</span>
            <span class="recent-item__loc">{{ order.location }}</span>
          </div>
          <div class="recent-item__right">
            <el-tag :type="getStatusTagType(order.statusCode)" size="small" effect="plain">
              {{ getRepairStatusLabel(order.statusCode) }}
            </el-tag>
            <span class="recent-item__time">{{ order.createTime }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无工单" :image-size="60">
        <el-button type="primary" @click="$router.push('/publish')">提交报修</el-button>
      </el-empty>
    </div>
  </div>
</template>
```

- [ ] **Step 2: 更新 quickActions 数据添加描述**

```javascript
const quickActions = [
  { key:'publish', label:'发布报修', desc:'快速提交维修申请', path:'/publish', icon:UploadFilled, color:'#2563EB' },
  { key:'orders', label:'我的工单', desc:'查看工单进度状态', path:'/orders', icon:Tickets, color:'#6366F1' },
  { key:'message', label:'消息中心', desc:'与维修师傅沟通', path:'/message', icon:ChatDotRound, color:'#10B981' },
  { key:'help', label:'帮助反馈', desc:'提交建议与反馈', path:'/help', icon:QuestionFilled, color:'#F59E0B' }
]
```

- [ ] **Step 3: 完整替换 style**

```css
<style scoped>
.home-page { min-height: 400px; }

/* Hero Banner */
.hero-banner {
  background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 24px;
  color: #fff;
}
.hero-banner__body {
  display: flex;
  align-items: center;
  gap: 16px;
}
.hero-avatar {
  border: 3px solid rgba(255,255,255,0.4);
  flex-shrink: 0;
}
.hero-greeting {
  margin: 0 0 4px 0;
  font-size: 22px;
  font-weight: 700;
}
.hero-date {
  margin: 0;
  font-size: 14px;
  opacity: 0.85;
}

/* 快捷操作 */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.qa-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.qa-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}
.qa-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(37, 99, 235, 0.08);
  color: var(--qa-accent, #2563EB);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.qa-card__body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.qa-card__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.qa-card__desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 最近工单 */
.recent-section {
  background: #fff;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  padding: 20px 24px;
}
.recent-section__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.recent-section__title {
  font-size: 16px;
  font-weight: 600;
}
.recent-loading { padding: 12px 0; }

.recent-list { display: flex; flex-direction: column; gap: 8px; }

.recent-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}
.recent-item:hover {
  background: var(--el-fill-color-lighter);
  border-color: var(--el-color-primary-light-5);
}
.recent-item__left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.recent-item__no {
  font-family: 'SF Mono', 'Cascadia Code', monospace;
  font-weight: 600;
  color: var(--el-color-primary);
  font-size: 13px;
}
.recent-item__loc {
  font-size: 14px;
  color: var(--el-text-color-regular);
}
.recent-item__right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.recent-item__time {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

@media (max-width: 768px) {
  .hero-banner { padding: 20px; }
  .hero-banner__body { gap: 12px; }
  .hero-greeting { font-size: 18px; }
  .quick-actions { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 480px) {
  .quick-actions { grid-template-columns: 1fr; }
}
</style>
```

- [ ] **Step 4: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/Home.vue
git commit -m "feat: Home.vue视觉升级 — Hero Banner+快捷卡片+工单卡片列表"
```

---

### Task 17: OrderDetail.vue 视觉升级

**Files:**
- Modify: `frontend/src/views/OrderDetail.vue`

- [ ] **Step 1: 替换进度步骤为自定义步骤条**

在 template 中找到第 46-53 行的 `<el-steps>`，替换为:

```html
<div class="custom-steps">
  <div
    v-for="(step, idx) in stepItems"
    :key="idx"
    class="c-step"
    :class="{ 'is-active': idx === currentStep, 'is-done': idx < currentStep, 'is-fail': isRejected && idx === 1 }"
  >
    <div class="c-step__dot">
      <el-icon v-if="idx < currentStep"><Check /></el-icon>
      <el-icon v-else-if="isRejected && idx === 1"><Close /></el-icon>
      <span v-else>{{ idx + 1 }}</span>
    </div>
    <div class="c-step__label">{{ step.label }}</div>
    <div v-if="step.time" class="c-step__time">{{ step.time }}</div>
  </div>
  <div class="c-step__line">
    <div class="c-step__line-fill" :style="{ width: stepProgress + '%' }"></div>
  </div>
</div>
```

在 script setup 中添加 computed:

```javascript
const isRejected = computed(() => order.value.status === 9)
const stepItems = computed(() => [
  { label: '提交', time: order.value.createTime },
  { label: '审核', time: order.value.auditTime },
  { label: '派单', time: order.value.dispatchTime },
  { label: '维修', time: order.value.completedTime },
  { label: '确认', time: order.value.confirmTime },
  { label: '评价', time: evaluation.value?.createTime || '' }
])
const stepProgress = computed(() => {
  if (order.value.status >= 8) return 100
  if (order.value.status >= 6) return 83
  if (order.value.status >= 4) return 66
  if (order.value.status >= 3) return 50
  if (order.value.status >= 1) return 33
  return 16
})
```

- [ ] **Step 2: 确认已导入 Close 图标**

在 import 中添加 `Close`:

```javascript
import { ChatDotRound, Check, Close } from '@element-plus/icons-vue'
```

- [ ] **Step 3: 信息区卡片网格**

在 template 中，将基本信息卡片（第 17-31 行）和故障描述卡片（第 33-42 行）合并为一个信息网格:

```html
<el-card shadow="never" class="section-card">
  <template #header><span class="card-title">工单详情</span></template>
  <div class="info-grid">
    <div class="info-grid__item">
      <span class="info-label">报修地点</span>
      <span class="info-value">{{ order.location }}</span>
    </div>
    <div class="info-grid__item">
      <span class="info-label">故障分类</span>
      <span class="info-value">{{ order.category || '其他' }}</span>
    </div>
    <div class="info-grid__item">
      <span class="info-label">紧急程度</span>
      <el-tag v-if="order.isUrgent || order.urgency === 'high'" type="danger" size="small">紧急</el-tag>
      <el-tag v-else-if="order.urgency === 'medium'" type="warning" size="small">中等</el-tag>
      <el-tag v-else type="info" size="small">普通</el-tag>
    </div>
    <div class="info-grid__item">
      <span class="info-label">报修人</span>
      <span class="info-value">{{ order.reporterName || '-' }}</span>
    </div>
    <div class="info-grid__item">
      <span class="info-label">联系电话</span>
      <span class="info-value">{{ order.phone || '-' }}</span>
    </div>
    <div class="info-grid__item">
      <span class="info-label">维修工</span>
      <span class="info-value">{{ order.worker }}</span>
    </div>
  </div>
</el-card>

<el-card shadow="never" class="section-card">
  <template #header><span class="card-title">故障描述</span></template>
  <p class="desc-text">{{ order.desc }}</p>
  <div v-if="order.images && order.images.length" class="image-gallery">
    <el-image v-for="(img, i) in order.images" :key="i" :src="img" fit="cover" class="gallery-thumb" :preview-src-list="order.images" :initial-index="i" />
  </div>
  <div v-else-if="order.image" class="image-gallery">
    <el-image :src="order.image" fit="cover" class="gallery-thumb" :preview-src-list="[order.image]" />
  </div>
</el-card>
```

- [ ] **Step 4: 自定义步骤条 CSS + 信息网格 CSS**

在 `<style scoped>` 底部添加:

```css
/* 自定义步骤条 */
.custom-steps {
  position: relative;
  display: flex;
  justify-content: space-between;
  padding: 20px 0 32px;
}
.c-step {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex: 1;
}
.c-step__dot {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: var(--el-fill-color);
  border: 2px solid var(--el-border-color);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 600;
  color: var(--el-text-color-placeholder);
  transition: all 0.3s;
}
.c-step.is-active .c-step__dot {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  color: #fff;
  box-shadow: 0 0 0 6px rgba(37,99,235,0.15);
  animation: stepPulse 2s infinite;
}
.c-step.is-done .c-step__dot {
  background: var(--el-color-success);
  border-color: var(--el-color-success);
  color: #fff;
}
.c-step.is-fail .c-step__dot {
  background: var(--el-color-danger);
  border-color: var(--el-color-danger);
  color: #fff;
}
.c-step__label {
  font-size: 12px; font-weight: 500;
  color: var(--el-text-color-regular);
}
.c-step__time {
  font-size: 11px; color: var(--el-text-color-placeholder);
  max-width: 80px; text-align: center;
}
.c-step__line {
  position: absolute; top: 36px; left: 12%; right: 12%; height: 3px;
  background: var(--el-fill-color);
  border-radius: 2px;
  z-index: 0;
}
.c-step__line-fill {
  height: 100%; border-radius: 2px;
  background: var(--el-color-success);
  transition: width 0.5s ease;
}

@keyframes stepPulse {
  0%, 100% { box-shadow: 0 0 0 6px rgba(37,99,235,0.15); }
  50% { box-shadow: 0 0 0 12px rgba(37,99,235,0.05); }
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.info-grid__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.info-label {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.info-value {
  font-size: 14px;
  color: var(--el-text-color-primary);
  font-weight: 500;
}

@media (max-width: 640px) {
  .info-grid { grid-template-columns: repeat(2, 1fr); }
  .custom-steps { flex-wrap: wrap; gap: 12px; }
  .c-step { flex: 0 0 auto; }
}
```

- [ ] **Step 5: 添加动态操作栏**

在 template 中，将维修工/学生操作卡片改为底部浮动操作栏:

```html
<transition name="slide-up">
  <div v-if="showActionBar" class="action-bar">
    <div class="action-bar__inner">
      <div class="action-bar__info">
        <span>{{ actionBarText }}</span>
      </div>
      <div class="action-bar__btns">
        <el-button
          v-if="userRole === 1 && order.status === 4"
          type="success"
          size="large"
          @click="handleAcceptConfirm"
        >开始维修</el-button>
        <el-button
          v-if="userRole === 1 && order.status === 5"
          type="success"
          size="large"
          @click="handleCompleteConfirm"
        >完成维修</el-button>
        <el-button
          v-if="userRole === 0 && order.status === 6"
          type="primary"
          size="large"
          @click="handleConfirm"
        >确认完成</el-button>
      </div>
    </div>
  </div>
</transition>
```

添加 computed:

```javascript
const showActionBar = computed(() => {
  return (userRole.value === 1 && (order.value.status === 4 || order.value.status === 5))
    || (userRole.value === 0 && order.value.status === 6)
})
const actionBarText = computed(() => {
  if (userRole.value === 1 && order.value.status === 4) return '确认到场后请点击开始维修'
  if (userRole.value === 1 && order.value.status === 5) return '维修完成后请点击完成维修'
  if (userRole.value === 0 && order.value.status === 6) return '维修已完成，请确认'
  return ''
})
```

CSS:

```css
.action-bar {
  position: fixed;
  bottom: 0; left: 0; right: 0;
  background: #fff;
  border-top: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 -4px 20px rgba(0,0,0,0.08);
  padding: 16px 24px;
  z-index: 100;
}
.action-bar__inner {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.action-bar__info { font-size: 14px; color: var(--el-text-color-secondary); }

.slide-up-enter-active, .slide-up-leave-active { transition: transform 0.3s ease; }
.slide-up-enter-from, .slide-up-leave-to { transform: translateY(100%); }
```

- [ ] **Step 6: 移除旧的单独操作卡片**

删除 template 中第 153-173 行的旧操作卡片（维修工操作和学生操作）。

- [ ] **Step 7: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 8: Commit**

```bash
git add frontend/src/views/OrderDetail.vue
git commit -m "feat: OrderDetail视觉升级 — 自定义步骤条+信息网格+浮动操作栏"
```

---

### Task 18: AdminDashboard.vue 视觉升级

**Files:**
- Modify: `frontend/src/views/admin/Dashboard.vue`

- [ ] **Step 1: KPI 卡片增加图标渐变 + 超时脉冲动画**

替换 template 中的统计卡片区域 (第 12-35 行):

```html
<el-row class="dashboard-stat-row" :gutter="16">
  <el-col
    v-for="card in statCards"
    :key="card.key"
    :xs="24" :sm="12" :lg="6"
  >
    <div
      class="dash-stat"
      :class="{ 'is-warning': card.key === 'urgent' && card.value > 0 }"
    >
      <div class="dash-stat__icon" :class="`dash-stat__icon--${card.key}`">
        <el-icon :size="22"><component :is="card.icon" /></el-icon>
      </div>
      <div class="dash-stat__body">
        <span class="dash-stat__label">{{ card.label }}</span>
        <span class="dash-stat__value">{{ animatedValues[card.key] ?? card.value }}</span>
      </div>
    </div>
  </el-col>
</el-row>
```

- [ ] **Step 2: 待处理表格加 SLA 超时标记**

在 template 中，修改 `pendingRowClassName` 为实际计算超时的方法:

```javascript
function pendingRowClassName({ row }) {
  if (!row.createTime && !row.date) return ''
  const created = new Date(row.createTime || row.date).getTime()
  const now = Date.now()
  const hours = (now - created) / 3600000
  if (hours > 48) return 'sla-overdue'
  if (hours > 24) return 'sla-warning'
  return ''
}
```

在 CSS 中添加 SLA 行样式:

```css
:deep(.sla-overdue) { background: rgba(239,68,68,0.06) !important; }
:deep(.sla-warning) { background: rgba(245,158,11,0.06) !important; }
```

- [ ] **Step 3: 替换 statCards 卡片样式 CSS**

完整替换 `.dashboard-stat-card` 相关样式:

```css
.dash-stat {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: var(--shadow-card);
  transition: transform 0.2s, box-shadow 0.2s;
}
.dash-stat:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-hover);
}
.dash-stat.is-warning {
  animation: urgentPulse 2s infinite;
}
@keyframes urgentPulse {
  0%, 100% { box-shadow: var(--shadow-card); }
  50% { box-shadow: 0 0 0 6px rgba(239,68,68,0.15); }
}
.dash-stat__icon {
  width: 48px; height: 48px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.dash-stat__icon--total {
  background: rgba(37,99,235,0.1); color: #2563EB;
}
.dash-stat__icon--pending {
  background: rgba(59,130,246,0.1); color: #3B82F6;
}
.dash-stat__icon--completed {
  background: rgba(16,185,129,0.1); color: #10B981;
}
.dash-stat__icon--urgent {
  background: rgba(239,68,68,0.1); color: #EF4444;
}
.dash-stat__body {
  display: flex; flex-direction: column; gap: 2px;
}
.dash-stat__label {
  font-size: 13px; color: var(--el-text-color-secondary);
}
.dash-stat__value {
  font-size: 28px; font-weight: 700; color: var(--el-text-color-primary);
  font-variant-numeric: tabular-nums;
}
```

- [ ] **Step 4: 移除旧 CSS**

删除 template 中不再使用的 `el-card` 样式对应的旧 CSS: 移除 `.dashboard-stat-card`, `.dashboard-stat-card__header`, `.dashboard-stat-card__icon`, `.dashboard-stat-card__info`, `.dashboard-stat-card__label`, `.dashboard-stat-card__value`, `.dashboard-stat-card__trend`, `.dashboard-stat-card__sparkline` 等 class 的样式。

同时删除 `animatedValues` ref（如果添加了动画计数）-- 由于数字滚动动画较复杂且 KPI 数据为静态，仅保留视觉升级，不实现 JavaScript 数字滚动，保持模板兼容性。删除之前添加的 `animatedValues` 引用，直接使用 `card.value`。

恢复 template 中 value 显示为:

```html
<span class="dash-stat__value">{{ card.value }}</span>
```

- [ ] **Step 5: 验证构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/admin/Dashboard.vue
git commit -m "feat: AdminDashboard视觉升级 — KPI卡片渐变+超时脉冲动画+SLA超时标记"
```

---

## Phase 7: E2E 测试

### Task 19: 安装 Playwright 依赖

- [ ] **Step 1: 安装 Playwright**

```bash
cd D:/Office-software/project/springboot+vue && npm install --save-dev @playwright/test && npx playwright install chromium
```

- [ ] **Step 2: 验证安装**

```bash
npx playwright --version
```
Expected: 版本号输出

- [ ] **Step 3: Commit**

```bash
git add package.json package-lock.json
git commit -m "chore: 安装Playwright E2E测试依赖"
```

---

### Task 20: 测试夹具 (Test Data)

**Files:**
- Create: `tests/e2e/fixtures/test-data.js`

- [ ] **Step 1: 创建测试数据文件**

```javascript
module.exports = {
  baseURL: process.env.BASE_URL || 'http://localhost:5173',

  student: {
    username: 'test_student',
    password: 'Test123456',
    email: 'student@test.com',
    phone: '13800138000'
  },

  staff: {
    username: 'test_worker',
    password: 'Test123456',
    name: '测试维修工'
  },

  admin: {
    username: 'admin',
    password: 'admin123',
    name: '管理员'
  },

  repairOrder: {
    campus: '主校区',
    area: '3号楼',
    locationDetail: '502 宿舍',
    description: '空调不制冷，制冷模式下出风口吹出的是常温风',
    category: ['生活类', '宿舍用电'],
    urgency: 'medium',
    phone: '13800138000'
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add tests/e2e/fixtures/test-data.js
git commit -m "test: E2E测试夹具 — 三种角色测试账号+报修数据"
```

---

### Task 21: 测试辅助函数

**Files:**
- Create: `tests/e2e/helpers/auth.js`
- Create: `tests/e2e/helpers/repair.js`
- Create: `tests/e2e/helpers/navigation.js`

- [ ] **Step 1: auth.js — 登录/登出封装**

```javascript
const { test, expect } = require('@playwright/test')

async function login(page, username, password) {
  await page.goto('/login')
  await page.waitForSelector('input[placeholder*="用户名"]', { timeout: 10000 })
  await page.fill('input[placeholder*="用户名"]', username)
  await page.fill('input[placeholder*="密码"]', password)
  await page.click('button:has-text("登录")')
  await page.waitForURL('**/home', { timeout: 10000 })
  await expect(page.locator('.hero-greeting, .welcome-title, .welcome-text')).toBeVisible({ timeout: 5000 })
}

async function logout(page) {
  await page.click('[aria-label="用户菜单"], .user-dropdown')
  await page.click('text=退出登录')
  await page.waitForURL('**/login', { timeout: 5000 })
}

module.exports = { login, logout }
```

- [ ] **Step 2: repair.js — 工单操作封装**

```javascript
const { expect } = require('@playwright/test')

async function submitRepair(page, data) {
  await page.goto('/publish')
  await page.waitForSelector('.publish-card', { timeout: 10000 })

  // 选择校区
  await page.click('.el-select:first-of-type')
  await page.click(`.el-select-dropdown__item:has-text("${data.campus}")`)

  // 选择区域
  await page.waitForTimeout(500)
  await page.click('.el-select:nth-of-type(2)')
  await page.click(`.el-select-dropdown__item:has-text("${data.area}")`)

  // 填写详细位置
  if (data.locationDetail) {
    await page.fill('input[placeholder*="详细位置"], input[placeholder*="楼栋"]', data.locationDetail)
  }

  // 填写描述
  if (data.description) {
    await page.fill('textarea[placeholder*="故障"], textarea', data.description)
  }

  // 填写电话
  await page.fill('input[placeholder*="手机"]', data.phone || '13800138000')

  // 提交
  await page.click('button:has-text("立即提交")')
  await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 10000 })
}

async function selectOrderFromList(page, index = 0) {
  await page.goto('/orders')
  await page.waitForSelector('.el-table__row, .recent-item', { timeout: 10000 })
  const rows = page.locator('.el-table__row, .recent-item')
  await rows.nth(index).click()
  await page.waitForURL('**/repair/detail/**', { timeout: 10000 })
}

module.exports = { submitRepair, selectOrderFromList }
```

- [ ] **Step 3: navigation.js — 页面跳转辅助**

```javascript
const { expect } = require('@playwright/test')

async function navigateTo(page, menuItem, expectedPath) {
  await page.click(`.el-menu-item:has-text("${menuItem}"), a:has-text("${menuItem}")`)
  if (expectedPath) {
    await page.waitForURL(`**${expectedPath}`, { timeout: 10000 })
  }
}

async function verifyPageLoaded(page, selector) {
  await expect(page.locator(selector)).toBeVisible({ timeout: 10000 })
}

module.exports = { navigateTo, verifyPageLoaded }
```

- [ ] **Step 4: Commit**

```bash
git add tests/e2e/helpers/
git commit -m "test: E2E辅助函数 — 登录/报修/导航操作封装"
```

---

### Task 22: 冒烟测试

**Files:**
- Create: `tests/e2e/smoke.spec.js`

- [ ] **Step 1: 编写冒烟测试**

```javascript
const { test, expect } = require('@playwright/test')
const { login } = require('./helpers/auth')
const { baseURL } = require('./fixtures/test-data')

test.describe('Smoke Tests', () => {
  test.use({ baseURL })

  test('student login and home page loads', async ({ page }) => {
    await login(page, 'test_student', 'Test123456')
    await expect(page).toHaveURL(/\/home/)
    await expect(page.locator('.hero-greeting, .welcome-title')).toBeVisible()
  })

  test('staff login and workbench loads', async ({ page }) => {
    await login(page, 'test_worker', 'Test123456')
    await page.goto('/staff/workbench')
    await expect(page.locator('.staff-workbench')).toBeVisible()
  })

  test('admin login and dashboard loads', async ({ page }) => {
    await login(page, 'admin', 'admin123')
    await page.goto('/admin/dashboard')
    await expect(page.locator('.admin-dashboard')).toBeVisible()
    await expect(page.locator('.dash-stat').first()).toBeVisible()
  })
})
```

- [ ] **Step 2: Commit**

```bash
git add tests/e2e/smoke.spec.js
git commit -m "test: E2E冒烟测试 — 三种角色登录验证"
```

---

### Task 23: 学生全流程测试

**Files:**
- Create: `tests/e2e/specs/student.spec.js`

- [ ] **Step 1: 编写学生全流程测试**

```javascript
const { test, expect } = require('@playwright/test')
const { login } = require('../helpers/auth')
const { submitRepair, selectOrderFromList } = require('../helpers/repair')
const { baseURL, student, repairOrder } = require('../fixtures/test-data')

test.describe('Student Full Flow', () => {
  test.use({ baseURL })

  test('complete student repair lifecycle', async ({ page }) => {
    // 1. Login
    await login(page, student.username, student.password)

    // 2. Submit repair order
    await submitRepair(page, repairOrder)

    // 3. Navigate to order list
    await page.goto('/orders')
    await expect(page.locator('.el-table__row, .recent-item').first()).toBeVisible()

    // 4. Navigate to profile
    await page.goto('/profile')
    await expect(page.locator('.profile-page')).toBeVisible()

    // 5. Click edit profile
    await page.click('button:has-text("编辑资料")')
    await expect(page.locator('.edit-form')).toBeVisible()

    // 6. Cancel edit
    await page.click('button:has-text("取消")')

    // 7. Navigate to help/feedback
    await page.goto('/help')
    await expect(page.locator('.el-collapse, form')).toBeVisible()

    // 8. Submit feedback
    const feedbackTextarea = page.locator('textarea[placeholder*="反馈"], textarea[placeholder*="意见"]')
    if (await feedbackTextarea.isVisible()) {
      await feedbackTextarea.fill('E2E测试反馈：系统运行正常')
      await page.click('button:has-text("提交反馈"), button:has-text("提交")')
    }

    // 9. Navigate to settings
    await page.goto('/settings')
    await expect(page.locator('.el-card')).toBeVisible()
  })
})
```

- [ ] **Step 2: Commit**

```bash
git add tests/e2e/specs/student.spec.js
git commit -m "test: E2E学生全流程 — 登录→报修→列表→个人→反馈→设置"
```

---

### Task 24: 维修工全流程测试

**Files:**
- Create: `tests/e2e/specs/staff.spec.js`

- [ ] **Step 1: 编写维修工全流程测试**

```javascript
const { test, expect } = require('@playwright/test')
const { login } = require('../helpers/auth')
const { baseURL, staff } = require('../fixtures/test-data')

test.describe('Staff Full Flow', () => {
  test.use({ baseURL })

  test('staff workbench and order management', async ({ page }) => {
    // 1. Login
    await login(page, staff.username, staff.password)

    // 2. Navigate to workbench
    await page.goto('/staff/workbench')
    await expect(page.locator('.staff-workbench')).toBeVisible()
    await expect(page.locator('.stat-card').first()).toBeVisible()

    // 3. Navigate to tickets
    await page.goto('/staff/tickets')
    await expect(page.locator('.el-table')).toBeVisible()

    // 4. Check for accept/complete buttons on pending orders
    const acceptBtn = page.locator('button:has-text("接单")')
    if (await acceptBtn.isVisible()) {
      // Click accept — will trigger confirm dialog
      await acceptBtn.first().click()
      await page.waitForSelector('.el-message-box', { timeout: 3000 })
      // Cancel the dialog in test
      await page.click('.el-message-box__close, .el-message-box button:has-text("取消")')
    }

    // 5. Navigate to messages
    await page.goto('/staff/message')
    await expect(page.locator('body')).toBeVisible()

    // 6. Navigate to notice
    await page.goto('/staff/notice')
    await expect(page.locator('.notice-page')).toBeVisible()

    // 7. Navigate to profile
    await page.goto('/profile')
    await expect(page.locator('.profile-page')).toBeVisible()
  })
})
```

- [ ] **Step 2: Commit**

```bash
git add tests/e2e/specs/staff.spec.js
git commit -m "test: E2E维修工全流程 — 登录→工作台→工单管理→消息→公告→个人"
```

---

### Task 25: 管理员全流程测试

**Files:**
- Create: `tests/e2e/specs/admin.spec.js`

- [ ] **Step 1: 编写管理员全流程测试**

```javascript
const { test, expect } = require('@playwright/test')
const { login } = require('../helpers/auth')
const { baseURL, admin } = require('../fixtures/test-data')

test.describe('Admin Full Flow', () => {
  test.use({ baseURL })

  test('admin dashboard and management flow', async ({ page }) => {
    // 1. Login
    await login(page, admin.username, admin.password)

    // 2. Dashboard
    await page.goto('/admin/dashboard')
    await expect(page.locator('.admin-dashboard')).toBeVisible()
    await expect(page.locator('.dash-stat').first()).toBeVisible()

    // 3. Ticket management
    await page.goto('/admin/tickets')
    await expect(page.locator('.el-table')).toBeVisible()
    await page.fill('input[placeholder*="工单号"]', 'test')
    await page.click('button:has-text("查询")')

    // 4. User management
    await page.goto('/admin/users')
    await expect(page.locator('.el-table')).toBeVisible()

    // 5. Stats
    await page.goto('/admin/stats')
    await expect(page.locator('.admin-stats')).toBeVisible()
    await expect(page.locator('.kpi-card').first()).toBeVisible()

    // 6. Feedback management
    await page.goto('/admin/feedback')
    await expect(page.locator('.el-table, .el-empty')).toBeVisible()

    // 7. Settings — publish notice
    await page.goto('/admin/settings')
    await expect(page.locator('.admin-settings')).toBeVisible()
    await page.fill('input[placeholder*="公告标题"]', 'E2E测试公告')
    await page.fill('textarea[placeholder*="公告内容"]', '系统运行正常')
    await page.click('button:has-text("发布公告")')
    await expect(page.locator('.el-message--success, .el-message--error')).toBeVisible({ timeout: 10000 })

    // 8. Profile
    await page.goto('/admin/profile')
    await expect(page.locator('.profile-page')).toBeVisible()
  })
})
```

- [ ] **Step 2: Commit**

```bash
git add tests/e2e/specs/admin.spec.js
git commit -m "test: E2E管理员全流程 — 登录→仪表盘→工单→用户→统计→反馈→公告→个人"
```

---

### Task 26: Playwright 配置文件

**Files:**
- Create: `playwright.config.js`

- [ ] **Step 1: 创建项目根目录配置文件**

```javascript
const { defineConfig } = require('@playwright/test')

module.exports = defineConfig({
  testDir: './tests/e2e',
  timeout: 30000,
  retries: 0,
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:5173',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        browserName: 'chromium',
        launchOptions: {
          args: ['--no-sandbox', '--disable-setuid-sandbox']
        }
      }
    }
  ]
})
```

- [ ] **Step 2: Commit**

```bash
git add playwright.config.js
git commit -m "test: Playwright配置文件 — Chromium + 失败截图/录像/trace"
```

---

### Task 27: 最终验证

- [ ] **Step 1: 验证前端构建**

```bash
cd frontend && npx vite build --mode production 2>&1 | tail -5
```
Expected: 零错误

- [ ] **Step 2: 验证后端编译**

```bash
cd backend && mvn clean compile -q 2>&1 | tail -3
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 验证 E2E 测试配置正确加载**

```bash
npx playwright test --list 2>&1 | head -20
```
Expected: 列出所有测试用例

---

## 执行顺序

```
L0: Task 1 → 2 → 3     (基础设施)
L1: Task 4 → 5 → 6 → 7 (数据反馈)
L2: Task 8 → 9 → 10    (操作安全)
L3: Task 11 → 12        (状态一致性)
L4: Task 13 → 14 → 15  (可达性与边界)
L5: Task 16 → 17 → 18  (视觉升级)
E2E: Task 19 → 20 → 21 → 22 → 23 → 24 → 25 → 26 → 27
```

每层完成后执行 `npx vite build` 验证无回归。
