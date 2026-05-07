import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import router from '@/router'

const TOKEN_KEY = 'token'
const USER_KEY = 'userInfo'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const userInfo = ref(
    (() => {
      try {
        const s = localStorage.getItem(USER_KEY)
        if (!s) return null
        const o = JSON.parse(s)
        if (o && o.role != null) {
          const n = Number(o.role)
          o.role = Number.isNaN(n) ? 0 : n
        }
        if (o && o.userId != null && o.userId !== '') {
          o.userId = String(o.userId)
        }
        return o
      } catch {
        return null
      }
    })()
  )

  function setToken(t) {
    token.value = t || ''
    if (t) localStorage.setItem(TOKEN_KEY, t)
    else localStorage.removeItem(TOKEN_KEY)
  }

  function normalizeUserPayload(res) {
    if (!res) return null
    const r = res.role
    const roleNum = r == null || r === '' ? 0 : Number(r)
    return {
      // 大整型在 JSON 中可能被截断，后端已转字符串；本地统一用 string 存 id
      userId: res.userId != null && res.userId !== '' ? String(res.userId) : undefined,
      username: res.username,
      realName: res.realName,
      role: Number.isNaN(roleNum) ? 0 : roleNum
    }
  }

  function setUserInfo(info) {
    if (info && 'role' in info) {
      const n = Number(info.role)
      const uid = info.userId != null && info.userId !== '' ? String(info.userId) : info.userId
      userInfo.value = { ...info, userId: uid, role: Number.isNaN(n) ? 0 : n }
    } else {
      userInfo.value = info
    }
    if (userInfo.value) localStorage.setItem(USER_KEY, JSON.stringify(userInfo.value))
    else localStorage.removeItem(USER_KEY)
  }

  async function login(username, password) {
    const data = await request.post('/auth/login', { username, password })
    const res = data.data || data
    setToken(res.token)
    setUserInfo(normalizeUserPayload(res))
  }

  /** 邮箱 + 验证码登录（发码 scene=LOGIN） */
  async function loginByEmail(email, code) {
    const data = await request.post('/auth/login-email', { email, code })
    const res = data.data || data
    setToken(res.token)
    setUserInfo(normalizeUserPayload(res))
  }

  async function logout() {
    try {
      await request.post('/auth/logout')
    } catch (e) {
      // 后端登出失败不影响前端清理本地状态
      console.warn('调用登出接口失败，将继续本地退出', e)
    } finally {
      setToken('')
      setUserInfo(null)
      router.push('/login')
    }
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    login,
    loginByEmail,
    logout
  }
})
