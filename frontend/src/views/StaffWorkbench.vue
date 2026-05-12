<template>
  <div class="staff-workbench">
    <div class="welcome-row">
      <div>
        <h2 class="welcome-text">{{ greeting }}，{{ userName }}</h2>
        <p class="welcome-date">{{ todayDate }}</p>
      </div>
      <el-button type="primary" @click="$router.push('/staff/tickets')">查看全部工单</el-button>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.key">
        <div class="stat-card" @click="$router.push('/staff/tickets')">
          <div class="stat-card__value">{{ card.value }}</div>
          <div class="stat-card__label">{{ card.label }}</div>
        </div>
      </el-col>
    </el-row>

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
          <template #default="{row}"><el-tag v-if="row.isUrgent" type="danger" size="small">紧急</el-tag><el-tag v-else type="info" size="small">普通</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{row}">
            <el-button v-if="row.statusCode===4" type="primary" size="small" @click.stop="handleAccept(row)">接单</el-button>
            <el-button v-else-if="row.statusCode===5" type="success" size="small" @click.stop="handleComplete(row)">完成</el-button>
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
import { getRepairListPage, acceptOrder, completeOrder } from '@/api/repair'

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
  catch (e) {
    records.value = []
    ElMessage.error(e?.message || '加载工单列表失败')
  }
  finally { loading.value = false }
}

async function handleAccept(row) { try { await acceptOrder(row.id); ElMessage.success('已接单'); await loadData() } catch(e) { ElMessage.error(e?.message||'操作失败') } }
async function handleComplete(row) { try { await completeOrder(row.id); ElMessage.success('已标记完成'); await loadData() } catch(e) { ElMessage.error(e?.message||'操作失败') } }
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
:deep(.el-table__row) { cursor:pointer; }
</style>
