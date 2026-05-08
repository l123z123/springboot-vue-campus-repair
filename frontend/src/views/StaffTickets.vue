<template>
  <div class="staff-tickets">
    <el-row :gutter="16" class="stat-row">
      <el-col :span="8" v-for="card in statCards" :key="card.key">
        <div class="stat-card" @click="switchTab(card.filterTab)">
          <div class="stat-card__value">{{ card.value }}</div>
          <div class="stat-card__label">{{ card.label }}</div>
        </div>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" @tab-change="onTabChange" class="ticket-tabs">
      <el-tab-pane label="待处理" name="pending" />
      <el-tab-pane label="进行中" name="processing" />
      <el-tab-pane label="已完成" name="done" />
    </el-tabs>

    <el-table :data="records" stripe border size="default" v-loading="loading" highlight-current-row @row-click="goDetail">
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
        <template #default="{ row }">{{ row.reporterName || row.studentName || '-' }}</template>
      </el-table-column>
      <el-table-column label="紧急度" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isUrgent || row.urgency === 'high'" type="danger" size="small">紧急</el-tag>
          <el-tag v-else-if="row.urgency === 'medium'" type="warning" size="small">中等</el-tag>
          <el-tag v-else type="info" size="small">普通</el-tag>
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
      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="loadData" />
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

const TAB_STATUS_MAP = { pending: '4', processing: '5', done: '6,7,8' }

const statCards = computed(() => {
  return [
    { key: 'pending', label: '待接单', value: tabCounts.value.pending, filterTab: 'pending' },
    { key: 'processing', label: '维修中', value: tabCounts.value.processing, filterTab: 'processing' },
    { key: 'done', label: '已完成', value: tabCounts.value.done, filterTab: 'done' }
  ]
})

const tabCounts = ref({ pending: 0, processing: 0, done: 0 })

async function loadCounts() {
  try {
    const [p, r, d] = await Promise.all([
      getRepairListPage({ page: 1, size: 1, statusIn: '4' }),
      getRepairListPage({ page: 1, size: 1, statusIn: '5' }),
      getRepairListPage({ page: 1, size: 1, statusIn: '6,7,8' })
    ])
    tabCounts.value = { pending: p.total || 0, processing: r.total || 0, done: d.total || 0 }
  } catch {}
}

async function loadData() {
  loading.value = true
  try {
    const statusIn = TAB_STATUS_MAP[activeTab.value]
    const res = await getRepairListPage({ page: page.value, size: pageSize, statusIn })
    records.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally { loading.value = false }
}

async function handleAccept(row) {
  try { await acceptOrder(row.id); ElMessage.success('已接单'); await refreshAll() }
  catch (e) { ElMessage.error(e?.message || '操作失败') }
}
async function handleComplete(row) {
  try { await completeOrder(row.id); ElMessage.success('已标记完成'); await refreshAll() }
  catch (e) { ElMessage.error(e?.message || '操作失败') }
}
function goDetail(row) { if (row?.id) router.push({ name: 'RepairDetail', params: { id: String(row.id) } }) }
function onTabChange() { page.value = 1; loadData(); loadCounts() }
function switchTab(tab) { activeTab.value = tab; onTabChange() }
async function refreshAll() { await Promise.all([loadData(), loadCounts()]) }

onMounted(() => refreshAll())
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
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
:deep(.el-table__row) { cursor: pointer; }
</style>
