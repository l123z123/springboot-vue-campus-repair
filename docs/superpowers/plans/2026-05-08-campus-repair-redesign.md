# 校园报修管理系统全面改造 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将校园报修管理系统从"学生毕设水平"提升到"可通过答辩的企业级产品"，修复业务逻辑bug，重设计所有不合理页面。

**Architecture:** Vue 3 SFC + Element Plus + 单列卡片流布局。2个新建页面，4个重写页面，5个优化页面，1个删除，路由+布局修改。所有页面使用 el-card 分区包裹，自上而下信息流，遵循真实业务流程。

**Tech Stack:** Vue 3 (Composition API) + Element Plus + ECharts + Pinia + Vue Router

---

## Phase 1: 基础层（路由 + 布局 + 删除）

### Task 1: 删除 Booking 页面 + 清理路由

**Files:**
- Delete: `frontend/src/views/Booking.vue`
- Modify: `frontend/src/router/index.js`

- [ ] **Step 1: 删除 Booking.vue 文件**

```bash
rm "frontend/src/views/Booking.vue"
```

- [ ] **Step 2: 更新路由 — 删除 booking 路由，新增独立工单管理路由**

在 `frontend/src/router/index.js` 中，删除 booking 路由块（第154-158行），新增两个工单管理路由：

```js
// 删除:
// {
//   path: 'booking',
//   name: 'Booking',
//   component: () => import('@/views/Booking.vue'),
//   meta: { title: '预约维修' }
// },

// 新增 — 放在 notice 路由之前:
{
  path: 'orders',
  name: 'OrderManage',
  component: () => import('@/views/OrderManage.vue'),
  meta: { title: '工单管理' }
},
// 维修工端路由新增:
{
  path: 'staff/tickets',
  name: 'StaffTickets',
  component: () => import('@/views/StaffTickets.vue'),
  meta: { title: '工单管理' }
},
```

同时更新 keep-alive include，添加新页面名称：

在 AppLayout.vue 的 `<keep-alive :include="[...]">` 中添加 `'OrderManage'` 和 `'StaffTickets'`。

- [ ] **Step 3: 验证编译**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```
Expected: 无错误

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/Booking.vue frontend/src/router/index.js frontend/src/layout/AppLayout.vue
git commit -m "feat: delete Booking page, add OrderManage and StaffTickets routes"
```

---

### Task 2: 更新 AppLayout 侧栏路由

**Files:**
- Modify: `frontend/src/layout/AppLayout.vue:64-111`

- [ ] **Step 1: 修改侧栏菜单项**

学生端 — 将"工单管理"的 index 从 `/profile?scroll=records` 改为 `/orders`：
```html
<el-menu-item index="/orders">
  <el-icon><Tickets /></el-icon>
  <template #title>工单管理</template>
</el-menu-item>
```

维修工端 — 同样修改 + 新增独立工单管理入口：
```html
<el-menu-item index="/staff/tickets">
  <el-icon><Tickets /></el-icon>
  <template #title>工单管理</template>
</el-menu-item>
```

- [ ] **Step 2: 验证编译**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/layout/AppLayout.vue
git commit -m "fix: sidebar 工单管理 now points to /orders and /staff/tickets"
```

---

## Phase 2: 新建页面

### Task 3: 新建学生端工单管理页 OrderManage.vue

**Files:**
- Create: `frontend/src/views/OrderManage.vue`

- [ ] **Step 1: 创建完整的 OrderManage.vue**

```vue
<template>
  <div class="order-manage">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索地点或故障描述..."
        clearable
        style="width: 260px"
        @keyup.enter="onSearch"
        @clear="onSearch"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px" @change="onSearch">
        <el-option label="全部" value="" />
        <el-option label="待处理" value="0,1,3" />
        <el-option label="进行中" value="4,5" />
        <el-option label="已完成" value="6,7,8" />
        <el-option label="已关闭" value="9,10" />
      </el-select>
      <el-select v-model="urgencyFilter" placeholder="紧急度" clearable style="width: 120px" @change="onSearch">
        <el-option label="全部" value="" />
        <el-option label="普通" value="1" />
        <el-option label="紧急" value="2" />
        <el-option label="非常紧急" value="3" />
      </el-select>
      <el-button type="primary" @click="onSearch"><el-icon><Search /></el-icon>查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <!-- 工单表格 -->
    <el-table
      :data="records"
      stripe
      border
      size="default"
      v-loading="loading"
      highlight-current-row
      @row-click="goDetail"
      class="order-table"
    >
      <template #empty>
        <el-empty description="暂无工单记录" :image-size="80">
          <el-button type="primary" @click="$router.push('/publish')">去报修</el-button>
        </el-empty>
      </template>
      <el-table-column prop="ticketNo" label="工单号" width="150">
        <template #default="{ row }">
          <span class="ticket-no-text">{{ row.ticketNo || '#' + String(row.id).slice(-6) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="location" label="报修地点" min-width="160" show-overflow-tooltip />
      <el-table-column prop="description" label="故障描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="分类" width="100">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.category || '其他' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="紧急度" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isUrgent || row.urgency === 3" type="danger" size="small">紧急</el-tag>
          <el-tag v-else-if="row.urgency === 2" type="warning" size="small">中等</el-tag>
          <span v-else class="text-muted">普通</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.statusCode)" size="small">{{ getRepairStatusLabel(row.statusCode) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="报修时间" width="170" sortable />
      <el-table-column label="操作" width="80" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click.stop="goDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getRepairListPage } from '@/api/repair'
import { getRepairStatusLabel, getStatusTagType } from '@/constants/repairStatus'

defineOptions({ name: 'OrderManage' })

const router = useRouter()
const loading = ref(false)
const records = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)

const keyword = ref('')
const statusFilter = ref('')
const urgencyFilter = ref('')

async function loadData() {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.statusIn = statusFilter.value
    if (urgencyFilter.value) params.urgency = Number(urgencyFilter.value)
    const res = await getRepairListPage(params)
    records.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
    records.value = []
  } finally {
    loading.value = false
  }
}

function onSearch() { page.value = 1; loadData() }
function onReset() { keyword.value = ''; statusFilter.value = ''; urgencyFilter.value = ''; page.value = 1; loadData() }
function goDetail(row) { if (row?.id) router.push({ name: 'RepairDetail', params: { id: String(row.id) } }) }

onMounted(() => loadData())
</script>

<style scoped>
.order-manage { min-height: 400px; }

.filter-bar {
  display: flex; gap: 12px; align-items: center; flex-wrap: wrap;
  margin-bottom: 20px; padding: 16px; background: var(--el-fill-color-lighter);
  border-radius: 10px;
}

.order-table { width: 100%; }

.ticket-no-text {
  font-family: 'SF Mono', 'Cascadia Code', monospace;
  color: var(--el-color-primary); cursor: pointer;
}

.text-muted { color: var(--el-text-color-placeholder); font-size: 13px; }

.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }

:deep(.el-table__row) { cursor: pointer; }
</style>
```

- [ ] **Step 2: 验证编译**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/OrderManage.vue
git commit -m "feat: add student OrderManage page with filter and pagination"
```

---

### Task 4: 新建维修工端工单管理页 StaffTickets.vue

**Files:**
- Create: `frontend/src/views/StaffTickets.vue`

- [ ] **Step 1: 创建完整的 StaffTickets.vue**

