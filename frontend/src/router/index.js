import { createRouter, createWebHistory } from 'vue-router'
import AuthLayout from '@/layouts/AuthLayout.vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import Welcome from '@/views/Welcome.vue'
import Home from '@/views/Home.vue'
import AnalyzeResult from '@/views/AnalyzeResult.vue'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import ForgotPassword from '@/views/ForgotPassword.vue'
import History from '@/views/History.vue'
import HistoryDetail from '@/views/HistoryDetail.vue'
import Articles from '@/views/Articles.vue'
import ArticleDetail from '@/views/ArticleDetail.vue'
import Knowledge from '@/views/Knowledge.vue'
import KnowledgeDetail from '@/views/KnowledgeDetail.vue'
import Favorites from '@/views/Favorites.vue'
import Profile from '@/views/Profile.vue'
import ProfilePassword from '@/views/ProfilePassword.vue'
import ProfileSecurity from '@/views/ProfileSecurity.vue'
import ProfileTransactions from '@/views/ProfileTransactions.vue'

const routes = [
  { path: '/', redirect: '/welcome' },
  {
    path: '/welcome',
    name: 'welcome',
    component: Welcome
  },
  {
    path: '/',
    component: AuthLayout,
    children: [
      { path: 'login', name: 'login', component: Login },
      { path: 'register', name: 'register', component: Register },
      { path: 'forgot-password', name: 'forgot-password', component: ForgotPassword }
    ]
  },
  {
    path: '/',
    component: DefaultLayout,
    children: [
      { path: 'home', name: 'home', component: Home },
      { path: 'history', name: 'history', component: History },
      { path: 'history/:taskId', name: 'history-detail', component: HistoryDetail },
      { path: 'articles', name: 'articles', component: Articles },
      { path: 'articles/:id', name: 'article-detail', component: ArticleDetail },
      { path: 'knowledge', name: 'knowledge', component: Knowledge },
      { path: 'knowledge/:id', name: 'knowledge-detail', component: KnowledgeDetail },
      { path: 'favorites', name: 'favorites', component: Favorites },
      { path: 'profile', name: 'profile', component: Profile },
      { path: 'profile/security', name: 'profile-security', component: ProfileSecurity },
      { path: 'profile/password', name: 'profile-password', component: ProfilePassword },
      { path: 'profile/transactions', name: 'profile-transactions', component: ProfileTransactions },
      { path: 'analyze/:taskId', name: 'analyze-detail', component: AnalyzeResult }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 导航守卫：未登录跳转
const publicRoutes = ['login', 'register', 'forgot-password', 'welcome']
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!token && !publicRoutes.includes(to.name)) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
