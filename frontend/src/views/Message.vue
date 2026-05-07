<template>
  <div
    class="message-page"
    :class="{ 'message-page--staff': isStaff }"
    v-loading="pageLoading"
  >
    <div v-if="sysItems.length" class="message-sys">
      <div class="message-sys__bar">
        <span class="message-sys__title">系统通知</span>
        <el-button v-if="sysUnread" link type="primary" size="small" @click="markSysRead">标为已读</el-button>
      </div>
      <ul class="message-sys__list">
        <li
          v-for="it in sysItems"
          :key="it.id"
          class="message-sys__item"
          :class="{ 'is-unread': !it.read }"
          @click="onSysClick(it)"
        >
          <div class="message-sys__row">
            <span class="message-sys__name">
              <span v-if="!it.read" class="message-sys__dot"></span>
              {{ it.title }}
            </span>
            <span class="message-sys__time">{{ it.time }}</span>
          </div>
          <div class="message-sys__meta">
            <span v-if="it.orderId" class="message-sys__oid">工单 #{{ it.orderId }}</span>
            <el-tag v-if="it.status != null" size="small" :type="notifyStatusTagType(it.status)">
              {{ notifyStatusText(it.status) }}
            </el-tag>
          </div>
          <p class="message-sys__summary">{{ it.summary }}</p>
        </li>
      </ul>
    </div>

    <div class="message-section__head">
      <span class="message-section__title">工单沟通</span>
      <el-button v-if="convList.length" link type="primary" size="small" @click="reloadConversations">刷新</el-button>
    </div>
    <ul v-if="convList.length" class="message-list">
      <li
        v-for="c in convList"
        :key="String(c.orderId) + '-' + String(c.id)"
        class="message-item"
        @click="goOrderChat(c.orderId)"
      >
        <div class="message-item__avatar order-avatar">
          <el-icon :size="26"><ChatDotRound /></el-icon>
        </div>
        <div class="message-item__body">
          <div class="message-item__row">
            <span class="message-item__name">工单 #{{ c.orderId }}</span>
            <div class="message-item__right">
              <span class="message-item__time">{{ formatMsgTime(c.createTime) }}</span>
              <span v-if="chatUnread(c.orderId) > 0" class="chat-unread-badge">
                {{ chatUnread(c.orderId) > 99 ? '99+' : chatUnread(c.orderId) }}
              </span>
            </div>
          </div>
          <p class="message-item__summary">
            <span v-if="c.senderName" class="sender-tag">{{ c.senderName }}：</span
            >{{ c.content || (imageUrls(c.images).length ? '[图片]' : '') }}
          </p>
        </div>
      </li>
    </ul>
    <el-empty v-else :description="emptyHint" :image-size="90" class="message-empty" />

    <p class="message-legacy-hint" :class="{ 'is-staff': isStaff }">
      说明：与报修人/维修工的实时聊天在管理员派单后可用；从工单详情的「联系」进入也可发起沟通。
    </p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ChatDotRound } from '@element-plus/icons-vue'
import { getConversations, getUnreadMessages } from '@/api/chat'
import { useSystemNotifyStore } from '@/stores/systemNotify'
import { getRepairStatusLabel } from '@/constants/repairStatus'

defineOptions({ name: 'Message' })

const router = useRouter()
const route = useRoute()
const systemNotifyStore = useSystemNotifyStore()
const isStaff = computed(() => route.path.startsWith('/staff'))
const pageLoading = ref(true)
const convList = ref([])
const sysItems = computed(() => systemNotifyStore.items.slice(0, 30))
const sysUnread = computed(() => systemNotifyStore.unreadCount)

const emptyHint = '暂无与工单相关的会话，派单后可在工单中联系对方'

let pollTimer = null

function onSysClick(it) {
  systemNotifyStore.markIdRead(it.id)
  if (it.orderId != null) {
    if (it.type === 'CHAT') {
      router.push({ name: 'OrderChat', params: { orderId: String(it.orderId) } })
      return
    }
    router.push({ name: 'RepairDetail', params: { id: String(it.orderId) } })
  }
}

function markSysRead() {
  systemNotifyStore.markAllRead()
}

function notifyStatusText(status) {
  return getRepairStatusLabel(Number(status))
}

function notifyStatusTagType(status) {
  const s = Number(status)
  if ([8, 7, 6].includes(s)) return 'success'
  if ([9, 10].includes(s)) return 'danger'
  if ([11, 3, 4, 12].includes(s)) return 'warning'
  if ([5].includes(s)) return 'primary'
  return 'info'
}

