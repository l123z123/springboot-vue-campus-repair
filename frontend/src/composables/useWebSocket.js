import { Client } from '@stomp/stompjs'
import { ElNotification } from 'element-plus'
import { getRepairStatusLabel } from '@/constants/repairStatus'
import { useUserStore } from '@/stores/user'
import { useSystemNotifyStore } from '@/stores/systemNotify'

const WS_TOPIC = '/topic/notify'

let stompClient = null
let reconnectAttempts = 0
const MAX_RECONNECT = 5

function extractRawField(rawBody, field) {
  if (typeof rawBody !== 'string' || !field) {
    return null
  }
  const escaped = field.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const reg = new RegExp(`"${escaped}"\\s*:\\s*("([^"]+)"|(\\d+)|null)`)
  const m = rawBody.match(reg)
  if (!m) {
    return null
  }
  if (m[2] != null) {
    return m[2]
  }
  if (m[3] != null) {
    return m[3]
  }
  return null
}

/**
 * 与 Vite 代理一致：context-path=/api，故为 ws(s)://当前站点/api/ws
 * 不再使用 sockjs-client，消除浏览器对 document.unload 的弃用警告
 */
function getStompBrokerUrl() {
  if (typeof window === 'undefined') {
    return 'ws://localhost:5173/api/ws'
  }
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const custom = import.meta.env.VITE_WS_BROKER_URL
  if (custom) {
    return String(custom)
  }
  return `${proto}//${window.location.host}/api/ws`
}

/** 广播 /topic/notify 下按当前登录角色与 targetUserId 做客户端过滤，减少无关弹窗 */
function shouldShowPush(msg, rawBody) {
  const u = useUserStore()
  const userId = u.userInfo?.userId
  const role = u.userInfo?.role ?? 0
  const t = msg.type
  if (t === 'SLA_TIMEOUT') {
    return role === 2
  }
  if (t === 'CREATED') {
    return role === 1 || role === 2
  }
  if (t === 'STATUS_CHANGED') {
    if (role === 2) {
      return true
    }
    const target = extractRawField(rawBody, 'targetUserId') ?? msg.targetUserId
    if (target == null) {
      return true
    }
    return String(userId) === String(target)
  }
  if (t === 'CHAT') {
    return String(userId) === String(msg.targetUserId)
  }
  return true
}

function handleMessage(body) {
  try {
    const rawBody = typeof body === 'string' ? body : ''
    const msg = typeof body === 'string' ? JSON.parse(body) : body
    if (!shouldShowPush(msg, rawBody)) {
      return
    }
    const type = msg.type
    const orderId = extractRawField(rawBody, 'orderId') ?? msg.orderId
    const status = msg.status
    const location = msg.location
    const sys = useSystemNotifyStore()
    if (type === 'CREATED') {
      const title = '新工单提醒'
      const line = `工单 #${orderId} 已提交${location ? '，地点：' + location : ''}`
      ElNotification({ title, message: line, type: 'info' })
      sys.add({ type, orderId, title, summary: line, status })
    } else if (type === 'STATUS_CHANGED') {
      const label = getRepairStatusLabel(status)
      const title = Number(status) === 8 ? '工单已评价' : '工单状态更新'
      const line = `工单 #${orderId} 已变更为「${label}」`
      ElNotification({
        title,
        message: line,
        type: 'success'
      })
      sys.add({ type, orderId, title, summary: line, status })
    } else if (type === 'SLA_TIMEOUT') {
      const text = msg.message || '存在工单处理超时风险，请关注'
      const cnt = msg.count != null ? `（${msg.count} 单）` : ''
      const line = `${text}${cnt}`
      const orderIds = Array.isArray(msg.orderIds)
        ? msg.orderIds.map((x) => String(x))
        : []
      const idHint =
        orderIds.length > 0
          ? ` 点击查看铃铛内「涉及工单」跳转详情。`
          : ''
      ElNotification({
        title: 'SLA超时预警',
        message: line + idHint,
        type: 'warning',
        duration: 10000
      })
      sys.add({
        type,
        title: 'SLA超时预警',
        summary: line,
        orderId: orderIds.length === 1 ? orderIds[0] : null,
        orderIds
      })
    } else if (type === 'CHAT') {
      const who = msg.senderName || '对方'
      const p = msg.preview || '新消息'
      const line = `${who}：${p}`
      ElNotification({
        title: '工单消息',
        message: line,
        type: 'info',
        duration: 4000
      })
      sys.add({ type, orderId: msg.orderId, title: '沟通', summary: line })
      try {
        window.dispatchEvent(
          new CustomEvent('repair-order-chat', { detail: { orderId: String(msg.orderId || '') } })
        )
      } catch {
        // ignore
      }
    }
  } catch (e) {
    console.warn('[WS] parse message failed', e)
  }
}

/**
 * 建立 WebSocket 连接并订阅 /topic/notify
 * @param {() => void} [onConnected] 连接成功回调
 * @returns {() => void} disconnect 函数
 */
export function useWebSocket(onConnected) {
  function connect() {
    if (stompClient?.connected) return () => {}

    const client = new Client({
      brokerURL: getStompBrokerUrl(),
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        reconnectAttempts = 0
        client.subscribe(WS_TOPIC, (frame) => handleMessage(frame.body))
        onConnected?.()
      },
      onStompError: (frame) => {
        console.warn('[WS] STOMP error', frame.headers?.message)
      },
      onWebSocketClose: () => {
        if (reconnectAttempts < MAX_RECONNECT && stompClient === client) {
          reconnectAttempts++
          setTimeout(() => client.activate(), 2000)
        }
      }
    })
    stompClient = client
    client.activate()

    return () => {
      client.deactivate()
      if (stompClient === client) stompClient = null
    }
  }

  return { connect }
}
