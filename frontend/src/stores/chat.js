import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'chatThreads'

function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function saveToStorage(threads) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(threads))
  } catch {
    // 忽略本地存储错误，保证聊天功能可用
  }
}

const defaultThreads = [
  {
    id: 1,
    user: '李师傅',
    avatar: '',
    lastMsg: '好的，我马上到。',
    time: '10:30',
    type: 'chat',
    relatedOrderId: 101,
    messages: [
      { text: '您的工单已被接单。', time: '10:05', self: false },
      { text: '好的，我马上到。', time: '10:30', self: false },
      { text: '好的', time: '10:32', self: true }
    ]
  },
  {
    id: 2,
    user: '系统通知',
    avatar: '',
    lastMsg: '您的工单已被接单。',
    time: '10:05',
    type: 'system',
    relatedOrderId: 101,
    messages: [{ text: '您的工单已被接单。', time: '10:05', self: false }]
  }
]

export const useChatStore = defineStore('chat', () => {
  const threads = ref(loadFromStorage() || defaultThreads)

  function persist() {
    saveToStorage(threads.value)
  }

  function threadList() {
    return threads.value
  }

  function getThread(id) {
    return threads.value.find((t) => String(t.id) === String(id))
  }

  function ensureThread(id) {
    let t = getThread(id)
    if (!t) {
      t = {
        id,
        user: '师傅',
        avatar: '',
        lastMsg: '',
        time: '',
        type: 'chat',
        relatedOrderId: null,
        messages: []
      }
      threads.value.push(t)
      persist()
    }
    return t
  }

  function addMessage(threadId, payload) {
    const t = ensureThread(threadId)
    const msg = {
      text: payload.text,
      time: payload.time,
      self: !!payload.self,
      type: payload.type || 'text',
      url: payload.url || null
    }
    t.messages.push(msg)
    t.lastMsg = msg.type === 'image' ? '[图片]' : (msg.text || '')
    t.time = msg.time
    persist()
  }

  return {
    threads,
    threadList,
    getThread,
    addMessage
  }
})

