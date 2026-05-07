import request from '@/utils/request'

// 提交反馈
export function submitFeedback(body) {
  return request.post('/feedback/submit', body).then((res) => res.data ?? res)
}

// 当前登录用户的反馈列表
export function getMyFeedback() {
  return request.get('/feedback/my-list').then((res) => res.data ?? res)
}

// 管理员：获取反馈列表（支持分页、搜索、状态筛选）
// 返回 { records, total, size, current, pages }，确保 total 正确解析
export function getAllFeedback(params = {}) {
  return request.get('/admin/feedback/list', { params }).then((res) => {
    // 响应可能是 { code, message, data: PageResult } 或直接是 PageResult
    const raw = res?.data ?? res
    const page = raw?.data ?? raw
    // 确保 total 为数字，避免 Total 显示 0 的 bug
    if (page && typeof page.total === 'string') {
      page.total = Number(page.total) || 0
    }
    return page
  })
}

// 管理员：更新反馈状态
export function updateFeedbackStatus(id, status = 1) {
  return request.put(`/admin/feedback/${id}/status`, { status }).then((res) => res.data ?? res)
}