function goOrderChat(orderId) {
  if (orderId == null) {
    return
  }
  systemNotifyStore.markChatOrderRead(orderId)
  router.push({ name: 'OrderChat', params: { orderId: String(orderId) } })
}

function chatUnread(orderId) {
  return systemNotifyStore.getChatUnreadByOrder(orderId)
}

function imageUrls(str) {
  if (!str) {
    return []
  }
  try {
    const j = JSON.parse(str)
    return Array.isArray(j) ? j : []
  } catch {
    return str.startsWith('http') ? [str] : []
  }
}

function formatMsgTime(t) {
  if (!t) {
    return ''
  }
  if (typeof t === 'string') {
    return t.slice(0, 16).replace('T', ' ')
  }
  if (Array.isArray(t) && t.length >= 3) {
    return `${t[0]}-${String(t[1]).padStart(2, '0')}-${String(t[2]).padStart(2, '0')}`
  }
  return String(t)
}

async function reloadConversations() {
  try {
    const { records } = await getConversations(1, 50)
    convList.value = records || []
    const unread = await getUnreadMessages()
    systemNotifyStore.setChatUnreadFromMessages(unread || [])
  } catch {
    convList.value = []
  }
}

onMounted(async () => {
  try {
    await reloadConversations()
  } finally {
    pageLoading.value = false
  }
  pollTimer = setInterval(() => {
    if (route.name === 'Message' || route.name === 'StaffMessage') {
      reloadConversations()
    }
  }, 20000)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
  }
})
</script>

<style scoped>
.message-page {
  min-height: 100vh;
  background: var(--el-bg-color);
  padding-bottom: 32px;
}
.message-page--staff {
  background: linear-gradient(180deg, #f0fdfa 0%, #f8fafc 40%, #f1f5f9 100%);
}
.message-page--staff .message-sys {
  background: rgba(255, 255, 255, 0.75);
  border-bottom: 8px solid rgba(13, 148, 136, 0.06);
  border-radius: 0 0 16px 16px;
}
.message-page--staff .message-section__title {
  color: #0f172a;
}
.message-page--staff .order-avatar {
  background: linear-gradient(145deg, #ccfbf1, #f0fdfa);
  color: #0d9488;
  border: 1px solid rgba(13, 148, 136, 0.15);
}
.message-page--staff .sender-tag {
  color: #0d9488;
}
.message-legacy-hint.is-staff {
  color: #64748b;
  background: rgba(255, 255, 255, 0.6);
  margin: 12px 16px 20px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(13, 148, 136, 0.1);
}
.message-sys {
  background: var(--el-bg-color);
  border-bottom: 8px solid var(--el-fill-color-lighter);
}
.message-sys__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 8px;
}
.message-sys__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.message-sys__list {
  list-style: none;
  margin: 0;
  padding: 0 0 8px;
}
.message-sys__item {
  padding: 10px 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: all 0.2s ease;
}
.message-sys__item:hover {
  background: #f8fafc;
}
.message-sys__item.is-unread {
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.08), rgba(99, 102, 241, 0.04));
}
.message-sys__item.is-unread .message-sys__name {
  font-weight: 700;
}
.message-sys__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.message-sys__name {
  font-size: 14px;
  color: var(--el-text-color-primary);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.message-sys__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ef4444;
  display: inline-block;
}
.message-sys__time {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  flex-shrink: 0;
}
.message-sys__meta {
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.message-sys__oid {
  font-size: 12px;
  color: #334155;
  font-weight: 600;
}
.message-sys__summary {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.message-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 4px;
}
.message-section__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.message-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.message-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
}
.message-item:hover {
  background: var(--el-fill-color-lighter);
}
.message-item__avatar {
  flex-shrink: 0;
}
.order-avatar {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}
.message-item__body {
  flex: 1;
  min-width: 0;
}
.message-item__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  gap: 8px;
}
.message-item__name {
  font-size: 15px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.message-item__time {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  flex-shrink: 0;
}
.message-item__right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.chat-unread-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 10px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
  font-weight: 600;
}
.message-item__summary {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.45;
}
.sender-tag {
  color: var(--el-color-primary);
  font-weight: 500;
}
.message-empty {
  padding: 8px 0 0;
}
.message-legacy-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  padding: 0 20px 24px;
  line-height: 1.5;
  margin: 12px 0 0;
}
</style>
