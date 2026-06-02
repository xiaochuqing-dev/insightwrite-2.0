import http from '@/api/index.js'

export function getFavorites(page = 1, size = 20) {
  return http.get('/favorites', { params: { page, size } })
}

export function getKnowledgeFavorites(page = 1, size = 20) {
  return http.get('/favorites/knowledge', { params: { page, size } })
}

export function addHistoryFavorite(taskId) {
  return http.post(`/history/${taskId}/favorite`)
}

export function removeHistoryFavorite(taskId) {
  return http.delete(`/history/${taskId}/favorite`)
}

export function addKnowledgeFavorite(knowledgeId) {
  return http.post(`/knowledge/${knowledgeId}/favorite`)
}

export function removeKnowledgeFavorite(knowledgeId) {
  return http.delete(`/knowledge/${knowledgeId}/favorite`)
}
