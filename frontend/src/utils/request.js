import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const request = axios.create({
  baseURL,
  timeout: 15000
})

const MAX_RETRY = 1

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function canRetry(err) {
  const cfg = err?.config || {}
  const method = String(cfg.method || 'get').toLowerCase()
  if (method !== 'get') {
    return false
  }
  const code = err?.code
  const status = err?.response?.status
  return code === 'ECONNABORTED' || code === 'ERR_NETWORK' || status === 502 || status === 503 || status === 504
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (err) => Promise.reject(err)
)

request.interceptors.response.use(
  (response) => {
    const { data } = response
    if (data.code === 401) {
      localStorage.removeItem('token')
      router.push('/login')
      return Promise.reject(new Error(data.message || '未登录或已过期'))
    }
    if (data.code !== 200) {
      // 后端全局“系统繁忙”提示在前端有兜底数据时不再重复弹出，降低干扰
      if (!(data.code === 500 && data.message && data.message.indexOf('系统繁忙') !== -1)) {
        ElMessage.error(data.message || '请求失败')
      }
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  (err) => {
    const cfg = err?.config || {}
    cfg.__retryCount = cfg.__retryCount || 0
    if (canRetry(err) && cfg.__retryCount < MAX_RETRY) {
      cfg.__retryCount += 1
      return sleep(300).then(() => request(cfg))
    }

    const msg = err.response?.data?.message || err.message || '网络错误'
    ElMessage.error(msg)
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    return Promise.reject(err)
  }
)

export default request
