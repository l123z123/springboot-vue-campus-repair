<template>
  <div class="profile-page">
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else>
      <!-- 用户头部卡片 -->
      <el-card shadow="never" class="profile-card">
        <div class="user-header">
          <div class="user-header__avatar">
            <el-avatar :size="80" :src="userAvatarUrl">
              <el-icon :size="36"><UserFilled /></el-icon>
            </el-avatar>
          </div>
          <div class="user-header__body">
            <div class="user-header__name">{{ form.nickname || '未设置' }}</div>
            <div class="user-header__meta">
              <span>{{ form.department || '未选择院系' }}</span>
              <span class="meta-sep">·</span>
              <span>{{ roleLabel }} {{ userInfo.studentId }}</span>
            </div>
            <div v-if="form.signature" class="user-header__sig">"{{ form.signature }}"</div>
          </div>
          <div class="user-header__action">
            <el-button v-if="!editing" type="primary" @click="startEdit">编辑资料</el-button>
          </div>
        </div>
      </el-card>

      <!-- 统计卡片 -->
      <el-card shadow="never" class="profile-card">
        <template #header><span class="card-title">我的统计</span></template>
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-item__value">{{ stats.total }}</div>
              <div class="stat-item__label">报修总数</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-item__value">{{ stats.completed }}</div>
              <div class="stat-item__label">已完成</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-item__value">{{ stats.avgRating }}</div>
              <div class="stat-item__label">评价均分</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 编辑资料卡片 -->
      <el-card v-if="editing" shadow="never" class="profile-card">
        <template #header><span class="card-title">编辑资料</span></template>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="edit-form">
          <el-form-item label="头像">
            <div class="avatar-row">
              <el-upload
                class="avatar-uploader"
                :action="uploadUrl"
                :headers="uploadHeaders"
                name="file"
                :show-file-list="false"
                :on-success="handleAvatarSuccess"
                :on-error="handleAvatarError"
                :before-upload="beforeAvatarUpload"
                accept="image/*"
              >
                <img v-if="form.avatarUrl" :src="avatarDisplayUrl" class="avatar-img" />
                <div v-else class="avatar-placeholder">
                  <el-icon><UserFilled /></el-icon>
                </div>
              </el-upload>
              <span class="avatar-tip">jpg/png，不超过 2MB</span>
            </div>
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" maxlength="20" show-word-limit style="max-width:360px" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio :value="1">男</el-radio>
              <el-radio :value="2">女</el-radio>
              <el-radio :value="0">保密</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="院系">
            <el-select v-model="form.department" placeholder="请选择" clearable style="max-width:360px">
              <el-option v-for="d in departmentOptions" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" maxlength="11" placeholder="11位手机号" style="max-width:360px" />
          </el-form-item>
          <el-form-item label="签名">
            <el-input v-model="form.signature" type="textarea" :rows="2" maxlength="50" show-word-limit placeholder="写点什么..." style="max-width:480px" />
          </el-form-item>
          <el-form-item>
            <el-button @click="cancelEdit">取消</el-button>
            <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
          </el-form-item>
        </el-form>
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
import { ElMessage } from 'element-plus'
import { UserFilled, Lock, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUserInfo, updateUserProfile } from '@/api/user'
import { getRepairListPage } from '@/api/repair'
import { normalizeImageUrl } from '@/utils/image'

defineOptions({ name: 'Profile' })

const userStore = useUserStore()
const loading = ref(true)
const editing = ref(false)
const saving = ref(false)
const formRef = ref(null)

const userInfo = ref({ name: '', studentId: '', department: '', avatar: '', avatarUrl: '', signature: '' })
const stats = ref({ total: 0, completed: 0, avgRating: '-' })
const form = ref({ avatarUrl: '', nickname: '', gender: 0, department: '', phone: '', signature: '' })

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const uploadUrl = computed(() => {
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return origin ? `${origin}${baseURL.replace(/\/$/, '')}/common/upload` : `${baseURL}/common/upload`
})
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const userAvatarUrl = computed(() => userInfo.value.avatarUrl || userInfo.value.avatar || '')
const avatarDisplayUrl = computed(() => normalizeImageUrl(form.value.avatarUrl))

const roleLabel = computed(() => {
  const role = userStore.userInfo?.role
  if (role === 1) return '工号'
  if (role === 2) return '管理员'
  return '学号'
})

