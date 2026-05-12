<template>
  <div class="order-manage">
    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索工单号、地点或故障描述" clearable style="width: 260px" @keyup.enter="onSearch" @clear="onSearch">
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

    <el-table :data="records" stripe border size="default" v-loading="loading" highlight-current-row @row-click="goDetail" class="order-table">
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
      <el-table-column prop="createTime" label="报修时间" width="170" sortable />
      <el-table-column label="操作" width="80" align="center" fixed="right">
        <template #default="{ row }">
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

.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }

:deep(.el-table__row) { cursor: pointer; }
</style>
