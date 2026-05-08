<template>
  <div class="order-chat">
    <div class="order-chat__header">
      <div>
        <h3 class="order-chat__title">{{ headTitle }}</h3>
        <p v-if="orderLocation" class="order-chat__sub">{{ orderLocation }}</p>
      </div>
    </div>

    <el-alert
      v-if="role === 2"
      type="warning"
      :closable="false"
      show-icon
      class="order-chat__admin-hint"
      title="管理员仅可查看本工单沟通记录，不参与聊天。"
    />

    <div ref="listRef" class="order-chat__list" v-loading="loading" :class="{ 'is-readonly': !canWrite }">
      <div v-for="m in messages" :key="m.id" class="oc-msg" :class="{ 'is-mine': isMine(m) }">
        <div class="oc-msg__meta">
          <span class="oc-msg__who">{{ m.senderName || '用户' }}</span>
          <span class="oc-msg__time">{{ formatTime(m.createTime) }}</span>
        </div>
        <div class="oc-msg__bubble">
          <p v-if="m.content" class="oc-msg__text">{{ m.content }}</p>
          <div v-if="imageUrls(m.images).length" class="oc-msg__imgs">
            <el-image
              v-for="(u, i) in imageUrls(m.images)"
              :key="i"
              :src="u"
              :preview-src-list="imageUrls(m.images)"
              :initial-index="i"
              fit="cover"
              class="oc-msg__img"
            />
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !messages.length" description="尚无消息，打个招呼或上传现场图" :image-size="64" />
    </div>

    <div v-if="canWrite" class="order-chat__input">
      <div v-if="pendingImgs.length" class="oc-pend">
        <el-image v-for="(u, i) in pendingImgs" :key="i" :src="u" class="oc-pend__thumb" />
        <el-button type="danger" link size="small" @click="pendingImgs = []">清除图片</el-button>
      </div>
      <div class="oc-input-row">
        <el-upload :show-file-list="false" accept="image/jpeg,image/png,image/jpg" :before-upload="onPickImage">
          <el-button :icon="Picture" circle text />
        </el-upload>
        <el-input
          v-model="text"
          type="textarea"
          :rows="2"
          placeholder="与对方沟通（文字或图片，至少一项）"
          maxlength="500"
          show-word-limit
          @keydown.enter.exact.prevent="send"
        />
        <el-button type="primary" :disabled="!canSend" :loading="sending" @click="send">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { getRepairDetail } from '@/api/repair'
import { getChatList, sendChatMessage, uploadChatFile, markOrderRead } from '@/api/chat'
import { useUserStore } from '@/stores/user'
import { useSystemNotifyStore } from '@/stores/systemNotify'
import { normalizeImageUrl } from '@/utils/image'

