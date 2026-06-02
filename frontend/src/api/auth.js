import http from '@/api/index.js'

export function login(email, password) {
  return http.post('/auth/login', { email, password })
}

export function register(email, username, password, code) {
  return http.post('/auth/register', { email, username, password, code })
}

export function sendCode(email, purpose) {
  return http.post('/auth/send-code', { email, purpose })
}

export function forgotPassword(email, code, newPassword) {
  return http.post('/auth/forgot-password', {
    email,
    code,
    new_password: newPassword
  })
}

export function getMe() {
  return http.get('/auth/me')
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/auth/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