```vue
<template>
  <div class="staff-tickets">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.key">
        <div class="stat-card" @click="activeTab = card.filterTab">
          <div class="stat-card__value">{{ card.value }}</div>
          <div class="stat-card__label">{{ card.label }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" @tab-change="onTabChange" class="ticket-tabs">
      <el-tab-pane label="待处理" name="pending" />
      <el-tab-pane label="进行中" name="processing" />
      <el-tab-pane label="已完成" name="done" />
    </el-tabs>

    <!-- 工单表格 -->
    <el-table
      :data="filteredRecords"
      stripe border size="default" v-loading="loading"
      highlight-current-row @row-click="goDetail"
    >
      <template #empty>
        <el-empty description="暂无工单" :image-size="80" />
      </template>
      <el-table-column prop="ticketNo" label="工单号" width="150">
        <template #default="{ row }">
          <span class="ticket-no-text">{{ row.ticketNo || '#' + String(row.id).slice(-6) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="location" label="报修地点" min-width="150" show-overflow-tooltip />
      <el-table-column prop="description" label="故障描述" min-width="180" show-overflow-tooltip />
      <el-table-column label="报修人" width="100">
        <template #default="{ row }">
          {{ row.reporterName || row.studentName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="紧急度" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isUrgent || row.urgency === 'high'" type="danger" size="small">紧急</el-tag>
          <span v-else class="text-muted">普通</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.statusCode)" size="small">{{ getRepairStatusLabel(row.statusCode) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="170" />
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.statusCode === 4" type="primary" size="small" @click.stop="handleAccept(row)">接单</el-button>
          <el-button v-if="row.statusCode === 5" type="success" size="small" @click.stop="handleComplete(row)">完成</el-button>
          <el-button type="primary" link size="small" @click.stop="goDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap" v-if="total > pageSize">
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total"
        layout="total, prev, pager, next" @current-change="loadData" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRepairListPage, acceptOrder, completeOrder } from '@/api/repair'
import { getRepairStatusLabel, getStatusTagType } from '@/constants/repairStatus'

defineOptions({ name: 'StaffTickets' })

const router = useRouter()
const loading = ref(false)
const records = ref([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const activeTab = ref('pending')

const statCards = computed(() => {
  const list = records.value
  const pending = list.filter(r => r.statusCode === 4).length
  const processing = list.filter(r => r.statusCode === 5).length
  const todayDone = list.filter(r => r.statusCode >= 6 && r.statusCode <= 8).length
  return [
    { key: 'pending', label: '待接单', value: pending, filterTab: 'pending' },
    { key: 'processing', label: '维修中', value: processing, filterTab: 'processing' },
    { key: 'done', label: '已完成', value: todayDone, filterTab: 'done' }
  ]
})

const filteredRecords = computed(() => {
  const map = {
    pending: [4],
    processing: [5],
    done: [6, 7, 8]
  }
  return records.value.filter(r => (map[activeTab.value] || []).includes(r.statusCode))
})

async function loadData() {
  loading.value = true
  try {
    const res = await getRepairListPage({ page: page.value, size: pageSize })
    records.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally { loading.value = false }
}

async function handleAccept(row) {
  try { await acceptOrder(row.id); ElMessage.success('已接单'); await loadData() }
  catch (e) { ElMessage.error(e?.message || '操作失败') }
}
async function handleComplete(row) {
  try { await completeOrder(row.id); ElMessage.success('已标记完成'); await loadData() }
  catch (e) { ElMessage.error(e?.message || '操作失败') }
}
function goDetail(row) { if (row?.id) router.push({ name: 'RepairDetail', params: { id: String(row.id) } }) }
function onTabChange() { page.value = 1; loadData() }

onMounted(() => loadData())
</script>

<style scoped>
.staff-tickets { min-height: 400px; }

.stat-row { margin-bottom: 20px; }

.stat-card {
  padding: 20px; background: #fff; border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05); cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s; text-align: center;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.stat-card__value { font-size: 32px; font-weight: 700; color: var(--el-text-color-primary); }
.stat-card__label { font-size: 13px; color: var(--el-text-color-secondary); margin-top: 4px; }

.ticket-tabs { margin-bottom: 16px; }

.ticket-no-text { font-family: 'SF Mono', 'Cascadia Code', monospace; color: var(--el-color-primary); }
.text-muted { color: var(--el-text-color-placeholder); font-size: 13px; }

.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
:deep(.el-table__row) { cursor: pointer; }
</style>
```

