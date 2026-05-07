import request from '@/utils/request'

const STATUS_MAP = {
  0: 'pending',
  1: 'audit',
  2: 'audited',
  3: 'dispatch',
  4: 'dispatched',
  5: 'processing',
  6: 'done',
  7: 'confirmed',
  8: 'closed',
  9: 'rejected',
  10: 'cancelled'
}
const URGENCY_MAP = { 1: 'low', 2: 'medium', 3: 'high' }

function formatDate(v) {
  if (!v) return ''
  if (typeof v === 'string') return v.slice(0, 16).replace('T', ' ')
  // 后端 LocalDateTime 序列化为 [year, month, day, hour, minute, second]
  if (Array.isArray(v) && v.length >= 6) {
    const [y, m, d, h = 0, min = 0, s = 0] = v
    const pad = (n) => String(n).padStart(2, '0')
    return `${y}-${pad(m)}-${pad(d)} ${pad(h)}:${pad(min)}:${pad(s)}`
  }
  return ''
}

function firstImage(images) {
  if (!images) return ''
  if (typeof images === 'string') {
    try {
      const arr = JSON.parse(images)
      return Array.isArray(arr) && arr[0] ? arr[0] : ''
    } catch {
      return ''
    }
  }
  if (Array.isArray(images) && images[0]) return images[0]
  return ''
}

function parseImages(images) {
  if (!images) return []
  if (Array.isArray(images)) return images
  if (typeof images === 'string') {
    try {
      const arr = JSON.parse(images)
      return Array.isArray(arr) ? arr : [images]
    } catch {
      return [images]
    }
  }
  return []
}

function normalizeRecord(r) {
  if (!r) return r
  const idRaw = r.orderId != null ? r.orderId : r.id
  const id = idRaw != null && idRaw !== '' ? String(idRaw) : undefined
  const orderIdStr = r.orderId != null && r.orderId !== '' ? String(r.orderId) : id
  const status = typeof r.status === 'number' ? (STATUS_MAP[r.status] ?? 'pending') : r.status
  const urgency = typeof r.urgency === 'number' ? (URGENCY_MAP[r.urgency] ?? 'low') : r.urgency
  const date = r.createTime ? formatDate(r.createTime) : (r.date || '')
  const image = firstImage(r.images) || r.image || ''
  const createTime = r.createTime ? formatDate(r.createTime) : (r.date || '')
  const ticketNo = r.ticketNo || (id != null ? `T${id}` : '')
  const imgs = parseImages(r.images)
  
  // 时间字段格式化
  const formatTimeField = (time) => time ? formatDate(time) : ''
  
  const num = typeof r.status === 'number' ? r.status : (r.status != null && !Number.isNaN(Number(r.status)) ? Number(r.status) : null)
  return {
    id,
    orderId: orderIdStr,
    ticketNo,
    location: r.location,
    description: r.description || r.title,
    title: r.title,
    urgency,
    status, // 字符串状态（用于列表/标签等）
    statusCode: num != null ? num : 0, // 数字状态，详情页与条件判断必用
    isUrgent: r.isUrgent,
    image,
    images: imgs.length ? imgs : (image ? [image] : []),
    date,
    createTime,
    auditTime: formatTimeField(r.auditTime),
    dispatchTime: formatTimeField(r.dispatchTime),
    startTime: formatTimeField(r.startTime),
    completedTime: formatTimeField(r.completedTime),
    confirmTime: formatTimeField(r.confirmTime),
    updateTime: formatTimeField(r.updateTime),
    completedImages: r.completedImages,
    repairmanId: r.repairmanId != null && r.repairmanId !== '' ? String(r.repairmanId) : null,
    phoneNumber: r.phoneNumber,
    phone: r.phoneNumber || r.phone,
    campus: r.campus,
    area: r.area,
    category: r.category || '其他',
    reporterName: r.studentName || r.reporterName || (r.userId != null && r.userId !== '' ? `用户${r.userId}` : '-'),
    reporterNo: r.reporterNo || r.userId || '-',
    userId: r.userId != null && r.userId !== '' ? String(r.userId) : undefined,
    repairmanName: r.repairmanName,
    studentName: r.studentName
  }
}

/** 列表 Tab 与后端多状态：传 statusIn=0,1,11，勿与旧 0/1/2 三态混用 */
const STATUS_TO_NUM = { pending: 0, processing: 1, done: 2, cancelled: 3, closed: 3, evaluated: 4 }

/**
 * 获取报修列表（后端返回 IPage.records）
 * @param {{ status?: string|number, page?: number, limit?: number, size?: number, keyword?: string, location?: string }} params
 * @returns {Promise<Array>}
 */
export function getRepairList(params = {}) {
  return getRepairListPage(params).then((p) => p.records ?? [])
}

const URGENCY_TO_NUM = { normal: 1, urgent: 2, critical: 3 }

/**
 * 获取报修列表（分页，返回 records + total）
 * @param {{ status?: string|number, page?: number, limit?: number, size?: number, keyword?: string, location?: string, campus?: string, urgency?: string|number, dateRange?: [string, string] }} params
 * @returns {Promise<{ records: Array, total: number }>}
 */
