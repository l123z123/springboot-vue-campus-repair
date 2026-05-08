import request from '@/utils/request'

/**
 * 获取公告列表（分页）
 * @param {{ page?: number, size?: number }} params
 */
export function getNoticeList(params) {
  return request.get('/notice/list', { params }).then(res => res.data ?? res)
}

/**
 * 管理员发布公告
 * @param {{ title: string, content: string, pinned?: boolean, author?: string }} body
 */
export function createNotice(body) {
  return request.post('/admin/notice', body).then(res => res.data ?? res)
}

/**
 * 管理员删除公告
 * @param {number|string} id
 */
export function deleteNotice(id) {
  return request.delete(`/admin/notice/${id}`).then(res => res.data ?? res)
}
