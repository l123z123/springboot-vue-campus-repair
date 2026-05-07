import request from '@/utils/request'

/**
 * 登录接口
 * @param {{ username: string, password: string }} userForm
 * @returns {Promise<{ token: string, userId: number, username: string, realName: string, role: number }>}
 */
export function login(userForm) {
  return request.post('/auth/login', userForm).then((res) => res.data ?? res)
}
