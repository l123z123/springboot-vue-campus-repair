<template>
  <div class="repair-page">
    <!-- Top Bar -->
    <header class="top-bar">
      <div class="top-bar__search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索报修单、设备..."
          clearable
          class="search-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="top-bar__actions">
        <el-button type="primary" link class="top-bar__btn" @click="handleScan">
          <el-icon><Camera /></el-icon>
          <span>扫码报修</span>
        </el-button>
        <el-button type="primary" link class="top-bar__btn" @click="handleMessage">
          <el-icon><ChatDotRound /></el-icon>
          <span>消息中心</span>
        </el-button>
      </div>
    </header>

    <!-- Tab Nav -->
    <nav class="tab-nav">
      <div
        v-for="tab in categoryTabs"
        :key="tab.value"
        class="tab-nav__item"
        :class="{ 'is-active': activeTab === tab.value }"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </div>
    </nav>

    <main class="main-content">
      <!-- Section 1: 快捷入口 (横向滚动) -->
      <section class="section quick-entry">
        <div class="quick-entry__scroll">
          <div
            v-for="entry in quickEntries"
            :key="entry.key"
            class="quick-entry__item"
            @click="entry.click"
          >
            <div class="quick-entry__icon">
              <el-icon><component :is="entry.icon" /></el-icon>
            </div>
            <span class="quick-entry__label">{{ entry.label }}</span>
          </div>
        </div>
      </section>

      <!-- Section 2: 核心操作区 -->
      <section class="section core-section">
        <!-- 2.1 提交报修照片 -->
        <div class="submit-card">
          <div class="submit-card__header">
            <el-icon class="submit-card__icon"><Camera /></el-icon>
            <span>提交报修照片</span>
          </div>
          <p class="submit-card__hint">点击上传或拖拽文件到此处</p>
          <div class="submit-card__upload">
            <ImageUpload
              ref="imageUploadRef"
              :token="token"
              :upload-url="uploadUrl"
              @success="onUploadSuccess"
            />
          </div>
        </div>

        <!-- 2.2 填写报修信息 -->
        <div class="form-card">
          <div class="form-card__header">
            <el-icon class="form-card__icon"><EditPen /></el-icon>
            <span>填写报修信息</span>
          </div>
          <el-form :model="form" label-position="top" class="repair-form">
            <el-form-item label="地点">
              <el-select
                v-model="form.location"
                placeholder="请选择报修地点"
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="opt in locationOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="故障描述">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="3"
                placeholder="请简要描述故障现象（如：投影仪无法开机、水管漏水等）"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="紧急程度">
              <el-radio-group v-model="form.urgency">
                <el-radio value="高">高</el-radio>
                <el-radio value="中">中</el-radio>
                <el-radio value="低">低</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </div>

        <!-- 2.3 预览区由 ImageUpload 内部展示（上传区下方） -->

        <!-- 2.4 立即提交报修 -->
        <div class="submit-actions">
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitLoading"
            @click="handleSubmitRepair"
          >
            立即提交报修
          </el-button>
        </div>
      </section>

      <!-- Section 3: 我的报修记录 (订单风格 + 折叠) -->
      <section class="section records-section">
        <el-collapse v-model="recordsCollapseActive">
          <el-collapse-item name="records">
            <template #title>
              <span class="records-title">
                <el-icon><Box /></el-icon>
                我的报修记录
              </span>
            </template>
            <ul class="record-list">
              <li
                v-for="item in recordList"
                :key="item.id"
                class="record-item"
              >
                <div class="record-item__thumb">
                  <img
                    v-if="item.thumbUrl"
                    :src="item.thumbUrl"
                    :alt="item.fileName"
                    class="record-item__img"
                  >
                  <el-icon v-else class="record-item__placeholder"><Picture /></el-icon>
                </div>
                <div class="record-item__info">
                  <div class="record-item__name">{{ item.fileName }}</div>
                  <div class="record-item__meta">
                    状态：<el-tag :type="statusTagType(item.status)" size="small">{{ item.status }}</el-tag>
                  </div>
                </div>
                <div class="record-item__op">
                  <el-dropdown trigger="click" @command="(cmd) => handleRecordCommand(cmd, item)">
                    <el-button type="primary" link size="small" class="op-btn">
                      <el-icon><Operation /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="download">下载</el-dropdown-item>
                        <el-dropdown-item command="delete" divided>
                          <span class="dropdown-item--danger">删除</span>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </li>
            </ul>
          </el-collapse-item>
        </el-collapse>
      </section>

      <!-- 底部占位，避免被固定导航遮挡 -->
      <div class="bottom-spacer" />
    </main>

    <!-- Bottom Nav (固定底部) -->
    <nav class="bottom-nav">
      <div class="bottom-nav__item" @click="goHome">
        <el-icon><HomeFilled /></el-icon>
        <span>首页</span>
      </div>
      <div class="bottom-nav__item is-active">
        <el-icon><UploadFilled /></el-icon>
        <span>发布</span>
      </div>
      <div class="bottom-nav__item" @click="handleMessage">
        <el-icon><ChatDotRound /></el-icon>
        <span>消息</span>
      </div>
      <div class="bottom-nav__item" @click="goMine">
        <el-icon><User /></el-icon>
        <span>我的</span>
      </div>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Camera,
  ChatDotRound,
  Lightning,
  Calendar,
  DataLine,
  Box,
  Picture,
  Operation,
  HomeFilled,
  UploadFilled,
  User,
  EditPen
} from '@element-plus/icons-vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const token = computed(() => userStore.token || localStorage.getItem('token') || '')

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const uploadUrl = computed(() => {
  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '') + '/file/upload'
  }
  return `${window.location.origin}${baseURL}/file/upload`
})

