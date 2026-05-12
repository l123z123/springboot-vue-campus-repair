<template>
  <div class="home-page">
    <div class="welcome-section">
      <h2 class="welcome-title">{{ greeting }}，{{ userName }}</h2>
      <p class="welcome-sub">{{ todayDate }}</p>
    </div>

    <el-row :gutter="16" class="quick-row">
      <el-col :span="6" v-for="item in quickActions" :key="item.key">
        <div class="quick-card" @click="$router.push(item.path)">
          <el-icon :size="28" :color="item.color"><component :is="item.icon" /></el-icon>
          <span class="quick-card__label">{{ item.label }}</span>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-head">
          <span class="section-title">最近工单</span>
          <el-button text type="primary" @click="$router.push('/orders')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentOrders" stripe size="default" v-loading="loading" @row-click="goDetail">
        <template #empty>
          <el-empty description="暂无工单" :image-size="60">
            <el-button type="primary" @click="$router.push('/publish')">提交报修</el-button>
          </el-empty>
        </template>
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
import { ElMessage } from 'element-plus'
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
  catch (e) {
    recentOrders.value = []
    ElMessage.error(e?.message || '加载工单列表失败')
  }
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
.quick-card {
  display:flex; flex-direction:column; align-items:center; gap:10px;
  padding:24px; background:#fff; border-radius:12px; box-shadow:0 2px 12px rgba(0,0,0,0.05);
  cursor:pointer; transition:transform 0.2s,box-shadow 0.2s;
}
.quick-card:hover { transform:translateY(-2px); box-shadow:0 4px 16px rgba(0,0,0,0.1); }
.quick-card__label { font-size:14px; font-weight:500; color:var(--el-text-color-primary); }

.section-card { border-radius:12px; border:1px solid var(--el-border-color-lighter); box-shadow:0 2px 12px rgba(0,0,0,0.05); }
.section-head { display:flex; justify-content:space-between; align-items:center; }
.section-title { font-size:15px; font-weight:600; }

.ticket-no { font-family:'SF Mono','Cascadia Code',monospace; color:var(--el-color-primary); }
:deep(.el-table__row) { cursor:pointer; }
</style>
