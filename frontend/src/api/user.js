import request from '@/utils/request'

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  return request.get('/user/profile').then((res) => res.data ?? res)
}

/**
 * 更新个人资料
 * @param {{
 *  avatarUrl?: string,
 *  nickname?: string,
 *  gender?: number,
 *  department?: string,
 *  phone?: string,
 *  signature?: string
 * }} body
 */
export function updateUserProfile(body) {
  return request.put('/user/profile', body).then((res) => res.data ?? res)
}

/**
 * 修改密码
 * @param {{ oldPassword: string, newPassword: string }} body
 */
export function updatePassword(body) {
  return request.put('/user/password', body).then((res) => res.data ?? res)
}

/**
 * 退出登录
 */
export function logout() {
  return request.post('/auth/logout').catch(() => {}).then(() => {})
}
