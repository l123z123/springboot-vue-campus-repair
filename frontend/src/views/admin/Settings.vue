<template>
  <div class="admin-settings">
    <el-card class="settings-card" shadow="never">
      <template #header><span>基础设置</span></template>
      <el-form :model="form" label-width="120px" class="settings-form">
        <el-row :gutter="24">
          <el-col :xs="24" :md="12">
            <el-form-item label="系统名称">
              <el-input v-model="form.systemName" placeholder="请输入系统名称" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
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
        </el-row>
      </el-form>
    </el-card>

    <el-card class="settings-card" shadow="never">
      <template #header><span>发布公告</span></template>
      <el-form ref="announceFormRef" :model="announceForm" :rules="announceRules" label-width="100px" class="announce-form">
        <el-row :gutter="24">
          <el-col :xs="24" :md="16">
            <el-form-item label="公告标题">
              <el-input v-model="announceForm.title" placeholder="请输入公告标题" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="发布者">
              <el-input v-model="announceForm.author" placeholder="后勤管理处" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="24">
            <el-form-item label="公告内容">
              <el-input v-model="announceForm.content" type="textarea" :rows="4" placeholder="请输入公告内容" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="置顶">
              <el-switch v-model="announceForm.pinned" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-button type="primary" :loading="publishing" @click="publishAnnouncement">发布公告</el-button>
          </el-col>
        </el-row>
      </el-form>

      <el-divider />
      <div class="notice-manage-head">
        <span class="notice-manage-head__title">已发布公告（{{ noticeList.length }}）</span>
      </div>
      <div v-if="noticeList.length" class="notice-manage-list" v-loading="noticeLoading">
        <div v-for="item in noticeList" :key="item.id" class="notice-manage-item">
          <div class="notice-manage-item__head">
            <el-tag v-if="item.pinned" type="danger" size="small" effect="dark">置顶</el-tag>
            <span class="notice-manage-item__title">{{ item.title }}</span>
            <span class="notice-manage-item__time">{{ formatTime(item.createTime) }}</span>
          </div>
          <p class="notice-manage-item__content">{{ item.content }}</p>
          <div class="notice-manage-item__meta">
            <span>{{ item.author || '后勤管理处' }}</span>
            <el-button type="danger" link size="small" @click="deleteAnnouncement(item.id)">删除</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无公告" :image-size="48" />
    </el-card>

    <el-card class="settings-card" shadow="never">
      <template #header><span>数据维护</span></template>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { normalizeImageUrl } from '@/utils/image'
import { getNoticeList, createNotice, deleteNotice } from '@/api/notice'

defineOptions({ name: 'AdminSettings' })

const STORAGE_KEY = 'admin_settings'

const saving = ref(false)
const publishing = ref(false)
const noticeLoading = ref(false)

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const uploadUrl = computed(() => {
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return origin ? `${origin}${baseURL.replace(/\/$/, '')}/file/upload` : `${baseURL}/file/upload`
})

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const form = reactive({ systemName: '校园报修管理系统', logoUrl: '' })
const announceForm = reactive({ title: '', content: '', pinned: false, author: '' })
const announceFormRef = ref(null)
const announceRules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { max: 100, message: '标题不超过100字', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' },
    { max: 500, message: '内容不超过500字', trigger: 'blur' }
  ]
}
const noticeList = ref([])

const logoDisplayUrl = computed(() => normalizeImageUrl(form.logoUrl))

function formatTime(t) {
  if (!t) return ''
  if (typeof t === 'string') return t.replace('T', ' ').substring(0, 19)
  return t
}

async function loadNoticeList() {
  noticeLoading.value = true
  try {
    const res = await getNoticeList({ page: 1, size: 50 })
    noticeList.value = res.records || []
  } catch { noticeList.value = [] }
  finally { noticeLoading.value = false }
}

async function publishAnnouncement() {
  if (announceFormRef.value?.validate) {
    try { await announceFormRef.value.validate() } catch { return }
  }
  publishing.value = true
  try {
    await createNotice({
      title: announceForm.title.trim(),
      content: announceForm.content.trim(),
      pinned: announceForm.pinned,
      author: announceForm.author.trim() || '后勤管理处'
    })
    ElMessage.success('公告已发布')
    announceForm.title = ''
    announceForm.content = ''
    announceForm.pinned = false
    announceForm.author = ''
    await loadNoticeList()
  } catch (e) {
    ElMessage.error(e?.message || '发布失败')
  } finally { publishing.value = false }
}

async function deleteAnnouncement(id) {
  try {
    await ElMessageBox.confirm('确定删除该公告？', '提示', { type: 'warning' })
  } catch { return }
  try {
    await deleteNotice(id)
    ElMessage.success('公告已删除')
    await loadNoticeList()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

function loadFromStorage() {
  try {
    const s = localStorage.getItem(STORAGE_KEY)
    if (s) {
      const data = JSON.parse(s)
      form.systemName = data.systemName || '校园报修管理系统'
      form.logoUrl = data.logoUrl || ''
    }
  } catch {}
}

function onSave() {
  saving.value = true
  setTimeout(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        systemName: form.systemName,
        logoUrl: form.logoUrl
      }))
      ElMessage.success('保存成功')
    } catch {
      ElMessage.error('保存失败')
    } finally {
      saving.value = false
    }
  }, 300)
}

function beforeLogoUpload(file) {
  if (file.size / 1024 / 1024 >= 2) {
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
  try {
    if (localStorage.getItem(STORAGE_KEY)) localStorage.removeItem(STORAGE_KEY)
    form.systemName = '校园报修管理系统'
    form.logoUrl = ''
    ElMessage.success('缓存已清除')
  } catch {
    ElMessage.error('清除缓存失败')
  }
}

function handleDataBackup() {
  try {
    const backup = {
      exportTime: new Date().toISOString(),
      systemName: form.systemName,
      logoUrl: form.logoUrl
    }
    const blob = new Blob([JSON.stringify(backup, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `admin-settings-${Date.now()}.json`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('设置已导出为 JSON 文件')
  } catch {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  loadFromStorage()
  loadNoticeList()
})
</script>

<style scoped>
.admin-settings {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
}

.settings-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.settings-form :deep(.el-form-item) { margin-bottom: 0; }

.logo-uploader { cursor: pointer; }

.logo-img,
.logo-placeholder {
  width: 120px; height: 60px;
  border: 1px dashed var(--el-border-color);
  border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  background: var(--el-fill-color-lighter);
}
.logo-img { object-fit: contain; }
.logo-placeholder { flex-direction: column; gap: 4px; color: var(--el-text-color-placeholder); font-size: 12px; }

.announce-form :deep(.el-form-item) { margin-bottom: 16px; }

.notice-manage-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
}
.notice-manage-head__title { font-size: 14px; font-weight: 600; }

.notice-manage-item {
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  margin-bottom: 12px;
  background: var(--el-fill-color-lighter);
}
.notice-manage-item__head {
  display: flex; align-items: center; gap: 8px; margin-bottom: 8px;
}
.notice-manage-item__title { font-weight: 600; font-size: 14px; }
.notice-manage-item__time { margin-left: auto; font-size: 12px; color: var(--el-text-color-placeholder); white-space: nowrap; }
.notice-manage-item__content {
  font-size: 13px; color: var(--el-text-color-regular); margin: 0 0 8px 0; line-height: 1.6;
}
.notice-manage-item__meta {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 12px; color: var(--el-text-color-placeholder);
}

.data-maintenance { display: flex; gap: 12px; }
.save-bar { margin-top: 8px; display: flex; justify-content: flex-end; gap: 8px; }
</style>
