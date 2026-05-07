import request from '@/utils/request'

/**
 * 通用图片上传，返回可访问的完整 URL
 */
export function uploadChatFile(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/common/upload', form).then((r) => r.data)
}

function unwrapPage(res) {
  const pageObj = res?.data
  if (pageObj && Array.isArray(pageObj.records)) {
    return { records: pageObj.records, total: pageObj.total ?? 0, current: pageObj.current, size: pageObj.size }
  }
  if (res && Array.isArray(res.records)) {
    return { records: res.records, total: res.total ?? 0, current: res.current, size: res.size }
  }
  return { records: [], total: 0, current: 1, size: 10 }
}

/**
 * 某工单下聊天记录
 */
export function getChatList(orderId, page = 1, size = 200) {
  return request
    .get(`/chat/list/${String(orderId)}`, { params: { page, size } })
    .then(unwrapPage)
}

/**
 * 发送（接收方可空，由后端按工单推断）
 * @param {{ orderId: string|number, content?: string, images?: string, receiverId?: string|number }} body
 */
export function sendChatMessage(body) {
  const pl = {
    orderId: String(body.orderId),
    content: body.content != null ? body.content : '',
    images: body.images
  }
  if (body.receiverId != null) {
    pl.receiverId = String(body.receiverId)
  }
  return request.post('/chat/send', pl).then((res) => {
    if (res?.data != null) {
      return res.data
    }
    return res
  })
}

/**
 * 最近会话（按工单聚合）
 */
export function getConversations(page = 1, size = 30) {
  return request.get('/chat/conversations', { params: { page, size } }).then(unwrapPage)
}

/**
 * 获取当前用户未读消息（按消息明细）
 */
export function getUnreadMessages() {
  return request.get('/chat/unread').then((res) => res.data ?? res ?? [])
}

/**
 * 将某工单消息标记为已读
 */
export function markOrderRead(orderId) {
  return request.post(`/chat/mark-read/${String(orderId)}`).then((res) => res.data ?? res)
}
