import dayjs from 'dayjs'

/**
 * 格式化为 YYYY-MM-DD HH:mm:ss
 * @param {string|number|Date} value
 * @returns {string}
 */
export function formatDateTime(value) {
  if (value == null || value === '') return '-'
  const d = dayjs(value)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : '-'
}