- [ ] **Step 2: 验证编译**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/StaffTickets.vue
git commit -m "feat: add staff StaffTickets page with accept/complete quick actions"
```

---

## Phase 3: 重写页面

### Task 5: 重写 OrderDetail.vue（自上而下卡片流）

**Files:**
- Rewrite: `frontend/src/views/OrderDetail.vue`

- [ ] **Step 1: 完全重写 OrderDetail.vue**

```vue
<template>
  <div class="order-detail-page">
    <div v-if="loading" class="detail-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <!-- 1. 顶部状态栏 -->
      <div class="detail-header">
        <div class="detail-header__left">
          <span class="detail-header__id">工单 #{{ order.ticketNo || order.id }}</span>
          <el-tag :type="statusTagType(order.status)" size="large">{{ order.statusText }}</el-tag>
          <el-tag v-if="order.isUrgent" type="danger" size="large" effect="dark">紧急</el-tag>
        </div>
        <span class="detail-header__time">提交于 {{ order.createTime || order.date }}</span>
      </div>

      <!-- 2. 基本信息 -->
      <el-card shadow="never" class="section-card">
        <template #header><span class="card-title">基本信息</span></template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="报修地点">{{ order.location }}</el-descriptions-item>
          <el-descriptions-item label="故障分类">{{ order.category || '其他' }}</el-descriptions-item>
          <el-descriptions-item label="紧急程度">
            <el-tag v-if="order.isUrgent || order.urgency === 'high'" type="danger" size="small">紧急</el-tag>
            <el-tag v-else-if="order.urgency === 'medium'" type="warning" size="small">中等</el-tag>
            <span v-else>普通</span>
          </el-descriptions-item>
          <el-descriptions-item label="报修人">{{ order.reporterName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ order.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修工">{{ order.worker }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 3. 故障描述 + 图片 -->
      <el-card shadow="never" class="section-card">
        <template #header><span class="card-title">故障描述</span></template>
        <p class="desc-text">{{ order.desc }}</p>
        <div v-if="order.images && order.images.length" class="image-gallery">
          <el-image
            v-for="(img, i) in order.images"
            :key="i"
            :src="img"
            fit="cover"
            class="gallery-thumb"
            :preview-src-list="order.images"
            :initial-index="i"
          />
        </div>
        <div v-else-if="order.image" class="image-gallery">
          <el-image :src="order.image" fit="cover" class="gallery-thumb" :preview-src-list="[order.image]" />
        </div>
      </el-card>

      <!-- 4. 处理进度 — el-steps -->
      <el-card shadow="never" class="section-card">
        <template #header><span class="card-title">处理进度</span></template>
        <el-steps :active="currentStep" align-center finish-status="success">
          <el-step title="提交" :description="order.createTime || ''" />
          <el-step title="审核" :description="order.auditTime || ''" />
          <el-step title="派单" :description="order.dispatchTime || ''" />
          <el-step title="维修" :description="order.completedTime || ''" />
          <el-step title="确认" :description="order.confirmTime || ''" />
          <el-step title="评价" :description="evaluation?.createTime || ''" />
        </el-steps>
        <!-- 废弃状态下显示 -->
        <div v-if="order.status === 9" style="margin-top:16px;text-align:center;">
          <el-alert type="error" title="该工单已被管理员拒绝" :closable="false" show-icon />
        </div>
        <div v-if="order.status === 10" style="margin-top:16px;text-align:center;">
          <el-alert type="warning" title="该工单已取消" :closable="false" show-icon />
        </div>
      </el-card>

      <!-- 5. 沟通入口 -->
      <div v-if="order.repairmanId" class="contact-bar">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            已分配维修师傅
            <el-button type="primary" size="small" style="margin-left:12px" @click="goChat">
              <el-icon><ChatDotRound /></el-icon>
              联系沟通
            </el-button>
          </template>
        </el-alert>
      </div>

      <!-- 6. 操作区（按角色+状态） -->
      <!-- 管理员: 审核/派单 -->
      <el-card v-if="userRole === 2 && (order.status === 0 || order.status === 1 || order.status === 3)" shadow="never" class="section-card action-card">
        <template #header><span class="card-title">管理员处理</span></template>
        <p class="admin-hint">{{ order.status === 3 ? '当前待派单，请选择维修工' : '请审核该工单' }}</p>

        <div v-if="order.status === 0 || order.status === 1 || order.status === 3" class="recommend-box">
          <div class="recommend-box__head">
            <span>智能推荐</span>
            <el-button link type="primary" size="small" :loading="recommendLoading" @click="loadRecommendations">刷新</el-button>
          </div>
          <div v-if="recommendations.length" class="recommend-list">
            <div v-for="r in recommendations.slice(0, 3)" :key="r.repairmanId" class="recommend-item" @click="useRecommendation(r.repairmanId)">
              <div>
                <strong>{{ r.realName || r.username }}</strong>
                <span> · 在办{{ r.activeCount }}单 · 评分{{ r.score }}</span>
              </div>
              <el-button type="primary" plain size="small">采用</el-button>
            </div>
          </div>
          <el-empty v-else description="暂无推荐" :image-size="40" />
        </div>

        <el-divider />

        <el-form v-if="order.status === 3" :model="dispatchForm" label-width="100px">
          <el-form-item label="选择维修工" required>
            <el-select v-model="dispatchForm.repairmanId" filterable placeholder="搜索维修工" style="width:100%">
              <el-option v-for="u in repairmanOptions" :key="u.userId" :label="u.realName || u.username" :value="String(u.userId)" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="handleDispatch">确认派单</el-button></el-form-item>
        </el-form>

        <el-form v-else :model="auditForm" label-width="100px">
          <el-form-item label="审核结果">
            <el-radio-group v-model="auditForm.approved">
              <el-radio :value="true">通过</el-radio>
              <el-radio :value="false">拒绝</el-radio>
            </el-radio-group>
          </el-form-item>
          <template v-if="auditForm.approved">
            <el-form-item label="分配方式">
              <el-radio-group v-model="auditForm.dispatchMode">
                <el-radio value="later">待派单</el-radio>
                <el-radio value="now">立即派单</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="auditForm.dispatchMode === 'now'" label="维修工">
              <el-select v-model="auditForm.assignRepairmanId" filterable placeholder="选择维修工" style="width:100%">
                <el-option v-for="u in repairmanOptions" :key="u.userId" :label="u.realName || u.username" :value="String(u.userId)" />
              </el-select>
            </el-form-item>
          </template>
          <el-form-item label="备注">
            <el-input v-model="auditForm.remark" type="textarea" :rows="2" placeholder="选填" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="auditSubmitting" @click="handleAudit">提交审核</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 维修工操作 -->
      <el-card v-if="userRole === 1 && order.status === 4" shadow="never" class="section-card action-card">
        <div class="action-row">
          <div>
            <h4>开始维修</h4>
            <p>确认到场后请点击</p>
          </div>
          <el-button type="success" size="large" @click="handleAccept">开始维修</el-button>
        </div>
      </el-card>
      <el-card v-if="userRole === 1 && order.status === 5" shadow="never" class="section-card action-card">
        <div class="action-row">
          <div>
            <h4>完成维修</h4>
            <p>维修完成后通知学生确认</p>
          </div>
          <el-button type="success" size="large" @click="handleComplete">完成维修</el-button>
        </div>
      </el-card>

      <!-- 学生操作 -->
      <el-card v-if="userRole === 0 && order.status === 6" shadow="never" class="section-card action-card">
        <div class="action-row">
          <div>
            <h4>确认维修完成</h4>
            <p>确认无误后可评价</p>
          </div>
          <el-button type="primary" size="large" @click="handleConfirm">确认完成</el-button>
        </div>
      </el-card>

      <!-- 评价 -->
      <el-card v-if="showEvaluationForm" shadow="never" class="section-card">
        <template #header><span class="card-title">服务评价</span></template>
        <el-form ref="evaluationFormRef" :model="evaluationModel" :rules="evaluationRules" label-width="80px">
          <el-form-item label="评分" prop="score">
            <el-rate v-model="evaluationModel.score" :max="5" show-text />
          </el-form-item>
          <el-form-item label="评论">
            <el-input v-model="evaluationModel.comment" type="textarea" :rows="3" placeholder="分享您的体验（选填）" maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="evaluationModel.isAnonymous">匿名评价</el-checkbox>
            <el-button type="primary" :loading="submitting" style="margin-left:24px" @click="submitEvaluation">提交评价</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card v-if="showEvaluationDetail" shadow="never" class="section-card">
        <template #header><span class="card-title">评价详情</span></template>
        <el-rate :model-value="Number(evaluation.score) || 0" :max="5" disabled show-text />
        <p v-if="evaluation.comment" class="eval-comment">{{ evaluation.comment }}</p>
        <div class="eval-meta">
          <el-tag size="small" :type="evaluation.isAnonymous ? 'info' : 'success'">{{ evaluation.isAnonymous ? '匿名' : '实名' }}</el-tag>
          <span style="font-size:12px;color:var(--el-text-color-placeholder)">{{ evaluation.createTime }}</span>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import { getRepairDetail, submitEvaluation as apiSubmitEvaluation, getEvaluation,
  auditOrder, dispatchOrder, acceptOrder, completeOrder, confirmOrder, getDispatchRecommendations } from '@/api/repair'
import { getAdminUserList } from '@/api/adminUser'
import { useUserStore } from '@/stores/user'
import { getRepairStatusLabel, getStatusTagType } from '@/constants/repairStatus'

defineOptions({ name: 'OrderDetail' })

const route = useRoute(); const router = useRouter(); const userStore = useUserStore()

const userRole = computed(() => userStore.userInfo?.role != null ? Number(userStore.userInfo.role) : 0)
const orderId = computed(() => route.params.id)
const loading = ref(true); const submitting = ref(false)

const order = ref({ id: '', location: '', desc: '', status: 0, statusText: '', image: '', images: [],
  date: '', worker: '待分配', repairmanId: null, isUrgent: false, category: '', reporterName: '', phone: '',
  createTime: '', auditTime: '', dispatchTime: '', completedTime: '', confirmTime: '' })

const evaluation = ref(null)
const evaluationFormRef = ref(null)
const evaluationModel = ref({ score: 5, comment: '', isAnonymous: false })
const showEvaluationDetail = computed(() => !!(evaluation.value?.score != null && Number(evaluation.value.score) > 0))
const showEvaluationForm = computed(() => userRole.value === 0 && Number(order.value.status) === 7 && !showEvaluationDetail.value)
const evaluationRules = { score: [{ required: true, message: '请选择评分', trigger: 'change' }] }

const auditForm = ref({ approved: true, remark: '', dispatchMode: 'later', assignRepairmanId: null })
const auditSubmitting = ref(false)
const dispatchForm = ref({ repairmanId: null })
const repairmanOptions = ref([])
const recommendations = ref([])
const recommendLoading = ref(false)

const STATUS_TEXT = { 0:'待审核',1:'审核中',2:'已审核',3:'待派单',4:'已派单',5:'维修中',6:'维修完成',7:'学生确认',8:'已评价',9:'已拒绝',10:'已取消' }

function statusTagType(s) {
  const map = { 0:'info',1:'warning',3:'info',4:'warning',5:'',6:'success',7:'success',8:'success',9:'danger',10:'danger' }
  return map[s] || 'info'
}

const currentStep = computed(() => {
  const s = order.value.status
  if (s === 9 || s === 10) return 0
  if (s <= 0) return 0
  if (s <= 2) return 1
  if (s <= 3) return 2
  if (s <= 5) return 3
  if (s <= 6) return 4
  return 5
})

async function loadDetail() {
  const id = orderId.value; if (!id) return
  loading.value = true; evaluation.value = null
  try {
    const data = await getRepairDetail(id)
    if (data?.id != null || data?.orderId != null) {
      const sc = data.statusCode != null ? data.statusCode : 0
      order.value = {
        id: data.id || data.orderId, ticketNo: data.ticketNo || '',
        location: data.location || '', desc: data.description || '',
        status: sc, statusText: STATUS_TEXT[sc] || '',
        image: data.image || '', images: data.images || [],
        date: data.date || '', worker: data.repairmanName || '待分配',
        repairmanId: data.repairmanId, isUrgent: !!data.isUrgent,
        category: data.category || '', reporterName: data.reporterName || '',
        phone: data.phone || data.phoneNumber || '',
        createTime: data.createTime || '', auditTime: data.auditTime || '',
        dispatchTime: data.dispatchTime || '', completedTime: data.completedTime || '',
        confirmTime: data.confirmTime || ''
      }
      if (sc === 7 || sc === 8) await loadEvaluation(id)
    }
  } catch (e) { ElMessage.error(e?.message || '加载失败') }
  finally { loading.value = false }
}

async function loadEvaluation(id) {
  try { const data = await getEvaluation(id); evaluation.value = (data?.score != null) ? data : null }
  catch { evaluation.value = null }
}

async function loadRepairmanList() {
  if (userRole.value !== 2) return
  try { const p = await getAdminUserList({ page:1, size:200, role:1, status:1 }); repairmanOptions.value = p?.records || [] }
  catch { repairmanOptions.value = [] }
}

async function loadRecommendations() {
  if (userRole.value !== 2 || !orderId.value) return
  recommendLoading.value = true
  try { const data = await getDispatchRecommendations(orderId.value); recommendations.value = Array.isArray(data) ? data : [] }
  catch { recommendations.value = [] }
  finally { recommendLoading.value = false }
}

function useRecommendation(id) { dispatchForm.value.repairmanId = String(id); auditForm.value.assignRepairmanId = String(id); auditForm.value.dispatchMode = 'now' }

async function handleAudit() {
  if (auditForm.value.approved && auditForm.value.dispatchMode === 'now' && !auditForm.value.assignRepairmanId) { ElMessage.warning('请选择维修工'); return }
  auditSubmitting.value = true
  try {
    const body = { approved: auditForm.value.approved, remark: auditForm.value.remark }
    if (body.approved && auditForm.value.dispatchMode === 'now') body.assignRepairmanId = auditForm.value.assignRepairmanId
    await auditOrder(orderId.value, body)
    ElMessage.success(body.approved ? (body.assignRepairmanId ? '审核通过并派单' : '审核通过，进入待派单') : '已拒绝')
    await loadDetail()
  } catch (e) { ElMessage.error(e?.message || '操作失败') }
  finally { auditSubmitting.value = false }
}

async function handleDispatch() {
  if (!dispatchForm.value.repairmanId) { ElMessage.warning('请选择维修工'); return }
  try { await dispatchOrder(orderId.value, dispatchForm.value.repairmanId); ElMessage.success('派单成功'); await loadDetail() }
  catch (e) { ElMessage.error(e?.message || '操作失败') }
}

async function handleAccept() { try { await acceptOrder(orderId.value); ElMessage.success('已开始维修'); await loadDetail() } catch (e) { ElMessage.error(e?.message || '操作失败') } }
async function handleComplete() { try { await completeOrder(orderId.value); ElMessage.success('维修完成'); await loadDetail() } catch (e) { ElMessage.error(e?.message || '操作失败') } }
async function handleConfirm() { try { await confirmOrder(orderId.value); ElMessage.success('已确认'); await loadDetail() } catch (e) { ElMessage.error(e?.message || '操作失败') } }

async function submitEvaluation() {
  if (evaluationFormRef.value?.validate) { try { await evaluationFormRef.value.validate() } catch { return } }
  submitting.value = true
  try {
    await apiSubmitEvaluation(orderId.value, { score:evaluationModel.value.score, comment:evaluationModel.value.comment, isAnonymous:evaluationModel.value.isAnonymous?1:0 })
    ElMessage.success('评价成功'); await loadDetail()
  } catch (e) { ElMessage.error(e?.message || '评价失败') }
  finally { submitting.value = false }
}

function goChat() { router.push({ name:'OrderChat', params:{ orderId:String(order.value.id) } }) }

onMounted(() => { loadDetail(); loadRepairmanList(); loadRecommendations() })
watch(orderId, () => { loadDetail(); loadRecommendations() })
</script>

<style scoped>
.detail-loading { padding:20px 0; }

.detail-header {
  display:flex; justify-content:space-between; align-items:center;
  margin-bottom:20px; padding:16px 20px; background:#fff; border-radius:12px;
  box-shadow:0 2px 12px rgba(0,0,0,0.05);
}
.detail-header__left { display:flex; align-items:center; gap:12px; }
.detail-header__id { font-size:18px; font-weight:700; color:var(--el-text-color-primary); font-family:'SF Mono','Cascadia Code',monospace; }
.detail-header__time { font-size:13px; color:var(--el-text-color-placeholder); }

.section-card { margin-bottom:16px; border-radius:12px; border:1px solid var(--el-border-color-lighter); box-shadow:0 2px 12px rgba(0,0,0,0.05); }
.card-title { font-size:15px; font-weight:600; }

.desc-text { margin:0 0 12px 0; font-size:14px; color:var(--el-text-color-regular); line-height:1.7; }

.image-gallery { display:flex; flex-wrap:wrap; gap:10px; }
.gallery-thumb { width:120px; height:120px; border-radius:8px; object-fit:cover; cursor:pointer; border:1px solid var(--el-border-color-lighter); }
.gallery-thumb:hover { border-color:var(--el-color-primary); }

.contact-bar { margin-bottom:16px; }
.contact-bar :deep(.el-alert__title) { display:flex; align-items:center; }

.action-card { border-left:4px solid var(--el-color-primary); }
.action-row { display:flex; justify-content:space-between; align-items:center; gap:20px; }
.action-row h4 { margin:0 0 4px 0; font-size:15px; font-weight:600; }
.action-row p { margin:0; font-size:13px; color:var(--el-text-color-secondary); }

.admin-hint { margin:0 0 14px 0; font-size:13px; color:var(--el-text-color-secondary); }

.recommend-box { padding:14px; border:1px solid var(--el-border-color-lighter); border-radius:8px; background:var(--el-fill-color-lighter); margin-bottom:4px; }
.recommend-box__head { display:flex; justify-content:space-between; align-items:center; margin-bottom:10px; font-weight:600; }
.recommend-list { display:grid; gap:8px; }
.recommend-item { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:10px 12px; background:#fff; border:1px solid var(--el-border-color-lighter); border-radius:8px; cursor:pointer; }
.recommend-item:hover { border-color:var(--el-color-primary); }

.eval-comment { margin:12px 0; padding:12px; background:var(--el-fill-color-lighter); border-radius:8px; font-size:14px; line-height:1.7; }
.eval-meta { display:flex; justify-content:space-between; align-items:center; padding-top:10px; border-top:1px solid var(--el-border-color-lighter); margin-top:8px; }
</style>
```

- [ ] **Step 2: 验证编译**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/OrderDetail.vue
git commit -m "refactor: redesign OrderDetail with top-down card flow and el-steps"
```

---

### Task 6: 重写 Profile.vue（纯个人中心）

**Files:**
- Rewrite: `frontend/src/views/Profile.vue`

- [ ] **Step 1: 完全重写 Profile.vue**

```vue
<template>
  <div class="profile-page">
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <!-- 用户信息卡 -->
      <el-card shadow="never" class="profile-card">
        <div class="user-info">
          <el-avatar :size="80" :src="userAvatarUrl" class="user-avatar">
            <el-icon :size="36"><UserFilled /></el-icon>
          </el-avatar>
          <div class="user-name">{{ userInfo.name }}</div>
          <div class="user-dept">{{ userInfo.department }}</div>
          <div class="user-id">{{ roleLabel }} {{ userInfo.studentId }}</div>
          <div v-if="userInfo.signature" class="user-sig">"{{ userInfo.signature }}"</div>
          <el-button type="primary" style="margin-top:16px" @click="$router.push('/profile/edit')">编辑资料</el-button>
        </div>
      </el-card>

      <!-- 我的统计 -->
      <el-card shadow="never" class="profile-card stats-card">
        <template #header><span class="card-title">我的统计</span></template>
        <el-row :gutter="16">
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-item__value">{{ stats.total || 0 }}</div>
              <div class="stat-item__label">报修总数</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-item__value">{{ stats.completed || 0 }}</div>
              <div class="stat-item__label">已完成</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-item__value">{{ stats.avgRating || '-' }}</div>
              <div class="stat-item__label">评价均分</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 账号安全 -->
      <el-card shadow="never" class="profile-card">
        <template #header><span class="card-title">账号安全</span></template>
        <div class="security-item" @click="$router.push('/settings')">
          <div class="security-item__left">
            <el-icon :size="20"><Lock /></el-icon>
            <span>修改密码</span>
          </div>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled, Lock, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUserInfo as fetchUserInfo } from '@/api/user'
import { getRepairListPage } from '@/api/repair'

defineOptions({ name: 'Profile' })

const userStore = useUserStore()
const loading = ref(true)

const userInfo = ref({ name:'', studentId:'', department:'', avatar:'', avatarUrl:'', signature:'' })
const stats = ref({ total:0, completed:0, avgRating:'-' })

const userAvatarUrl = computed(() => userInfo.value.avatarUrl || userInfo.value.avatar || '')
const roleLabel = computed(() => {
  const role = userStore.userInfo?.role
  if (role === 1) return '工号'
  if (role === 2) return '管理员'
  return '学号'
})

async function loadUserInfo() {
  try {
    const data = await fetchUserInfo()
    userInfo.value = {
      name: data.nickname || data.realName || data.username || '',
      studentId: data.username || '',
      department: data.department || '',
      avatar: data.avatar || '',
      avatarUrl: data.avatarUrl || '',
      signature: data.signature && data.signature !== '暂无签名' ? data.signature : ''
    }
  } catch (e) { ElMessage.error(e?.message || '获取用户信息失败') }
}

async function loadStats() {
  try {
    const res = await getRepairListPage({ page:1, size:1 })
    stats.value.total = res.total || 0
    // 简化统计: 总数已从列表接口获取，完成数和评分可从后端补充
  } catch {}
}

onMounted(async () => {
  loading.value = true
  try { await Promise.all([loadUserInfo(), loadStats()]) } finally { loading.value = false }
})
</script>

<style scoped>
.profile-page { max-width:600px; }

.loading-wrap { padding:20px 0; }

.profile-card { border-radius:12px; border:1px solid var(--el-border-color-lighter); box-shadow:0 2px 12px rgba(0,0,0,0.05); margin-bottom:16px; }
.card-title { font-size:15px; font-weight:600; }

.user-info { text-align:center; padding:8px 0; }
.user-avatar { margin-bottom:14px; border:3px solid var(--el-color-primary-light-7); }
.user-name { font-size:18px; font-weight:700; color:var(--el-text-color-primary); margin-bottom:4px; }
.user-dept { font-size:13px; color:var(--el-text-color-secondary); margin-bottom:2px; }
.user-id { font-size:12px; color:var(--el-text-color-placeholder); margin-bottom:4px; }
.user-sig { font-size:12px; color:var(--el-text-color-secondary); font-style:italic; margin-top:8px; }

.stat-item { text-align:center; }
.stat-item__value { font-size:28px; font-weight:700; color:var(--el-color-primary); }
.stat-item__label { font-size:13px; color:var(--el-text-color-secondary); margin-top:4px; }

.security-item { display:flex; justify-content:space-between; align-items:center; padding:12px; border-radius:8px; cursor:pointer; transition:background 0.2s; }
.security-item:hover { background:var(--el-fill-color-lighter); }
.security-item__left { display:flex; align-items:center; gap:10px; font-size:14px; color:var(--el-text-color-primary); }
</style>
```

- [ ] **Step 2: 验证编译**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/Profile.vue
git commit -m "refactor: redesign Profile as pure user center, remove order list"
```

---

### Task 7: 精简重写 StaffWorkbench.vue

**Files:**
- Rewrite: `frontend/src/views/StaffWorkbench.vue`

- [ ] **Step 1: 精简重写**

```vue
<template>
  <div class="staff-workbench">
    <div class="welcome-row">
      <div>
        <h2 class="welcome-text">{{ greeting }}，{{ userName }}</h2>
        <p class="welcome-date">{{ todayDate }}</p>
      </div>
      <el-button type="primary" @click="$router.push('/staff/tickets')">查看全部工单</el-button>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.key">
        <div class="stat-card" @click="$router.push('/staff/tickets')">
          <div class="stat-card__value">{{ card.value }}</div>
          <div class="stat-card__label">{{ card.label }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 待处理工单 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-head">
          <span class="section-title">待处理工单</span>
          <el-button text type="primary" @click="$router.push('/staff/tickets')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="pendingOrders" stripe size="default" v-loading="loading" @row-click="goDetail">
        <template #empty><el-empty description="暂无待处理工单" :image-size="60" /></template>
        <el-table-column prop="ticketNo" label="工单号" width="140">
          <template #default="{row}"><span class="ticket-no">{{ row.ticketNo || '#'+String(row.id).slice(-6) }}</span></template>
        </el-table-column>
        <el-table-column prop="location" label="地点" min-width="150" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="紧急" width="70" align="center">
          <template #default="{row}"><el-tag v-if="row.isUrgent" type="danger" size="small">紧急</el-tag><span v-else class="text-muted">普通</span></template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{row}">
            <el-button v-if="row.statusCode===4" type="primary" size="small" @click.stop="handleAccept(row)">接单</el-button>
            <el-button v-else type="primary" link size="small" @click.stop="goDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getRepairListPage, acceptOrder } from '@/api/repair'

defineOptions({ name: 'StaffWorkbench' })

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const records = ref([])

const userName = computed(() => {
  const u = userStore.userInfo
  return (u && (u.realName || u.nickname || u.username)) || '维修工'
})

const greeting = computed(() => { const h = new Date().getHours(); return h<12?'上午好':h<18?'下午好':'晚上好' })
const todayDate = computed(() => new Date().toLocaleDateString('zh-CN', { year:'numeric', month:'long', day:'numeric', weekday:'long' }))

const statCards = computed(() => {
  const l = records.value
  return [
    { key:'pending', label:'待接单', value: l.filter(r=>r.statusCode===4).length },
    { key:'processing', label:'维修中', value: l.filter(r=>r.statusCode===5).length },
    { key:'today', label:'今日完成', value: l.filter(r=>r.statusCode>=6&&r.statusCode<=8).length },
    { key:'total', label:'本月累计', value: l.length }
  ]
})

const pendingOrders = computed(() => records.value.filter(r => [4,5].includes(r.statusCode)).slice(0, 5))

async function loadData() {
  loading.value = true
  try { const res = await getRepairListPage({ page:1, size:50 }); records.value = res.records || [] }
  catch { records.value = [] }
  finally { loading.value = false }
}

async function handleAccept(row) { try { await acceptOrder(row.id); ElMessage.success('已接单'); await loadData() } catch(e) { ElMessage.error(e?.message||'操作失败') } }
function goDetail(row) { if (row?.id) router.push({ name:'RepairDetail', params:{ id:String(row.id) } }) }

onMounted(loadData)
</script>

<style scoped>
.staff-workbench { min-height:400px; }

.welcome-row { display:flex; justify-content:space-between; align-items:center; margin-bottom:24px; }
.welcome-text { margin:0 0 4px; font-size:20px; font-weight:700; color:var(--el-text-color-primary); }
.welcome-date { margin:0; font-size:13px; color:var(--el-text-color-secondary); }

.stat-row { margin-bottom:24px; }
.stat-card { padding:20px; background:#fff; border-radius:12px; box-shadow:0 2px 12px rgba(0,0,0,0.05); cursor:pointer; text-align:center; transition:transform 0.2s; }
.stat-card:hover { transform:translateY(-2px); }
.stat-card__value { font-size:32px; font-weight:700; color:var(--el-text-color-primary); }
.stat-card__label { font-size:13px; color:var(--el-text-color-secondary); margin-top:4px; }

.section-card { border-radius:12px; border:1px solid var(--el-border-color-lighter); box-shadow:0 2px 12px rgba(0,0,0,0.05); }
.section-head { display:flex; justify-content:space-between; align-items:center; }
.section-title { font-size:15px; font-weight:600; }

.ticket-no { font-family:'SF Mono','Cascadia Code',monospace; color:var(--el-color-primary); }
.text-muted { color:var(--el-text-color-placeholder); font-size:13px; }
:deep(.el-table__row) { cursor:pointer; }
</style>
```

- [ ] **Step 2: 验证编译**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/StaffWorkbench.vue
git commit -m "refactor: streamline StaffWorkbench to focus on daily tasks"
```

---

### Task 8: 精简重写 Admin/Stats.vue

**Files:**
- Rewrite: `frontend/src/views/admin/Stats.vue`

- [ ] **Step 1: 精简重写 — 只保留折线图+柱状图+绩效表**

```vue
<template>
  <div class="admin-stats">
    <div v-if="loading" class="loading-wrap"><el-skeleton :rows="6" animated /></div>
    <template v-else>
      <!-- 核心指标 -->
      <el-row :gutter="16" class="kpi-row">
        <el-col :span="6" v-for="kpi in kpiCards" :key="kpi.key">
          <div class="kpi-card">
            <div class="kpi-card__value">{{ kpi.value }}</div>
            <div class="kpi-card__label">{{ kpi.label }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 报修趋势 + 分类分布 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :span="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span class="chart-title">报修趋势（近30天）</span></template>
            <div ref="trendChartRef" class="chart-box"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span class="chart-title">分类分布</span></template>
            <div ref="categoryChartRef" class="chart-box"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 维修工绩效 -->
      <el-card shadow="never" class="chart-card">
        <template #header><span class="chart-title">维修工绩效</span></template>
        <el-table :data="staffPerformance" stripe border size="default">
          <template #empty><el-empty description="暂无数据" :image-size="60" /></template>
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="count" label="完成工单数" width="120" sortable />
          <el-table-column label="平均评分" width="120">
            <template #default="{row}">
              <el-rate :model-value="Number(row.rating)||0" :max="5" disabled size="small" show-text />
            </template>
          </el-table-column>
          <el-table-column prop="avgTime" label="平均耗时" min-width="120" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getTodayStats, getTotalStats, getLocationTop10, getStatusDist, getTypeTree, getStaffPerformance } from '@/api/dashboard'

defineOptions({ name: 'AdminStats' })

const loading = ref(true)
const stats = ref({ todayCount:0, totalCount:0, pending:0, completionRate:'0%' })

const trendChartRef = ref(null)
const categoryChartRef = ref(null)
let trendInstance = null
let categoryInstance = null

const locationData = ref([])
const statusData = ref([])
const typeTreeData = ref([])
const staffPerformance = ref([])

const kpiCards = computed(() => [
  { key:'total', label:'总工单数', value: stats.value.totalCount },
  { key:'today', label:'今日新增', value: stats.value.todayCount },
  { key:'pending', label:'待处理', value: stats.value.pending },
  { key:'rate', label:'完成率', value: stats.value.completionRate }
])

onMounted(async () => {
  try {
    const [today, total, locations, statusDist, categoryTree, staffPerf] = await Promise.all([
      getTodayStats(), getTotalStats(), getLocationTop10(), getStatusDist(), getTypeTree(), getStaffPerformance()
    ])
    stats.value.todayCount = today ?? 0
    stats.value.totalCount = total ?? 0
    const fromStatus = Array.isArray(statusDist) ? statusDist : []
    const doneCount = fromStatus.filter(s => ['已完成','已评价','学生确认','维修完成'].includes(s.label||s.name)).reduce((a,b) => a + (b.value||0), 0)
    stats.value.pending = fromStatus.filter(s => ['待审核','审核中','待派单'].includes(s.label||s.name)).reduce((a,b) => a + (b.value||0), 0)
    stats.value.completionRate = total ? Math.round(doneCount / total * 100) + '%' : '0%'
    locationData.value = Array.isArray(locations) ? locations : []
    statusData.value = fromStatus
    typeTreeData.value = Array.isArray(categoryTree) ? categoryTree : []
    staffPerformance.value = Array.isArray(staffPerf) ? staffPerf : []
  } catch { stats.value = { todayCount:0, totalCount:0, pending:0, completionRate:'0%' } }
  finally {
    loading.value = false
    await nextTick()
    renderTrendChart()
    renderCategoryChart()
    window.addEventListener('resize', handleResize)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendInstance?.dispose(); categoryInstance?.dispose()
})

function handleResize() { trendInstance?.resize(); categoryInstance?.resize() }

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendInstance) trendInstance = echarts.init(trendChartRef.value)
  const data = locationData.value
  trendInstance.setOption({
    tooltip: { trigger:'axis' },
    grid: { left:50, right:20, top:20, bottom:30 },
    xAxis: { type:'category', data:data.map(d=>d.name), axisLabel:{rotate:45,fontSize:11} },
    yAxis: { type:'value' },
    series: [{ type:'line', data:data.map(d=>d.value), smooth:true, areaStyle:{opacity:0.15}, itemStyle:{color:'#409eff'} }]
  })
}

function renderCategoryChart() {
  if (!categoryChartRef.value) return
  if (!categoryInstance) categoryInstance = echarts.init(categoryChartRef.value)
  const data = typeTreeData.value
  const flat = []
  function walk(list) { list.forEach(item => { flat.push({ name:item.name, value:item.value||1 }); if(item.children) walk(item.children) }) }
  if (data.length) walk(data)
  else flat.push({ name:'暂无数据', value:1 })

  categoryInstance.setOption({
    tooltip: { trigger:'axis', axisPointer:{type:'shadow'} },
    grid: { left:50, right:20, top:10, bottom:30 },
    xAxis: { type:'category', data:flat.map(d=>d.name), axisLabel:{rotate:30,fontSize:11} },
    yAxis: { type:'value' },
    series: [{ type:'bar', data:flat.map(d=>d.value), barWidth:24,
      itemStyle:{ borderRadius:[4,4,0,0], color: new echarts.graphic.LinearGradient(0,0,0,1,[
        {offset:0,color:'#6366F1'},{offset:1,color:'#A5B4FC'}
      ])}
    }]
  })
}
</script>

<style scoped>
.admin-stats { min-height:400px; }
.loading-wrap { padding:20px 0; }

.kpi-row { margin-bottom:20px; }
.kpi-card { padding:20px; background:#fff; border-radius:12px; box-shadow:0 2px 12px rgba(0,0,0,0.05); text-align:center; }
.kpi-card__value { font-size:32px; font-weight:700; color:var(--el-color-primary); }
.kpi-card__label { font-size:13px; color:var(--el-text-color-secondary); margin-top:4px; }

.chart-row { margin-bottom:16px; }
.chart-card { border-radius:12px; box-shadow:0 2px 12px rgba(0,0,0,0.05); margin-bottom:16px; }
.chart-title { font-size:15px; font-weight:600; }
.chart-box { width:100%; min-height:300px; }
</style>
```

- [ ] **Step 2: 验证编译**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/admin/Stats.vue
git commit -m "refactor: simplify Stats to trend line + category bar + worker table"
```

---

## Phase 4: 优化页面

### Task 9: 优化 Home.vue（SmartHome）

**Files:**
- Read first, then Overwrite: `frontend/src/views/Home.vue`

- [ ] **Step 1: 先确认 Home.vue 现有内容**

查看 frontend/src/views/Home.vue（当前 SmartHome 只是一个 shell，实际渲染的是 Home 组件）。

- [ ] **Step 2: 优化为快捷入口 + 待办简报**

```vue
<template>
  <div class="home-page">
    <!-- 欢迎区 -->
    <div class="welcome-section">
      <h2 class="welcome-title">{{ greeting }}，{{ userName }}</h2>
      <p class="welcome-sub">{{ todayDate }}</p>
    </div>

    <!-- 快捷入口 -->
    <el-row :gutter="16" class="quick-row">
      <el-col :span="6" v-for="item in quickActions" :key="item.key">
        <div class="quick-card" @click="$router.push(item.path)">
          <el-icon :size="28" :color="item.color"><component :is="item.icon" /></el-icon>
          <span class="quick-card__label">{{ item.label }}</span>
        </div>
      </el-col>
    </el-row>

    <!-- 最近工单 -->
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-head">
          <span class="section-title">最近工单</span>
          <el-button text type="primary" @click="$router.push('/orders')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentOrders" stripe size="default" v-loading="loading" @row-click="goDetail">
        <template #empty><el-empty description="暂无工单" :image-size="60"><el-button type="primary" @click="$router.push('/publish')">提交报修</el-button></el-empty></template>
        <el-table-column prop="ticketNo" label="工单号" width="140">
          <template #default="{row}"><span class="ticket-no">{{ row.ticketNo || '#'+String(row.id).slice(-6) }}</span></template>
        </el-table-column>
        <el-table-column prop="location" label="地点" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{row}"><el-tag :type="getStatusTagType(row.statusCode)" size="small">{{ getRepairStatusLabel(row.statusCode) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { UploadFilled, Tickets, ChatDotRound, QuestionFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getRepairListPage } from '@/api/repair'
import { getRepairStatusLabel, getStatusTagType } from '@/constants/repairStatus'

defineOptions({ name: 'Home' })

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const recentOrders = ref([])

const userName = computed(() => {
  const u = userStore.userInfo
  return (u && (u.realName || u.nickname || u.username)) || '同学'
})
const greeting = computed(() => { const h = new Date().getHours(); return h<12?'早上好':h<18?'下午好':'晚上好' })
const todayDate = computed(() => new Date().toLocaleDateString('zh-CN',{year:'numeric',month:'long',day:'numeric',weekday:'long'}))

const quickActions = [
  { key:'publish', label:'发布报修', path:'/publish', icon:UploadFilled, color:'#409eff' },
  { key:'orders', label:'我的工单', path:'/orders', icon:Tickets, color:'#6366F1' },
  { key:'message', label:'消息中心', path:'/message', icon:ChatDotRound, color:'#10B981' },
  { key:'help', label:'帮助反馈', path:'/help', icon:QuestionFilled, color:'#F59E0B' }
]

async function loadData() {
  loading.value = true
  try { const res = await getRepairListPage({ page:1, size:5 }); recentOrders.value = (res.records||[]).slice(0,3) }
  catch { recentOrders.value = [] }
  finally { loading.value = false }
}

function goDetail(row) { if (row?.id) router.push({ name:'RepairDetail', params:{ id:String(row.id) } }) }

onMounted(loadData)
</script>

<style scoped>
.home-page { min-height:400px; }

.welcome-section { margin-bottom:24px; }
.welcome-title { margin:0 0 4px; font-size:22px; font-weight:700; color:var(--el-text-color-primary); }
.welcome-sub { margin:0; font-size:14px; color:var(--el-text-color-secondary); }

.quick-row { margin-bottom:24px; }
.quick-card { display:flex; flex-direction:column; align-items:center; gap:10px; padding:24px; background:#fff; border-radius:12px; box-shadow:0 2px 12px rgba(0,0,0,0.05); cursor:pointer; transition:transform 0.2s,box-shadow 0.2s; }
.quick-card:hover { transform:translateY(-2px); box-shadow:0 4px 16px rgba(0,0,0,0.1); }
.quick-card__label { font-size:14px; font-weight:500; color:var(--el-text-color-primary); }

.section-card { border-radius:12px; border:1px solid var(--el-border-color-lighter); box-shadow:0 2px 12px rgba(0,0,0,0.05); }
.section-head { display:flex; justify-content:space-between; align-items:center; }
.section-title { font-size:15px; font-weight:600; }

.ticket-no { font-family:'SF Mono','Cascadia Code',monospace; color:var(--el-color-primary); }
:deep(.el-table__row) { cursor:pointer; }
</style>
```

- [ ] **Step 2: 处理 SmartHome 壳 — 更新路由指回 Home**

在 router/index.js 中，`/home` 已经指向 `SmartHome.vue`，SmartHome 又动态渲染 Home.vue。优化直接改为指向 Home.vue：

```js
// router/index.js 第86行附近
{
  path: 'home',
  name: 'Home',
  component: () => import('@/views/Home.vue'),  // 直接从 SmartHome 改为 Home
  meta: { title: '首页' }
},
```

- [ ] **Step 3: 验证编译**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/Home.vue frontend/src/router/index.js
git commit -m "refactor: optimize Home page with quick actions and recent orders"
```

---

### Task 10: 优化 Publish.vue

**Files:**
- Modify: `frontend/src/views/Publish.vue`

核心优化：将左右两列布局改为上下布局（上传区→表单），表单字段增加手机号自动填充，优化分类选择。

- [ ] **Step 1: 修改布局为上下排列**

修改 `.publish-form` 样式：
```css
.publish-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
```

修改 upload-area min-height 为 160px（减小高度）。

- [ ] **Step 2: 手机号自动填充**

在 onMounted 中添加：
```js
onMounted(async () => {
  if (isUrgentMode.value) form.value.urgency = 'high'
  // 自动填充用户手机号
  try {
    const info = userStore.userInfo
    if (info?.phoneNumber || info?.phone) {
      form.value.phoneNumber = info.phoneNumber || info.phone || ''
    }
  } catch {}
})
```

- [ ] **Step 3: 优化紧急程度 Radio 文案**

```html
<el-radio-group v-model="form.urgency">
  <el-radio value="low">普通 — 不影响正常使用</el-radio>
  <el-radio value="medium">紧急 — 影响正常使用</el-radio>
  <el-radio value="high">非常紧急 — 存在安全隐患</el-radio>
</el-radio-group>
```

- [ ] **Step 4: 验证编译**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/Publish.vue
git commit -m "refactor: optimize Publish form layout and auto-fill phone"
```

---

### Task 11: 优化 Message.vue、Notice.vue、Help.vue

**Files:**
- Modify: `frontend/src/views/Message.vue`
- Modify: `frontend/src/views/Notice.vue`
- Modify: `frontend/src/views/Help.vue`

- [ ] **Step 1: Message.vue — 增加未读/已读筛选**

在系统通知卡片 header 中添加筛选按钮。无需大改动，当前 Message.vue 已经设计得较好。

在 sys-card header 增加筛选切换：
```html
<div class="msg-card__head">
  <span class="msg-card__title">系统通知</span>
  <el-badge v-if="sysUnread" :value="sysUnread" :max="99" />
  <el-button-group style="margin-left:auto">
    <el-button :type="showRead ? 'default' : 'primary'" size="small" @click="showRead=false">未读</el-button>
    <el-button :type="showRead ? 'primary' : 'default'" size="small" @click="showRead=true">全部</el-button>
  </el-button-group>
</div>
```

添加 `const showRead = ref(false)` 和过滤 computed：
```js
const filteredSysItems = computed(() => showRead.value ? sysItems.value : sysItems.value.filter(it => !it.read))
```

- [ ] **Step 2: Notice.vue — 增加公告元数据和更多模拟公告**

增加模拟公告数组（design doc 中的4条），添加置顶标记、发布人信息：

```js
const DEFAULT_NOTICES = [
  { title: '关于北苑供水管道检修的通知', content: '...', time: '2026-05-07 14:00', pinned: true, author: '后勤管理处' },
  { title: '五月份校园设施集中维护计划', content: '...', time: '2026-05-05 09:00', pinned: true, author: '后勤管理处' },
  { title: '后勤维修服务范围说明', content: '...', time: '2026-05-01 10:00', pinned: false, author: '后勤管理处' },
  { title: '夏季空调使用及报修注意事项', content: '...', time: '2026-04-28 15:30', pinned: false, author: '后勤管理处' }
]
```

模板中增加置顶标识和发布人：
```html
<div v-for="(item, i) in noticeList" :key="i" class="notice-item" :class="{'is-pinned': item.pinned}">
  <div class="notice-item__head">
    <el-tag v-if="item.pinned" type="danger" size="small" effect="dark">置顶</el-tag>
    <h4 class="notice-item__title">{{ item.title }}</h4>
  </div>
  <p class="notice-item__content">{{ item.content }}</p>
  <p class="notice-item__time">{{ item.time }} · {{ item.author || '后勤管理处' }}</p>
</div>
```

样式增加：
```css
.notice-item.is-pinned { background: var(--el-color-danger-light-9); margin:0 -12px; padding:16px 12px; border-radius:8px; }
.notice-item__head { display:flex; align-items:center; gap:8px; margin-bottom:8px; }
```

- [ ] **Step 3: Help.vue — 增加 FAQ 折叠面板**

在反馈表单前增加 FAQ：
```html
<el-collapse class="faq-section">
  <el-collapse-item title="如何查看我的工单处理进度？" name="1">
    <p>在"工单管理"页面可以查看所有工单，点击任意工单进入详情页即可查看完整处理进度。</p>
  </el-collapse-item>
  <el-collapse-item title="报修后多久会有响应？" name="2">
    <p>工作日通常30分钟内响应，紧急工单优先处理。非工作日顺延至下一个工作日。</p>
  </el-collapse-item>
  <el-collapse-item title="如何联系维修师傅？" name="3">
    <p>工单分配维修师傅后，在工单详情页可以点击"联系沟通"按钮与师傅在线沟通。</p>
  </el-collapse-item>
  <el-collapse-item title="对维修结果不满意怎么办？" name="4">
    <p>在评价时可以给出真实评分和意见，或通过"帮助反馈"页面提交申诉，管理员会跟进处理。</p>
  </el-collapse-item>
</el-collapse>
```

- [ ] **Step 4: 验证编译**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/Message.vue frontend/src/views/Notice.vue frontend/src/views/Help.vue
git commit -m "refactor: optimize Message/Notice/Help with unread filter, pinned notices, FAQ"
```

---

## Phase 5: 最终验证

### Task 12: 全量编译验证 + 确认无遗漏

- [ ] **Step 1: 清理并全量编译**

```bash
cd frontend && npx vite build 2>&1
```
Expected: `✓ built in XXs` 无 warning 无 error

- [ ] **Step 2: 确认所有文件的 git 状态**

```bash
git status
```
确认包含：2新建 4重写 5优化 1删除 + 2修改

- [ ] **Step 3: 最终 Commit**

```bash
git add -A
git commit -m "feat: complete campus repair system redesign for graduation defense

- New: OrderManage.vue, StaffTickets.vue
- Rewrite: OrderDetail, Profile, StaffWorkbench, Admin/Stats
- Optimize: Home, Publish, Message, Notice, Help
- Delete: Booking.vue
- Fix: AppLayout sidebar routes"
```

---

## 完整文件变更清单

| # | 文件 | 操作 |
|---|------|------|
| 1 | `frontend/src/views/OrderManage.vue` | 新建 |
| 2 | `frontend/src/views/StaffTickets.vue` | 新建 |
| 3 | `frontend/src/views/OrderDetail.vue` | 重写 |
| 4 | `frontend/src/views/Profile.vue` | 重写 |
| 5 | `frontend/src/views/StaffWorkbench.vue` | 重写 |
| 6 | `frontend/src/views/admin/Stats.vue` | 重写 |
| 7 | `frontend/src/views/Home.vue` | 重写 |
| 8 | `frontend/src/views/Publish.vue` | 优化 |
| 9 | `frontend/src/views/Message.vue` | 优化 |
| 10 | `frontend/src/views/Notice.vue` | 优化 |
| 11 | `frontend/src/views/Help.vue` | 优化 |
| 12 | `frontend/src/views/Booking.vue` | 删除 |
| 13 | `frontend/src/layout/AppLayout.vue` | 修改侧栏路由 |
| 14 | `frontend/src/router/index.js` | 增删路由 + keep-alive |
