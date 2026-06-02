import http from '@/api/index.js'

export function getHistory(page = 1, size = 20) {
  return http.get('/history', { params: { page, size } })
}

export function getHistoryDetail(taskId) {
  return http.get(`/history/${taskId}`)
}
