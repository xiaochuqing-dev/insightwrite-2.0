import http from '@/api/index.js'

export function getCredits() {
  return http.get('/credits')
}

export function getTransactions(page = 1) {
  return http.get('/credits/transactions', { params: { page } })
}