const searchKeyword = ref('')
const activeTab = ref('全部')

const categoryTabs = [
  { label: '全部', value: '全部' },
  { label: '教室', value: '教室' },
  { label: '机房', value: '机房' },
  { label: '宿舍', value: '宿舍' },
  { label: '公共区域', value: '公共区域' }
]

const quickEntries = [
  { key: 'urgent', label: '急修通道', icon: Lightning, click: () => ElMessage.info('急修通道') },
  { key: 'book', label: '预约维修', icon: Calendar, click: () => ElMessage.info('预约维修') },
  { key: 'progress', label: '查看进度', icon: DataLine, click: () => ElMessage.info('查看进度') },
  { key: 'history', label: '历史工单', icon: Box, click: () => router.push('/repair-list') }
]

const imageUploadRef = ref(null)

const locationOptions = [
  { label: '教室', value: '教室' },
  { label: '机房', value: '机房' },
  { label: '宿舍', value: '宿舍' },
  { label: '公共区域', value: '公共区域' },
  { label: '其他', value: '其他' }
]

const form = ref({
  location: '',
  description: '',
  urgency: '中'
})

const submitLoading = ref(false)

function handleSubmitRepair() {
  const photoUrl = imageUploadRef.value?.imageUrl?.value ?? ''
  const { location, description, urgency } = form.value
  if (!photoUrl && !(description && description.trim())) {
    ElMessage.warning('请至少上传一张报修照片或填写故障描述')
    return
  }
  submitLoading.value = true
  setTimeout(() => {
    submitLoading.value = false
    ElMessage.success('报修单已提交，我们会尽快处理')
    form.value = { location: '', description: '', urgency: '中' }
    if (imageUploadRef.value?.clearPreview) {
      imageUploadRef.value.clearPreview()
    }
  }, 600)
}

const recordsCollapseActive = ref([])

// 我的报修记录 Mock（订单风格：缩略图 + 文件名 + 状态）
const RECORD_MOCK = [
  { id: 1, fileName: '教室A302_投影仪.jpg', status: '处理中', thumbUrl: '', downloadUrl: '' },
  { id: 2, fileName: '宿舍B501_水管.png', status: '已完成', thumbUrl: '', downloadUrl: '' },
  { id: 3, fileName: '机房C201_电脑.pdf', status: '待处理', thumbUrl: '', downloadUrl: '' },
  { id: 4, fileName: '报修照片_宿舍楼A.jpg', status: '处理中', thumbUrl: '', downloadUrl: '' },
  { id: 5, fileName: '维修单_20260308.pdf', status: '已完成', thumbUrl: '', downloadUrl: '' }
]

const recordList = ref([...RECORD_MOCK])

function statusTagType(status) {
  const map = { 待处理: 'info', 处理中: 'warning', 已完成: 'success' }
  return map[status] || 'info'
}

function handleScan() {
  ElMessage.info('扫码报修（可接入扫码能力）')
}

function handleMessage() {
  ElMessage.info('消息中心')
}

function goHome() {
  router.push('/upload')
}

function goMine() {
  ElMessage.info('我的（可跳转个人中心）')
}

function onUploadSuccess() {
  ElMessage.success('报修照片已提交，可在「我的报修记录」中查看')
}

function handleRecordCommand(command, row) {
  if (command === 'download') {
    handleRecordDownload(row)
  } else if (command === 'delete') {
    handleRecordDelete(row)
  }
}

