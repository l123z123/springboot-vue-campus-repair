<template>
  <div class="admin-settings">
    <el-card class="settings-card" shadow="never">
      <template #header>
        <span>基础设置</span>
      </template>
      <el-form :model="form" label-width="120px" class="settings-form">
        <el-row :gutter="24">
          <el-col :xs="24" :sm="24" :md="12">
            <el-form-item label="系统名称">
              <el-input v-model="form.systemName" placeholder="请输入系统名称" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12">
            <el-form-item label="Logo 上传">
              <el-upload
                class="logo-uploader"
                :action="uploadUrl"
                :headers="uploadHeaders"
                name="file"
                :show-file-list="false"
                :on-success="handleLogoSuccess"
                :before-upload="beforeLogoUpload"
                accept="image/*"
              >
                <img v-if="form.logoUrl" :src="logoDisplayUrl" class="logo-img" />
                <div v-else class="logo-placeholder">
                  <el-icon><Plus /></el-icon>
                  <span>点击上传 Logo</span>
                </div>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="公告内容">
              <el-input
                v-model="form.announcement"
                type="textarea"
                :rows="4"
                placeholder="请输入系统公告内容"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card class="settings-card" shadow="never">
      <template #header>
        <span>工单规则</span>
      </template>
      <el-form :model="form" label-width="120px" class="settings-form">
        <el-row :gutter="24">
          <el-col :xs="24" :sm="24" :md="12">
            <el-form-item label="是否开启审核">
              <el-switch v-model="form.auditEnabled" />
              <span class="form-tip">开启后，工单需审核通过方可分配</span>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12">
            <el-form-item label="自动分配开关">
              <el-switch v-model="form.autoAssignEnabled" />
              <span class="form-tip">开启后，新工单自动分配给空闲维修工</span>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12">
            <el-form-item label="超时提醒时间">
              <el-input-number
                v-model="form.timeoutMinutes"
                :min="0"
                :max="1440"
                placeholder="分钟"
              />
              <span class="form-tip">工单超过此时间未处理将发送提醒（0 表示不提醒）</span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card class="settings-card" shadow="never">
      <template #header>
        <span>数据维护</span>
      </template>
      <div class="data-maintenance">
        <el-button @click="handleClearCache">清除缓存</el-button>
        <el-button @click="handleDataBackup">数据备份</el-button>
      </div>
    </el-card>

    <div class="save-bar">
      <el-button type="primary" :loading="saving" @click="onSave">保存设置</el-button>
      <el-button type="default" plain @click="loadFromStorage">重置未保存更改</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { normalizeImageUrl } from '@/utils/image'

defineOptions({ name: 'AdminSettings' })

const STORAGE_KEY = 'admin_settings'

const saving = ref(false)

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const uploadUrl = computed(() => {
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return origin ? `${origin}${baseURL.replace(/\/$/, '')}/file/upload` : `${baseURL}/file/upload`
})

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const form = reactive({
  systemName: '校园保修管理系统',
  logoUrl: '',
  announcement: '',
  auditEnabled: false,
  autoAssignEnabled: false,
  timeoutMinutes: 60
})

const logoDisplayUrl = computed(() => normalizeImageUrl(form.logoUrl))

function loadFromStorage() {
  try {
    const s = localStorage.getItem(STORAGE_KEY)
    if (s) {
      const data = JSON.parse(s)
      Object.assign(form, data)
    }
  } catch (_) {}
}

function onSave() {
  saving.value = true
  setTimeout(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        systemName: form.systemName,
        logoUrl: form.logoUrl,
        announcement: form.announcement,
        auditEnabled: form.auditEnabled,
        autoAssignEnabled: form.autoAssignEnabled,
        timeoutMinutes: form.timeoutMinutes
      }))
      ElMessage.success('保存成功')
    } catch (_) {
      ElMessage.error('保存失败')
    } finally {
      saving.value = false
    }
  }, 300)
}

function beforeLogoUpload(file) {
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('Logo 大小不能超过 2MB')
    return false
  }
  return true
}

function handleLogoSuccess(res) {
  const data = res?.data ?? res
  const url = typeof data === 'string' ? data : data?.url
  if (url) form.logoUrl = url
}

function handleClearCache() {
  const keysToRemove = [STORAGE_KEY]
  let count = 0
  try {
    keysToRemove.forEach((key) => {
      if (localStorage.getItem(key)) {
        localStorage.removeItem(key)
        count++
      }
    })
    form.systemName = '校园保修管理系统'
    form.logoUrl = ''
    form.announcement = ''
    form.auditEnabled = false
    form.autoAssignEnabled = false
    form.timeoutMinutes = 60
    ElMessage.success(count > 0 ? `已清除 ${count} 项本地缓存` : '缓存已为空')
  } catch (_) {
    ElMessage.error('清除缓存失败')
  }
}

function handleDataBackup() {
  try {
    const backup = {
      exportTime: new Date().toISOString(),
      systemName: form.systemName,
      logoUrl: form.logoUrl,
      announcement: form.announcement,
      auditEnabled: form.auditEnabled,
      autoAssignEnabled: form.autoAssignEnabled,
      timeoutMinutes: form.timeoutMinutes
    }
    const blob = new Blob([JSON.stringify(backup, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `admin-settings-${Date.now()}.json`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('设置已导出为 JSON 文件')
  } catch (_) {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  loadFromStorage()
})
</script>

<style scoped>
.admin-settings {
  display: flex;
  flex-direction: column;
  gap: 20px;
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
}

.settings-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.settings-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.logo-uploader {
  cursor: pointer;
}

.logo-img,
.logo-placeholder {
  width: 120px;
  height: 60px;
  border: 1px dashed var(--el-border-color);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-lighter);
}

.logo-img {
  object-fit: contain;
}

.logo-placeholder {
  flex-direction: column;
  gap: 4px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.data-maintenance {
  display: flex;
  gap: 12px;
}

.save-bar {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
