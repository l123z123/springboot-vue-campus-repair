<template>
  <div class="dashboard-mobile">
    <div class="dashboard-mobile__tip">
      <el-alert
        type="info"
        :closable="false"
        show-icon
      >
        建议使用电脑访问管理后台，以获得完整功能体验
      </el-alert>
    </div>

    <div class="dashboard-mobile__welcome">
      你好，{{ userDisplayName }}
    </div>

    <div class="dashboard-mobile__cards">
      <div class="stat-card">
        <div class="stat-card__value">{{ stats.totalCount ?? 0 }}</div>
        <div class="stat-card__label">总工单</div>
      </div>
      <div class="stat-card">
        <div class="stat-card__value">{{ stats.pendingCount ?? 0 }}</div>
        <div class="stat-card__label">待处理</div>
      </div>
      <div class="stat-card">
        <div class="stat-card__value">{{ stats.completedCount ?? 0 }}</div>
        <div class="stat-card__label">已完成</div>
      </div>
    </div>

    <div class="dashboard-mobile__actions">
      <el-button type="primary" @click="router.push('/admin/dashboard')">
        进入完整看板（电脑端）
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getStatsOverview } from '@/api/dashboard'

defineOptions({ name: 'DashboardMobile' })

const router = useRouter()
const userStore = useUserStore()

const stats = ref({
  totalCount: 0,
  pendingCount: 0,
  completedCount: 0
})

const userDisplayName = computed(() => {
  const u = userStore.userInfo
  return (u && (u.realName || u.username)) || '管理员'
})

onMounted(async () => {
  try {
    const overview = await getStatsOverview()
    if (overview) {
      stats.value.totalCount = overview.total ?? 0
      stats.value.pendingCount = overview.pending ?? 0
      stats.value.completedCount = overview.completed ?? 0
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
  }
})
</script>

<style scoped>
.dashboard-mobile {
  padding: 16px;
  min-height: 100vh;
  background: var(--el-fill-color-lighter);
}

.dashboard-mobile__tip {
  margin-bottom: 20px;
}

.dashboard-mobile__welcome {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
  color: var(--el-text-color-primary);
}

.dashboard-mobile__cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.stat-card {
  padding: 16px;
  background: var(--el-bg-color);
  border-radius: 12px;
  text-align: center;
}

.stat-card__value {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.stat-card__label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.dashboard-mobile__actions {
  text-align: center;
}
</style>
