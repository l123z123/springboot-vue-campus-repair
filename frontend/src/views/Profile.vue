<template>
  <div class="profile-page">
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
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

      <el-card shadow="never" class="profile-card">
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

.profile-card {
  border-radius:12px; border:1px solid var(--el-border-color-lighter);
  box-shadow:0 2px 12px rgba(0,0,0,0.05); margin-bottom:16px;
}
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

.security-item {
  display:flex; justify-content:space-between; align-items:center;
  padding:12px; border-radius:8px; cursor:pointer; transition:background 0.2s;
}
.security-item:hover { background:var(--el-fill-color-lighter); }
.security-item__left { display:flex; align-items:center; gap:10px; font-size:14px; color:var(--el-text-color-primary); }
</style>
