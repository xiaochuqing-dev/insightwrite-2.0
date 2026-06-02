import http from '@/api/index.js'

export function getEntries(page = 1, size = 20, category = '', search = '') {
  const params = { page, size }
  if (category) params.category = category
  if (search) params.search = search
  return http.get('/knowledge', { params })
}

export function getEntryDetail(id) {
  return http.get(`/knowledge/${id}`)
}
