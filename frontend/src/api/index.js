import axios from 'axios'

// 开发阶段用 mock，上线时改此 URL
const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器——自动带 token
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器——统一错误处理
http.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
    }
    return Promise.reject(err)
  }
)

export default http
