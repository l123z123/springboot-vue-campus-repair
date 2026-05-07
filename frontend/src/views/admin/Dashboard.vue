<template>
  <div class="admin-dashboard">
    <div class="admin-dashboard__welcome">
      {{ userDisplayName }}，{{ todayLabel }}
    </div>

    <div v-if="loading" class="admin-dashboard__loading">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <!-- 顶部统计卡片 -->
      <el-row class="dashboard-stat-row" :gutter="16">
        <el-col
          v-for="card in statCards"
          :key="card.key"
          :xs="24"
          :sm="12"
          :lg="6"
        >
          <el-card class="dashboard-stat-card" shadow="never">
            <div class="dashboard-stat-card__header">
              <div class="dashboard-stat-card__icon" :class="`is-${card.key}`">
                <el-icon>
                  <component :is="card.icon" />
                </el-icon>
              </div>
              <div class="dashboard-stat-card__info">
                <div class="dashboard-stat-card__label">{{ card.label }}</div>
                <div class="dashboard-stat-card__value">{{ card.value }}</div>
              </div>
              <div
                class="dashboard-stat-card__trend"
                :class="{ 'is-up': card.trend >= 0, 'is-down': card.trend < 0 }"
              >
                <el-icon v-if="card.trend >= 0"><CaretTop /></el-icon>
                <el-icon v-else><CaretBottom /></el-icon>
                <span>{{ Math.abs(card.trend) }}%</span>
              </div>
            </div>
            <div
              class="dashboard-stat-card__sparkline"
              :ref="(el) => setSparklineRef(card.key, el)"
            />
          </el-card>
        </el-col>
      </el-row>

      <!-- 待处理工单列表 -->
      <div class="pending-table">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
          <h4 class="pending-table__title" style="margin: 0;">待处理工单 (SLA超时预警)</h4>
          <el-button type="primary" link @click="goToWorkorders">查看全部工单 <el-icon><ArrowRight /></el-icon></el-button>
        </div>
        <el-table
          :data="pendingList"
          stripe
          border
          size="small"
          v-loading="tableLoading"
          :row-class-name="pendingRowClassName"
        >
          <template #empty>
            <el-empty description="暂无待处理工单" />
          </template>
          <el-table-column prop="ticketNo" label="工单号" width="140">
            <template #default="{ row }">
              <span class="pending-ticket-no" style="font-family: monospace;">{{ row.ticketNo || row.id || row.orderId || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="urgency" label="紧急度" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="urgencyTagType(row.urgency)" size="small" effect="dark">
                {{ urgencyLabel(row.urgency) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="故障标题" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="pending-title">{{ row.title || row.description || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="location" label="地点" width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="pending-location">{{ row.location || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag
                size="small"
                :type="getStatusTagType(row.statusCode)"
                :class="['pending-status-tag', `pending-status-tag--c${row.statusCode}`]"
              >
                {{ getRepairStatusLabel(row.statusCode) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="date" label="提交时间" width="180">
            <template #default="{ row }">
              <span class="pending-date">{{ row.date }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-tooltip content="查看详情" placement="top">
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="showDetail(row)"
                >
                  <el-icon><View /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="前往处理" placement="top">
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="handleProcess(row)"
                >
                  <el-icon><Edit /></el-icon>
                </el-button>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <!-- 工单详情弹窗（PC 端专业样式） -->
    <el-dialog
      v-model="detailVisible"
      title="工单详情"
      width="640px"
      destroy-on-close
      class="order-detail-dialog"
    >
      <div v-if="detailLoading" class="detail-loading">
        <el-skeleton :rows="6" animated />
      </div>
      <template v-else>
        <div v-if="currentOrder.image" class="detail-dialog__image">
          <el-image :src="currentOrder.image" fit="contain" style="max-height: 200px; width: 100%;" />
        </div>
        <el-descriptions :column="2" border class="detail-dialog__desc">
          <el-descriptions-item label="工单号">{{ currentOrder.ticketNo || currentOrder.id || currentOrder.orderId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(currentOrder.statusCode)" size="small">
              {{ getRepairStatusLabel(currentOrder.statusCode) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ currentOrder.title || currentOrder.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ currentOrder.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="校区">{{ currentOrder.campus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="紧急度">
            <el-tag :type="urgencyTagType(currentOrder.urgency)" size="small">
              {{ urgencyLabel(currentOrder.urgency) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ currentOrder.createTime || currentOrder.date || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentOrder.phoneNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="故障描述" :span="2">
            <div class="detail-dialog__desc-text">{{ currentOrder.description || currentOrder.title || '-' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="goToWorkorders">前往处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { CaretTop, CaretBottom, DataLine, List, Check, Warning, View, Edit, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getStatsOverview } from '@/api/dashboard'
import { getRepairListPage, getRepairDetail } from '@/api/repair'
import { getRepairStatusLabel, getStatusTagType } from '@/constants/repairStatus'

defineOptions({ name: 'AdminDashboard' })

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const tableLoading = ref(false)
const stats = ref({
  totalCount: 0,
  pendingCount: 0,
  completedCount: 0,
  urgentCount: 0
})
const pendingList = ref([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentOrder = ref({})

// 迷你折线图相关
const sparklineRefs = ref({})
const sparklineCharts = ref({})
// 简单的 7 日趋势 mock 数据，占位用，后续可从后端统计接口获取
const sparklineData = ref({
  total: [60, 72, 80, 65, 90, 110, 95],
  pending: [20, 18, 25, 22, 28, 26, 24],
  completed: [30, 40, 38, 45, 55, 60, 58],
  urgent: [5, 6, 7, 6, 8, 9, 7]
})

const userDisplayName = computed(() => {
  const u = userStore.userInfo
  return (u && (u.realName || u.username)) || '管理员'
})

const todayLabel = computed(() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${week}`
})

const statCards = computed(() => [
  {
    key: 'total',
    label: '总工单数',
    value: stats.value.totalCount ?? 0,
    trend: 12,
    icon: DataLine
  },
  {
    key: 'pending',
    label: '待处理',
    value: stats.value.pendingCount ?? 0,
    trend: -5,
    icon: List
  },
  {
    key: 'completed',
    label: '已完成',
    value: stats.value.completedCount ?? 0,
    trend: 18,
    icon: Check
  },
  {
    key: 'urgent',
    label: '紧急工单',
    value: stats.value.urgentCount ?? 0,
    trend: 3,
    icon: Warning
  }
])


function urgencyLabel(u) {
  const map = { 1: '普通', 2: '一般', 3: '紧急', low: '普通', medium: '一般', high: '紧急' }
  return map[u] ?? u ?? '-'
}

function urgencyTagType(u) {
  const map = { 1: 'info', 2: 'warning', 3: 'danger', low: 'info', medium: 'warning', high: 'danger' }
  return map[u] ?? 'info'
}

function setSparklineRef(key, el) {
  if (!el) return
  if (!sparklineRefs.value[key]) {
    sparklineRefs.value[key] = el
  }
}

function initSparkline(key) {
  const el = sparklineRefs.value[key]
  if (!el) return
  if (!sparklineCharts.value[key]) {
    sparklineCharts.value[key] = echarts.init(el)
  }
  const chart = sparklineCharts.value[key]
  const data = sparklineData.value[key] || []
  chart.setOption({
    grid: { top: 4, bottom: 0, left: 0, right: 0 },
    xAxis: {
      type: 'category',
      show: false,
      data: data.map((_, idx) => idx + 1)
    },
    yAxis: { type: 'value', show: false },
    tooltip: { show: false },
    series: [
      {
        type: 'line',
        smooth: true,
        showSymbol: false,
        data,
        lineStyle: { width: 2, color: '#6366F1' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(99,102,241,0.35)' },
            { offset: 1, color: 'rgba(99,102,241,0.02)' }
          ])
        }
      }
    ]
  })
}

function pendingRowClassName() {
  return 'pending-row'
}

async function showDetail(row) {
  const id = row.id || row.orderId
  if (!id) return
  detailVisible.value = true
  detailLoading.value = true
  currentOrder.value = {}
  try {
    const data = await getRepairDetail(id)
    currentOrder.value = data ? { ...data } : {}
  } catch (_) {
    currentOrder.value = { ...row }
  } finally {
    detailLoading.value = false
  }
}

function goToWorkorders() {
  detailVisible.value = false
  const id = currentOrder.value?.id ?? currentOrder.value?.orderId
  if (id) {
    window.open(router.resolve({ path: `/repair/detail/${id}` }).href, '_blank')
  } else {
    router.push('/admin/tickets')
  }
}

function handleProcess(row) {
  const id = row.id ?? row.orderId
  if (id) {
    window.open(router.resolve({ path: `/repair/detail/${id}` }).href, '_blank')
  } else {
    router.push('/admin/tickets')
  }
}

onMounted(async () => {
  try {
    const overview = await getStatsOverview()
    if (overview) {
      stats.value.totalCount = overview.total ?? 0
      stats.value.pendingCount = overview.pending ?? 0
      stats.value.completedCount = overview.completed ?? 0
      stats.value.urgentCount = overview.urgent ?? 0
    }
  } catch (_) {
    try {
      const [total, statusDist] = await Promise.all([
        import('@/api/dashboard').then((m) => m.getTotalStats()),
        import('@/api/dashboard').then((m) => m.getStatusDist())
      ])
      stats.value.totalCount = total ?? 0
      const dist = Array.isArray(statusDist) ? statusDist : []
      stats.value.pendingCount = dist.find((d) => String(d.name) === '0')?.value ?? 0
      stats.value.completedCount = dist.find((d) => String(d.name) === '2')?.value ?? 0
    } catch (_) {}
  } finally {
    loading.value = false
  }

  tableLoading.value = true
  try {
    const { records } = await getRepairListPage({ statusIn: '0,1,11', page: 1, size: 10 })
    pendingList.value = records ?? []
  } catch (_) {
    pendingList.value = []
  } finally {
    tableLoading.value = false
  }

  // 初始化迷你趋势图
  statCards.value.forEach((card) => initSparkline(card.key))
})
</script>

<style scoped>
.admin-dashboard {
  min-height: 400px;
}

.admin-dashboard__welcome {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 24px;
  color: var(--el-text-color-primary);
}

.admin-dashboard__loading {
  padding: 20px 0;
}

.dashboard-stat-row {
  margin-bottom: 24px;
}

.dashboard-stat-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  border: none;
  padding: 18px 20px 10px;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.dashboard-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.08);
}

.dashboard-stat-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dashboard-stat-card__icon {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(99, 102, 241, 0.12);
  color: #6366f1;
  flex-shrink: 0;
}

.dashboard-stat-card__icon.is-pending {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.dashboard-stat-card__icon.is-completed {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.dashboard-stat-card__icon.is-urgent {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.dashboard-stat-card__info {
  flex: 1;
  min-width: 0;
}

.dashboard-stat-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.dashboard-stat-card__value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.6;
  color: var(--el-text-color-primary);
}

.dashboard-stat-card__trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
}

.dashboard-stat-card__trend.is-up {
  color: #16a34a;
}

.dashboard-stat-card__trend.is-down {
  color: #dc2626;
}

.dashboard-stat-card__sparkline {
  margin-top: 8px;
  height: 36px;
  width: 100%;
}

.pending-table {
  margin-top: 24px;
}

.pending-table__title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
}

.pending-ticket-no {
  font-weight: 500;
  color: var(--el-color-primary);
}

.pending-title {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.pending-location {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.pending-date {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.pending-status-tag {
  border-radius: 999px;
  border: none;
  padding: 0 10px;
  font-size: 12px;
}

.pending-status-tag--pending {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.pending-status-tag--processing {
  background: rgba(99, 102, 241, 0.12);
  color: #4338ca;
}

.pending-status-tag--done,
.pending-status-tag--evaluated {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.pending-status-tag--cancelled {
  background: rgba(148, 163, 184, 0.18);
  color: #4b5563;
}

.pending-row:hover > td.el-table__cell {
  background-color: rgba(99, 102, 241, 0.04) !important;
}

.detail-loading {
  padding: 20px 0;
}

.detail-dialog__image {
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-fill-color);
}

.detail-dialog__desc-text {
  max-height: 120px;
  overflow-y: auto;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
