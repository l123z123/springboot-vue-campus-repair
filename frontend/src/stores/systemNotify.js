import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const MAX = 100

/**
 * 工单类 WebSocket 推文的内存列表，供消息页/管理端铃铛查看（不落库，与毕设“本地可演示”一致）
 */
export const useSystemNotifyStore = defineStore('systemNotify', () => {
  const items = ref([])
  const chatUnreadByOrder = ref({})

  const unreadCount = computed(() => items.value.filter((x) => !x.read).length)
  const chatUnreadTotal = computed(() =>
    Object.values(chatUnreadByOrder.value).reduce((sum, n) => sum + Number(n || 0), 0)
  )

  function add(record) {
    const id = `sn-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
    const normalizedOrderId =
      record?.orderId != null && record.orderId !== '' ? String(record.orderId) : null
    const orderIds = Array.isArray(record?.orderIds)
      ? record.orderIds.map((x) => (x != null && x !== '' ? String(x) : null)).filter(Boolean)
      : []
    const row = {
      id,
      read: false,
      time: new Date().toLocaleString('zh-CN', { hour12: false }),
      ...record,
      orderId: normalizedOrderId,
      orderIds
    }
    items.value = [row, ...items.value].slice(0, MAX)
    if (row.type === 'CHAT' && row.orderId != null) {
      const oid = String(row.orderId)
      chatUnreadByOrder.value = {
        ...chatUnreadByOrder.value,
        [oid]: Number(chatUnreadByOrder.value[oid] || 0) + 1
      }
    }
  }

  function markAllRead() {
    items.value = items.value.map((x) => ({ ...x, read: true }))
  }

  function markIdRead(id) {
    items.value = items.value.map((x) => (x.id === id ? { ...x, read: true } : x))
  }

  function setChatUnreadFromMessages(messages = []) {
    const next = {}
    for (const m of messages) {
      const oid = m?.orderId
      if (oid == null) continue
      const key = String(oid)
      next[key] = Number(next[key] || 0) + 1
    }
    chatUnreadByOrder.value = next
  }

  function getChatUnreadByOrder(orderId) {
    if (orderId == null) return 0
    return Number(chatUnreadByOrder.value[String(orderId)] || 0)
  }

  function markChatOrderRead(orderId) {
    if (orderId == null) return
    const key = String(orderId)
    if (!chatUnreadByOrder.value[key]) return
    const next = { ...chatUnreadByOrder.value }
    delete next[key]
    chatUnreadByOrder.value = next
  }

  function clearChatUnread() {
    chatUnreadByOrder.value = {}
  }

  return {
    items,
    unreadCount,
    chatUnreadTotal,
    add,
    markAllRead,
    markIdRead,
    setChatUnreadFromMessages,
    getChatUnreadByOrder,
    markChatOrderRead,
    clearChatUnread
  }
})
