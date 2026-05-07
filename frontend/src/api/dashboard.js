import request from '@/utils/request'

export function getTodayStats() {
  return request.get('/admin/stats/today').then((res) => res.data ?? res)
}

export function getTotalStats() {
  return request.get('/admin/stats/total').then((res) => res.data ?? res)
}

export function getLocationTop10() {
  return request.get('/admin/stats/location-top10').then((res) => res.data ?? res)
}

export function getStatusDist() {
  return request.get('/admin/stats/status-dist').then((res) => res.data ?? res)
}

export function getTypeTree() {
  return request.get('/admin/stats/type-tree').then((res) => res.data ?? res)
}

export function getHourHeatmap() {
  return request.get('/admin/stats/hour-heatmap').then((res) => res.data ?? res)
}

export function getStaffPerformance() {
  return request.get('/admin/stats/staff-performance').then((res) => res.data ?? res)
}

/**
 * 概览统计：{ total, pending, completed, urgent }
 */
export function getStatsOverview() {
  return request.get('/admin/stats/overview').then((res) => res.data ?? res)
}