export function getRepairListPage(params = {}) {
  const { status, statusIn, page = 1, limit, size, keyword, location, campus, urgency, dateRange } = params
  const query = { page, size: size || limit || 10 }
  if (statusIn != null && statusIn !== '') {
    query.statusIn = Array.isArray(statusIn) ? statusIn.join(',') : String(statusIn)
  } else if (status != null && status !== 'all') {
    query.status = typeof status === 'number' ? status : (STATUS_TO_NUM[status] ?? status)
  }
  if (keyword) query.keyword = keyword
  if (location) query.location = location
  if (campus) query.campus = campus
  if (urgency != null && urgency !== '') {
    query.urgency = typeof urgency === 'number' ? urgency : (URGENCY_TO_NUM[urgency] ?? urgency)
  }
  if (dateRange && Array.isArray(dateRange) && dateRange.length === 2) {
    query.startDate = dateRange[0]
    query.endDate = dateRange[1]
  }

  return request
    .get('/repair/list', { params: query })
    .then((res) => {
      const data = res.data ?? res
      const pageObj = Array.isArray(data) ? { records: data, total: data.length } : (data.data ?? data)
      const raw = pageObj.records ?? pageObj.list ?? []
      const total = typeof pageObj.total === 'string' ? Number(pageObj.total) || 0 : (pageObj.total ?? 0)
      return {
        records: raw.map(normalizeRecord),
        total
      }
    })
}

/**
 * 获取报修详情
 * @param {number|string} id
 * @returns {Promise<Object>}
 */
export function getRepairDetail(id) {
  return request.get(`/repair/${id}`).then((res) => {
    const raw = res.data ?? res
    return normalizeRecord(raw)
  })
}

/**
 * 更新工单状态（目标 status 为后端 RepairOrderStatus 数字码，如 5 维修中、6 完成等）
 * @param {number|string} id 工单 ID
 * @param {number} status 目标状态
 */
export function updateRepairStatus(id, status) {
  return request.patch(`/repair/${id}/status`, { status }).then((res) => res.data ?? res)
}

/**
 * 提交评价
 * @param {number|string} id 工单 ID
 * @param {{ score: number, comment: string, isAnonymous: number }} body
 * @returns {Promise<Object>}
 */
export function submitEvaluation(id, body) {
  return request.post(`/repair/${id}/evaluation`, body).then((res) => res.data ?? res)
}

/**
 * 获取评价
 * @param {number|string} id 工单 ID
 * @returns {Promise<Object>}
 */
export function getEvaluation(id) {
  return request.get(`/repair/${id}/evaluation`).then((res) => res.data ?? res)
}

/**
 * 提交报修
 * @param {{ title: string, description?: string, location?: string, urgency: number, images?: string }} body
 * @returns {Promise<Object>}
 */
export function createRepair(body) {
  return request.post('/repair', body).then((res) => res.data ?? res)
}

/**
 * 审核工单（管理员）
 * @param {number|string} orderId 工单ID
 * @param {boolean} approved 是否通过
 * @param {{ approved: boolean, remark?: string, assignRepairmanId?: string|number }} body
 *    assignRepairmanId 为空时进入待派单；有值时审核通过后直接派单
 * @returns {Promise<Object>}
 */
export function auditOrder(orderId, body) {
  return request.post(`/repair/${orderId}/audit`, body).then((res) => res.data ?? res)
}

/**
 * 派单给维修工（管理员）
 * @param {number|string} orderId 工单ID
 * @param {number|string} repairmanId 维修工ID
 * @returns {Promise<Object>}
 */
export function dispatchOrder(orderId, repairmanId) {
  return request.post(`/repair/${orderId}/dispatch`, { repairmanId }).then((res) => res.data ?? res)
}

/**
 * 获取智能派单推荐（管理员）
 * @param {number|string} orderId 工单ID
 * @returns {Promise<Array>}
 */
export function getDispatchRecommendations(orderId) {
  return request.get(`/repair/${orderId}/dispatch-recommendations`).then((res) => res.data ?? res)
}

/**
 * 维修工接单
 * @param {number|string} orderId 工单ID
 * @returns {Promise<Object>}
 */
export function acceptOrder(orderId) {
  return request.post(`/repair/${orderId}/accept`).then((res) => res.data ?? res)
}

/**
 * 维修工完成维修
 * @param {number|string} orderId 工单ID
 * @returns {Promise<Object>}
 */
export function completeOrder(orderId) {
  return request.post(`/repair/${orderId}/complete`).then((res) => res.data ?? res)
}

/**
 * 学生确认维修完成
 * @param {number|string} orderId 工单ID
 * @returns {Promise<Object>}
 */
export function confirmOrder(orderId) {
  return request.post(`/repair/${orderId}/confirm`).then((res) => res.data ?? res)
}

/**
 * 维修工完成维修（支持上传完成照片）
 * @param {number|string} orderId 工单ID
 * @param {string} completedImages 完成照片（JSON字符串）
 * @returns {Promise<Object>}
 */
export function completeOrderWithImages(orderId, completedImages) {
  return request.post(`/repair/${orderId}/complete-with-images`, { completedImages }).then((res) => res.data ?? res)
}

