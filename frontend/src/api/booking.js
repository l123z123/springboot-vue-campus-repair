/**
 * 预约维修：当前为前端 localStorage 演示，非派单主流程的必选项。
 * 报修/审核/派单/维修/评价等以 repair 主接口为准。
 */
// import request from '@/utils/request'

/**
 * 提交预约
 * @param {{ time: string, location?: string, desc?: string }} body
 * @returns {Promise<{ id?: number, success: boolean }>}
 */
export function submitBooking(body) {
  return Promise.resolve({ success: true })
}

/**
 * 获取我的预约列表
 * @returns {Promise<Array<{ id: number, time: string, location?: string, desc?: string }>>}
 */
export function getMyBookings() {
  return Promise.resolve([])
}
