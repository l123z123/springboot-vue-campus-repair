import request from '@/utils/request'

/**
 * 管理端用户列表（分页，与 IPage 一致）
 */
export function getAdminUserList (params) {
  return request.get('/admin/user/list', { params }).then((res) => res.data ?? res)
}

export function createAdminUser (body) {
  return request.post('/admin/user', body).then((res) => res.data ?? res)
}

export function updateAdminUser (userId, body) {
  return request.put(`/admin/user/${userId}`, body).then((res) => res.data ?? res)
}

export function updateAdminUserStatus (userId, status) {
  return request.patch(`/admin/user/${userId}/status`, { status }).then((res) => res.data ?? res)
}

export function deleteAdminUser (userId) {
  return request.delete(`/admin/user/${userId}`).then((res) => res.data ?? res)
}