const departmentOptions = [
  { value: '计算机学院', label: '计算机学院' },
  { value: '机械工程学院', label: '机械工程学院' },
  { value: '经济管理学院', label: '经济管理学院' },
  { value: '文学院', label: '文学院' },
  { value: '后勤保障部', label: '后勤保障部' }
]

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

async function loadData() {
  try {
    const data = await getUserInfo()
    userStore.setUserInfo(data)
    userInfo.value = {
      name: data.nickname || data.realName || data.username || '',
      studentId: data.username || '',
      department: data.department || '',
      avatar: data.avatar || '',
      avatarUrl: data.avatarUrl || '',
      signature: data.signature && data.signature !== '暂无签名' ? data.signature : ''
    }
    form.value = {
      avatarUrl: data.avatarUrl || data.avatar || '',
      nickname: data.nickname || data.realName || data.username || '',
      gender: data.gender ?? 0,
      department: data.department || '',
      phone: data.phone || '',
      signature: data.signature && data.signature !== '暂无签名' ? data.signature : ''
    }
  } catch (e) {
    ElMessage.error(e?.message || '获取用户信息失败')
  }
}

async function loadStats() {
  try {
    const [allRes, doneRes] = await Promise.all([
      getRepairListPage({ page: 1, size: 1 }),
      getRepairListPage({ page: 1, size: 1, statusIn: '6,7,8' })
    ])
    stats.value.total = allRes.total || 0
    stats.value.completed = doneRes.total || 0
  } catch {}
}

function startEdit() { editing.value = true }

function cancelEdit() {
  loadData()
  editing.value = false
}

function beforeAvatarUpload(file) {
  if (file.size / 1024 / 1024 >= 2) { ElMessage.error('头像大小不能超过 2MB'); return false }
  return true
}

function handleAvatarSuccess(res) {
  const data = res?.data ?? res
  const url = typeof data === 'string' ? data : data?.url
  if (url) form.value.avatarUrl = url
}

function handleAvatarError() { ElMessage.error('头像上传失败') }

async function onSave() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    const updated = await updateUserProfile({
      avatarUrl: form.value.avatarUrl || null,
      nickname: form.value.nickname,
      gender: form.value.gender,
      department: form.value.department || null,
      phone: form.value.phone || null,
      signature: form.value.signature || null
    })
    userStore.setUserInfo(updated)
    userInfo.value = {
      name: updated.nickname || updated.realName || updated.username || '',
      studentId: updated.username || '',
      department: updated.department || '',
      avatar: updated.avatar || '',
      avatarUrl: updated.avatarUrl || updated.avatar || '',
      signature: updated.signature && updated.signature !== '暂无签名' ? updated.signature : ''
    }
    ElMessage.success('资料已更新')
    editing.value = false
  } catch (e) {
    ElMessage.error(e?.message || '更新失败')
  } finally { saving.value = false }
}

onMounted(async () => {
  loading.value = true
  try { await Promise.all([loadData(), loadStats()]) } finally { loading.value = false }
})
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.loading-wrap { padding: 20px 0; }

.profile-card {
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.card-title { font-size: 15px; font-weight: 600; }

/* ---- 用户头部 ---- */
.user-header {
  display: flex;
  align-items: center;
  gap: 24px;
}

.user-header__avatar {
  flex-shrink: 0;
}

.user-header__avatar :deep(.el-avatar) {
  border: 3px solid var(--el-color-primary-light-7);
}

.user-header__body {
  flex: 1;
  min-width: 0;
}

.user-header__name {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.user-header__meta {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.meta-sep {
  margin: 0 6px;
}

.user-header__sig {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
  font-style: italic;
  margin-top: 6px;
}

.user-header__action {
  flex-shrink: 0;
  margin-left: auto;
}

/* ---- 编辑表单 ---- */
.edit-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-img,
.avatar-placeholder {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--el-fill-color-lighter);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.avatar-img { object-fit: cover; }

.avatar-tip { font-size: 12px; color: var(--el-text-color-secondary); }

/* ---- 统计 ---- */
.stat-item {
  text-align: center;
  padding: 12px 0;
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
}

.stat-item__value {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.stat-item__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

/* ---- 安全 ---- */
.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.security-item:hover { background: var(--el-fill-color-lighter); }

.security-item__left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

/* ---- 响应式 ---- */
@media (max-width: 640px) {
  .user-header {
    flex-wrap: wrap;
    gap: 12px;
  }

  .user-header__action {
    margin-left: 0;
    width: 100%;
  }

  .user-header__action .el-button {
    width: 100%;
  }
}
</style>
