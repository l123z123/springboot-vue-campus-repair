/**
 * 图片 URL 标准化：支持完整 URL、相对路径、/api/files/ 路径
 * 后端可能返回完整 URL (http://...) 或相对路径 (/api/files/xxx)
 */
export function normalizeImageUrl(url) {
  if (!url || typeof url !== 'string') return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/')) {
    const base = import.meta.env.VITE_API_BASE_URL || '/api'
    // 避免重复拼接 /api（如 /api/files/xxx）
    const path = url.startsWith(base) ? url : base + url
    return `${typeof window !== 'undefined' ? window.location.origin : ''}${path}`
  }
  if (typeof window !== 'undefined' && url.includes('/api/files/')) {
    const m = url.match(/\/api\/files\/(.+)$/)
    if (m) return `${window.location.origin}/api/files/${m[1]}`
  }
  return url
}

/** 从上传接口响应中提取图片 URL，兼容 { data: { url } } 和 { data: "url" } */
export function extractImageUrl(res) {
  if (!res) return ''
  const data = res.data ?? res
  if (typeof data === 'string') return data
  if (data && typeof data.url === 'string') return data.url
  return ''
}

/** 单张图片最大 5MB */
export const MAX_IMAGE_SIZE = 5 * 1024 * 1024
