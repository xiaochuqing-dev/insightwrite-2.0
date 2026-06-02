import http from '@/api/index.js'

export function getArticles(page = 1, size = 20, category = '') {
  const params = { page, size }
  if (category) params.category = category
  return http.get('/articles', { params })
}

export function getArticleDetail(id) {
  return http.get(`/articles/${id}`)
}
