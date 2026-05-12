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

    <!-- Quick Actions -->
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

    <!-- Recent Orders -->
    <div class="recent-section">
      <div class="recent-section__head">
        <span class="recent-section__title">最近工单</span>
        <el-button text type="primary" @click="$router.push('/orders')">查看全部 &rarr;</el-button>
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
  { key:'publish', label:'发布报修', desc:'快速提交维修申请', path:'/publish', icon:UploadFilled, color:'#2563EB' },
  { key:'orders', label:'我的工单', desc:'查看工单进度状态', path:'/orders', icon:Tickets, color:'#6366F1' },
  { key:'message', label:'消息中心', desc:'与维修师傅沟通', path:'/message', icon:ChatDotRound, color:'#10B981' },
  { key:'help', label:'帮助反馈', desc:'提交建议与反馈', path:'/help', icon:QuestionFilled, color:'#F59E0B' }
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

/* Quick Actions */
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

/* Recent Orders */
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
