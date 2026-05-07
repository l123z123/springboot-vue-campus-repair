<template>
  <div class="chat-page">
    <header class="chat-header">
      <el-button type="primary" link class="chat-header__back" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="chat-header__title">{{ thread?.user || '聊天' }}</span>
    </header>
    <!-- 工单卡片 -->
    <div v-if="thread?.relatedOrderId" class="chat-order-card">
      <div class="chat-order-card__thumb">
        <el-image v-if="order.image" :src="order.image" fit="cover" />
        <el-icon v-else class="chat-order-card__placeholder"><Picture /></el-icon>
      </div>
      <div class="chat-order-card__info">
        <div class="chat-order-card__location">{{ order.location }}</div>
        <el-tag :type="statusTagType(order.status)" size="small">{{ order.statusText }}</el-tag>
      </div>
    </div>

    <!-- 聊天记录 -->
    <div class="chat-list" ref="chatListRef">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="chat-msg"
        :class="{ 'is-self': msg.self }"
      >
        <div v-if="msg.type === 'image' && msg.url" class="chat-msg__bubble chat-msg__bubble--img">
          <el-image :src="normalizeImageUrl(msg.url)" fit="cover" :preview-src-list="[normalizeImageUrl(msg.url)]" />
        </div>
        <div v-else class="chat-msg__bubble">{{ msg.text }}</div>
        <div class="chat-msg__meta">{{ msg.time }}</div>
      </div>
    </div>

    <!-- 底部输入 -->
    <div class="chat-footer">
      <input ref="fileInputRef" type="file" accept="image/*" class="chat-footer__file" @change="onImageSelect" />
      <el-button type="primary" link class="chat-footer__btn" :disabled="uploadLoading" @click="triggerImage">
        <el-icon v-if="!uploadLoading"><Picture /></el-icon>
        <el-icon v-else class="is-loading"><Loading /></el-icon>
        <span>图片</span>
      </el-button>
      <el-input
        v-model="inputText"
        placeholder="输入消息..."
        class="chat-footer__input"
        @keyup.enter="send"
      />
      <el-button type="primary" class="chat-footer__send" @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Picture, ArrowLeft, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useChatStore } from '@/stores/chat'
import request from '@/utils/request'
import { normalizeImageUrl, extractImageUrl, MAX_IMAGE_SIZE } from '@/utils/image'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const chatListRef = ref(null)
const fileInputRef = ref(null)
const inputText = ref('')
const uploadLoading = ref(false)

const threadId = computed(() => route.params.id)
const thread = computed(() => chatStore.getThread(threadId.value))
const messages = computed(() => thread.value?.messages || [])

const order = computed(() => ({
  location: '教学楼 A-302',
  status: 'processing',
  statusText: '处理中',
  image: ''
}))


function statusTagType(s) {
  const map = { pending: 'info', processing: 'warning', done: 'success' }
  return map[s] || 'info'
}

function formatTime() {
  return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function send() {
  const t = inputText.value?.trim()
  if (!t || !threadId.value) return
  chatStore.addMessage(threadId.value, { text: t, time: formatTime(), self: true })
  inputText.value = ''
  scrollToBottom()
}

function triggerImage() {
  fileInputRef.value?.click()
}

async function onImageSelect(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file || !file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > MAX_IMAGE_SIZE) {
    ElMessage.error('图片不能超过 5MB')
    return
  }
  const fd = new FormData()
  fd.append('file', file)
  uploadLoading.value = true
  try {
    const res = await request.post('/file/upload', fd)
    const url = extractImageUrl(res)
    if (url && threadId.value) {
      chatStore.addMessage(threadId.value, { text: '', time: formatTime(), self: true, type: 'image', url })
      scrollToBottom()
    } else {
      ElMessage.error('上传失败')
    }
  } catch (err) {
    ElMessage.error('上传失败：' + (err?.message || '未知错误'))
  } finally {
    uploadLoading.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (chatListRef.value) chatListRef.value.scrollTop = chatListRef.value.scrollHeight
  })
}

watch(messages, () => scrollToBottom(), { deep: true })
onMounted(() => scrollToBottom())
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding-bottom: 60px;
  background: var(--el-fill-color-lighter);
}
.chat-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.chat-header__back {
  margin-right: 8px;
}
.chat-header__title {
  font-size: 16px;
  font-weight: 500;
}
.chat-order-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.chat-order-card__thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-fill-color);
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-order-card__thumb .el-image {
  width: 100%;
  height: 100%;
}
.chat-order-card__placeholder {
  font-size: 24px;
  color: var(--el-text-color-placeholder);
}
.chat-order-card__info {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chat-order-card__location {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
.chat-msg {
  margin-bottom: 12px;
}
.chat-msg.is-self {
  text-align: right;
}
.chat-msg__bubble {
  display: inline-block;
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
}
.chat-msg__bubble--img :deep(.el-image) {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  vertical-align: bottom;
}
.chat-msg.is-self .chat-msg__bubble {
  background: var(--el-color-primary);
  color: #fff;
  border-color: var(--el-color-primary);
}
.chat-msg.is-self .chat-msg__bubble--img {
  background: transparent;
  padding: 2px;
  border: none;
}
.chat-msg__meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.chat-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom, 0));
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color-lighter);
  z-index: 100;
}
.chat-footer__file {
  display: none;
}
.chat-footer__btn {
  flex-shrink: 0;
}
.chat-footer__input {
  flex: 1;
  min-width: 0;
}
.chat-footer__send {
  flex-shrink: 0;
}
</style>
