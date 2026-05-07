/**
 * 公告通知：当前为前端 localStorage/静态演示，与核心业务分离。
 * 若毕设/答辩需强调「可扩展」可在此接 GET /notice/list，不影响报修主流程。
 */
// import request from '@/utils/request'

/**
 * 获取公告列表
 * @returns {Promise<Array<{ id: number, title: string, content: string, time: string }>>}
 */
export function getNoticeList() {
  return Promise.resolve([])
}