async function handleRecordDownload(row) {
  const url = row.downloadUrl
  if (!url) {
    ElMessage.info('Mock 数据无下载链接')
    return
  }
  try {
    const res = await fetch(url, { credentials: 'include' })
    if (!res.ok) throw new Error(res.statusText)
    const blob = await res.blob()
    const blobUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = row.fileName
    a.style.display = 'none'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(blobUrl)
    ElMessage.success(`${row.fileName} 已保存到本地`)
  } catch (e) {
    ElMessage.error('下载失败：' + (e?.message || '未知错误'))
  }
}

function handleRecordDelete(row) {
  ElMessageBox.confirm('确定要删除该记录吗？此操作不可恢复。', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      recordList.value = recordList.value.filter((item) => item.id !== row.id)
      ElMessage.success('已删除')
    })
    .catch(() => {})
}
</script>

<style scoped>
.repair-page {
  min-height: 100vh;
  padding-bottom: 64px;
  background: var(--el-fill-color-lighter);
}

/* Top Bar */
.top-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.top-bar__search {
  flex: 1;
  min-width: 0;
}
.search-input {
  width: 100%;
}
.top-bar__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.top-bar__btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
}

/* Tab Nav */
.tab-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
.tab-nav::-webkit-scrollbar {
  display: none;
}
.tab-nav__item {
  flex-shrink: 0;
  padding: 6px 14px;
  font-size: 14px;
  color: var(--el-text-color-regular);
  border-radius: 20px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.tab-nav__item:hover {
  background: var(--el-fill-color);
  color: var(--el-color-primary);
}
.tab-nav__item.is-active {
  background: var(--el-color-primary);
  color: #fff;
}

/* Main */
.main-content {
  padding: 16px;
}

/* Section 1: 快捷入口 横向滚动 */
.quick-entry {
  margin-bottom: 16px;
}
.quick-entry__scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding: 4px 0;
  -webkit-overflow-scrolling: touch;
}
.quick-entry__scroll::-webkit-scrollbar {
  height: 4px;
}
.quick-entry__item {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 72px;
  padding: 12px 8px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.quick-entry__item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.quick-entry__icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border-radius: 10px;
  font-size: 20px;
}
.quick-entry__label {
  font-size: 12px;
  color: var(--el-text-color-regular);
  text-align: center;
  white-space: nowrap;
}

/* Section 2: 提交报修卡片 */
.core-section {
  margin-bottom: 16px;
}
.submit-card {
  padding: 24px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.submit-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}
.submit-card__icon {
  font-size: 22px;
  color: var(--el-color-primary);
}
.submit-card__hint {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}
.submit-card__upload :deep(.image-upload) {
  max-width: 100%;
}

/* 填写报修信息卡片 */
.form-card {
  margin-top: 16px;
  padding: 24px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.form-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 16px;
}
.form-card__icon {
  font-size: 22px;
  color: var(--el-color-primary);
}
.repair-form :deep(.el-form-item) {
  margin-bottom: 16px;
}
.repair-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

/* 立即提交报修按钮 */
.submit-actions {
  margin-top: 24px;
  text-align: center;
}
.submit-btn {
  min-width: 200px;
}

/* Section 3: 我的报修记录 */
.records-section :deep(.el-collapse) {
  border: none;
}
.records-section :deep(.el-collapse-item__header) {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  padding: 0 16px;
  height: 52px;
  font-size: 15px;
  font-weight: 500;
  background: var(--el-bg-color);
}
.records-section :deep(.el-collapse-item__wrap) {
  border: 1px solid var(--el-border-color-lighter);
  border-top: none;
  border-radius: 0 0 12px 12px;
}
.records-section :deep(.el-collapse-item__content) {
  padding: 0;
  background: var(--el-bg-color);
}
.records-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.records-title .el-icon {
  font-size: 18px;
  color: var(--el-text-color-secondary);
}

.record-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.record-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.record-item:last-child {
  border-bottom: none;
}
.record-item__thumb {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-fill-color);
  display: flex;
  align-items: center;
  justify-content: center;
}
.record-item__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.record-item__placeholder {
  font-size: 24px;
  color: var(--el-text-color-placeholder);
}
.record-item__info {
  flex: 1;
  min-width: 0;
}
.record-item__name {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.record-item__meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.record-item__op {
  flex-shrink: 0;
}
.op-btn {
  padding: 4px;
}
.dropdown-item--danger {
  color: var(--el-color-danger);
}

.bottom-spacer {
  height: 24px;
}

/* Bottom Nav 固定底部 */
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 56px;
  padding-bottom: env(safe-area-inset-bottom, 0);
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color-lighter);
  z-index: 100;
}
.bottom-nav__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: color 0.2s;
}
.bottom-nav__item:hover,
.bottom-nav__item.is-active {
  color: var(--el-color-primary);
}
.bottom-nav__item .el-icon {
  font-size: 22px;
}
</style>