defineOptions({ name: 'OrderChat' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const systemNotifyStore = useSystemNotifyStore()

const orderId = computed(() => String(route.params.orderId || ''))
const listRef = ref(null)
const loading = ref(true)
const sending = ref(false)
const messages = ref([])
const text = ref('')
const pendingImgs = ref([])

const orderLocation = ref('')
const headTitle = ref('沟通')

const selfId = computed(() => String(userStore.userInfo?.userId ?? ''))
const role = computed(() => (userStore.userInfo?.role != null ? userStore.userInfo.role : 0))
const isStaff = computed(() => role.value === 1)
const canWrite = computed(() => role.value === 0 || role.value === 1)

let pollTimer = null
let onChatEv = null

const canSend = computed(
  () => (text.value.trim().length > 0 || pendingImgs.value.length > 0) && !sending.value
)

function isMine(m) {
  return m.senderId != null && String(m.senderId) === selfId.value
}

function imageUrls(str) {
  if (!str) return []
  try {
    const j = JSON.parse(str)
    if (Array.isArray(j)) return j.map((u) => normalizeImageUrl(String(u)))
  } catch {
    if (str.startsWith('http')) return [normalizeImageUrl(str)]
  }
  return []
}

function formatTime(t) {
  if (!t) return ''
  if (typeof t === 'string') return t.slice(0, 16).replace('T', ' ')
  if (Array.isArray(t) && t.length >= 5) {
    const [y, m, d, h = 0, mm = 0, s = 0] = t
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(mm).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return String(t)
}

function scrollToBottom() {
  nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function loadOrder() {
  const id = orderId.value
  if (!id) return
  try {
    const o = await getRepairDetail(id)
    orderLocation.value = o?.location || ''
    if (isStaff.value) {
      const nm = o?.reporterName && o.reporterName !== '-' ? o.reporterName : '报修人'
      headTitle.value = `与 ${nm} 沟通`
    } else {
      headTitle.value = o?.repairmanId ? '与维修工沟通' : '沟通'
    }
  } catch {
    headTitle.value = '沟通'
  }
}

async function loadMessages() {
  const id = orderId.value
  if (!id) return
  try {
    const { records } = await getChatList(id, 1, 500)
    messages.value = records || []
    systemNotifyStore.markChatOrderRead(id)
    markOrderRead(id).catch(() => {})
    scrollToBottom()
  } catch (e) {
    console.warn('load messages', e)
  } finally {
    loading.value = false
  }
}

async function onPickImage(file) {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('单张图不超过 5MB')
    return false
  }
  try {
    const url = await uploadChatFile(file)
    if (url) pendingImgs.value = [...pendingImgs.value, url]
  } catch { /* request handled */ }
  return false
}

async function send() {
  if (!canSend.value) return
  const id = orderId.value
  if (!id) return
  const c = text.value.trim()
  const imgs = pendingImgs.value.length ? JSON.stringify(pendingImgs.value) : undefined
  if (!c && !imgs) return
  sending.value = true
  try {
    await sendChatMessage({ orderId: id, content: c, images: imgs })
    text.value = ''
    pendingImgs.value = []
    await loadMessages()
  } catch (e) {
    /* handled */
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  if (!userStore.token) {
    ElMessage.error('请先登录')
    router.push('/login')
    return
  }
  loadOrder()
  loadMessages()
  pollTimer = setInterval(() => {
    if (document.hidden) return
    loadMessages()
  }, 15000)

  onChatEv = (ev) => {
    if (String(ev?.detail?.orderId) === orderId.value) loadMessages()
  }
  window.addEventListener('repair-order-chat', onChatEv)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (onChatEv) window.removeEventListener('repair-order-chat', onChatEv)
})

watch(orderId, (v) => {
  if (v) { loadOrder(); loadMessages() }
})
</script>

<style scoped>
.order-chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 180px);
}

.order-chat__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.order-chat__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.order-chat__sub {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.order-chat__admin-hint {
  margin-bottom: 12px;
}

.order-chat__list {
  flex: 1;
  overflow: auto;
  padding: 0 4px 16px;
}
.order-chat__list.is-readonly {
  padding-bottom: 0;
}

.oc-msg {
  margin-bottom: 14px;
  max-width: 560px;
}
.oc-msg.is-mine {
  margin-left: auto;
  text-align: right;
}
.oc-msg.is-mine .oc-msg__bubble {
  background: var(--el-color-primary-light-8);
}

.oc-msg__meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
  display: flex;
  gap: 8px;
}
.is-mine .oc-msg__meta {
  flex-direction: row-reverse;
}

.oc-msg__bubble {
  display: inline-block;
  text-align: left;
  background: var(--el-fill-color-light);
  padding: 10px 14px;
  border-radius: 12px;
  max-width: 100%;
  word-break: break-word;
}

.oc-msg__text {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  color: var(--el-text-color-primary);
}

.oc-msg__imgs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.oc-msg__img {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
}

.order-chat__input {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.oc-pend {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.oc-pend__thumb {
  width: 56px;
  height: 56px;
  border-radius: 6px;
}

.oc-input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}
</style>
