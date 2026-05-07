<template>
  <div class="message-page" v-loading="pageLoading">
    <div class="message-grid">
      <!-- 系统通知 -->
      <el-card shadow="never" class="msg-card">
        <template #header>
          <div class="msg-card__head">
            <span class="msg-card__title">系统通知</span>
            <el-badge v-if="sysUnread" :value="sysUnread" :max="99" />
            <el-button-group size="small" style="margin-left:auto">
              <el-button :type="!showRead ? 'primary' : 'default'" @click="showRead=false">未读</el-button>
              <el-button :type="showRead ? 'primary' : 'default'" @click="showRead=true">全部</el-button>
            </el-button-group>
          </div>
        </template>
        <div v-if="filteredSysItems.length" class="sys-list">
          <div
            v-for="it in filteredSysItems"
            :key="it.id"
            class="sys-item"
            :class="{ 'is-unread': !it.read }"
            @click="onSysClick(it)"
          >
            <div class="sys-item__icon">
              <el-icon v-if="it.type === 'CHAT'" :size="18"><ChatDotRound /></el-icon>
              <el-icon v-else :size="18"><Bell /></el-icon>
            </div>
            <div class="sys-item__body">
              <div class="sys-item__top">
                <span class="sys-item__title">{{ it.title }}</span>
                <span class="sys-item__time">{{ it.time }}</span>
              </div>
              <div class="sys-item__meta">
                <el-tag v-if="it.status != null" size="small" :type="notifyStatusTagType(it.status)" effect="plain">
                  {{ notifyStatusText(it.status) }}
                </el-tag>
                <span v-if="it.orderId" class="sys-item__oid">#{{ it.orderId }}</span>
              </div>
              <p v-if="it.summary" class="sys-item__summary">{{ it.summary }}</p>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无系统通知" :image-size="64" />
      </el-card>

      <!-- 工单沟通 -->
      <el-card shadow="never" class="msg-card">
        <template #header>
          <div class="msg-card__head">
            <span class="msg-card__title">工单沟通</span>
            <el-button v-if="convList.length" link type="primary" size="small" style="margin-left: auto" @click="reloadConversations">刷新</el-button>
          </div>
        </template>
        <div v-if="convList.length" class="conv-list">
          <div
            v-for="c in convList"
            :key="String(c.orderId) + '-' + String(c.id)"
            class="conv-item"
            @click="goOrderChat(c.orderId)"
          >
            <div class="conv-item__avatar">
              <el-icon :size="20"><ChatDotRound /></el-icon>
            </div>
            <div class="conv-item__body">
              <div class="conv-item__top">
                <span class="conv-item__name">工单 #{{ c.orderId }}</span>
                <div class="conv-item__right">
                  <span class="conv-item__time">{{ formatMsgTime(c.createTime) }}</span>
                  <span v-if="chatUnread(c.orderId) > 0" class="conv-badge">
                    {{ chatUnread(c.orderId) > 99 ? '99+' : chatUnread(c.orderId) }}
                  </span>
                </div>
              </div>
              <p class="conv-item__preview">
                <template v-if="c.senderName">
                  <strong>{{ c.senderName }}</strong>
                  <span class="conv-sep">:</span>
                </template>
                {{ c.content || (imageUrls(c.images).length ? '[图片]' : '暂无消息') }}
              </p>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无工单沟通记录" :image-size="64" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ChatDotRound, Bell } from '@element-plus/icons-vue'
import { getConversations, getUnreadMessages } from '@/api/chat'
import { useSystemNotifyStore } from '@/stores/systemNotify'
import { getRepairStatusLabel } from '@/constants/repairStatus'

defineOptions({ name: 'Message' })

const router = useRouter()
const route = useRoute()
const systemNotifyStore = useSystemNotifyStore()
const pageLoading = ref(true)
const convList = ref([])
const sysItems = computed(() => systemNotifyStore.items.slice(0, 30))
const sysUnread = computed(() => systemNotifyStore.unreadCount)
const showRead = ref(false)
const filteredSysItems = computed(() => showRead.value ? sysItems.value : sysItems.value.filter(it => !it.read))

