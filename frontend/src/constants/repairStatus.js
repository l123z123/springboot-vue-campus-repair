/**
 * 与后端 RepairOrderStatus 数字码一致（管理员派单版主流程）
 */
export const REPAIR_STATUS_LABEL = {
  0: '待审核',
  1: '审核中',
  2: '已审核',
  3: '待派单',
  4: '已派单',
  5: '维修中',
  6: '维修完成',
  7: '学生确认',
  8: '已评价',
  9: '已拒绝',
  10: '已取消'
}

export function getRepairStatusLabel (code) {
  if (code == null) return '-'
  const c = Number(code)
  return REPAIR_STATUS_LABEL[c] ?? `状态${c}`
}

/** 管理端列表筛选用：多状态 */
export const ADMIN_STATUS_FILTERS = [
  { key: 'all', label: '全部', statusIn: null },
  { key: 'queue', label: '待办（待审/待派单）', statusIn: '0,1,3' },
  { key: 'working', label: '在办', statusIn: '4,5' },
  { key: 'after', label: '完工/确认/已评', statusIn: '6,7,8' },
  { key: 'end', label: '拒绝/取消', statusIn: '9,10' }
]

export function getStatusTagType (code) {
  const c = Number(code)
  if (c === 9 || c === 10) return 'danger'
  if (c === 6 || c === 7 || c === 8) return 'success'
  if (c === 5) return 'primary'
  if (c === 3 || c === 4) return 'warning'
  if (c === 0 || c === 1) return 'info'
  return 'info'
}