let pollTimer = null

function onSysClick(it) {
  systemNotifyStore.markIdRead(it.id)
  if (it.orderId != null) {
    router.push(it.type === 'CHAT'
      ? { name: 'OrderChat', params: { orderId: String(it.orderId) } }
      : { name: 'RepairDetail', params: { id: String(it.orderId) } }
    )
  }
}

function markSysRead() { systemNotifyStore.markAllRead() }
function notifyStatusText(status) { return getRepairStatusLabel(Number(status)) }
function notifyStatusTagType(status) {
  const s = Number(status)
  if ([8, 7, 6].includes(s)) return 'success'
  if ([9, 10].includes(s)) return 'danger'
  if ([11, 3, 4, 12].includes(s)) return 'warning'
  if ([5].includes(s)) return ''
  return 'info'
}

function goOrderChat(orderId) {
  if (orderId == null) return
  systemNotifyStore.markChatOrderRead(orderId)
  router.push({ name: 'OrderChat', params: { orderId: String(orderId) } })
}

function chatUnread(orderId) { return systemNotifyStore.getChatUnreadByOrder(orderId) }

function imageUrls(str) {
  if (!str) return []
  try { const j = JSON.parse(str); return Array.isArray(j) ? j : [] }
  catch { return str.startsWith('http') ? [str] : [] }
}

function formatMsgTime(t) {
  if (!t) return ''
  if (typeof t === 'string') return t.slice(0, 16).replace('T', ' ')
  if (Array.isArray(t) && t.length >= 3) return `${t[0]}-${String(t[1]).padStart(2, '0')}-${String(t[2]).padStart(2, '0')}`
  return String(t)
}

async function reloadConversations() {
  try {
    const { records } = await getConversations(1, 50)
    convList.value = records || []
    const unread = await getUnreadMessages()
    systemNotifyStore.setChatUnreadFromMessages(unread || [])
  } catch { convList.value = [] }
}

onMounted(async () => {
  try { await reloadConversations() } finally { pageLoading.value = false }
  pollTimer = setInterval(() => {
    if (route.name === 'Message' || route.name === 'StaffMessage') reloadConversations()
  }, 20000)
})
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<style scoped>
.message-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  align-items: start;
}

.msg-card {
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.msg-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.msg-card__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

/* system notification items */
.sys-item {
  display: flex;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: background 0.15s;
}
.sys-item:last-child { border-bottom: none; }
.sys-item:hover { background: var(--el-fill-color-lighter); margin: 0 -12px; padding: 14px 12px; border-radius: 8px; }
.sys-item.is-unread { background: linear-gradient(90deg, rgba(64,158,255,0.06) 0%, transparent 100%); }

.sys-item__icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
}
.sys-item.is-unread .sys-item__icon {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.sys-item__body { flex: 1; min-width: 0; }

.sys-item__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 4px;
}

.sys-item__title {
  font-size: 14px;
  color: var(--el-text-color-primary);
  font-weight: 500;
  line-height: 1.4;
}
.sys-item.is-unread .sys-item__title { font-weight: 700; }

.sys-item__time {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  flex-shrink: 0;
  margin-top: 2px;
}

.sys-item__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.sys-item__oid {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  font-family: 'SF Mono', 'Cascadia Code', monospace;
}

.sys-item__summary {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* conversation items */
.conv-item {
  display: flex;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: background 0.15s;
}
.conv-item:last-child { border-bottom: none; }
.conv-item:hover { background: var(--el-fill-color-lighter); margin: 0 -12px; padding: 14px 12px; border-radius: 8px; }

.conv-item__avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.conv-item__body { flex: 1; min-width: 0; }

.conv-item__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  gap: 8px;
}

.conv-item__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.conv-item__right { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }

.conv-item__time {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.conv-badge {
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

.conv-item__preview {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.conv-item__preview strong {
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.conv-sep {
  margin: 0 2px;
  color: var(--el-text-color-placeholder);
}
</style>
