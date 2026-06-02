<template>
  <div class="default-layout" :class="{ 'is-collapsed': isCollapsed }" :style="bgStyle">
    <aside class="sidebar" aria-label="Primary navigation">
      <div class="sidebar__header">
        <RouterLink class="brand" to="/home" aria-label="InsightWrite home">
          <span class="brand__mark" aria-hidden="true">IW</span>
          <span class="brand__text">InsightWrite</span>
        </RouterLink>

        <button
          class="icon-button sidebar__toggle"
          type="button"
          :aria-label="isCollapsed ? 'Expand navigation' : 'Collapse navigation'"
          @click="isCollapsed = !isCollapsed"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>
      </div>

      <nav class="nav-list">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          class="nav-item"
          :to="item.to"
          :title="isCollapsed ? item.label : undefined"
        >
          <span class="nav-item__icon" v-html="item.icon" aria-hidden="true"></span>
          <span class="nav-item__label">{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="sidebar__footer">
        <RouterLink class="sidebar-user" to="/profile" :title="isCollapsed ? displayName : undefined">
          <span class="avatar sidebar-user__avatar" aria-hidden="true">
            <img v-if="avatarUrl" :src="avatarUrl" alt="" />
            <span v-else>{{ avatarInitial }}</span>
          </span>
          <span class="sidebar-user__body">
            <strong>{{ displayName }}</strong>
            <small>{{ userEmail || '个人设置' }}</small>
          </span>
        </RouterLink>

        <button class="logout-button" type="button" @click="doLogout">
          <svg viewBox="0 0 24 24" aria-hidden="true" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
          <span class="logout-button__text">退出登录</span>
        </button>
      </div>
    </aside>

    <section class="app-shell">
      <header class="topbar">
        <div class="topbar__title">
          <span class="topbar__eyebrow">英语写作与佳作精读平台</span>
          <strong>InsightWrite</strong>
        </div>

        <div class="profile-menu">
          <button
            class="profile-trigger"
            type="button"
            aria-haspopup="menu"
            :aria-expanded="isProfileOpen"
            @click="isProfileOpen = !isProfileOpen"
          >
            <span class="avatar" aria-hidden="true">
              <img v-if="avatarUrl" :src="avatarUrl" alt="" />
              <span v-else>{{ avatarInitial }}</span>
            </span>
            <span class="profile-trigger__name">{{ displayName }}</span>
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="m6 9 6 6 6-6" />
            </svg>
          </button>

          <div v-if="isProfileOpen" class="profile-dropdown" role="menu">
            <div class="profile-dropdown__credits">
              <span>积分</span>
              <strong>{{ userCreditsText }}</strong>
            </div>
            <RouterLink role="menuitem" to="/profile" @click="isProfileOpen = false">
              个人设置
            </RouterLink>
            <button type="button" role="menuitem" @click="doLogout">
              退出登录
            </button>
          </div>
        </div>
      </header>

      <main class="content-area">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getMe } from '@/api/auth.js'
import mainBg from '@/assets/images/main.jpg'
import historyBg from '@/assets/images/history.jpg'
import articleBg from '@/assets/images/article.jpg'
import grammarBg from '@/assets/images/grammar.jpg'
import favoriteBg from '@/assets/images/favorite.jpg'
import settingBg from '@/assets/images/setting.jpg'
import outcomeBg from '@/assets/images/outcome.jpg'

const route = useRoute()
const router = useRouter()

const bgMap = {
  home: mainBg, history: historyBg, 'history-detail': historyBg,
  articles: articleBg, 'article-detail': articleBg,
  knowledge: grammarBg, 'knowledge-detail': grammarBg,
  favorites: favoriteBg, profile: settingBg, 'profile-security': settingBg,
  'profile-password': settingBg, 'profile-transactions': settingBg,
  'analyze-detail': outcomeBg
}

const bgStyle = computed(() => {
  const img = bgMap[route.name] || mainBg
  return {
    backgroundImage: `url(${img})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center',
    backgroundAttachment: 'fixed'
  }
})

function doLogout() {
  localStorage.removeItem('token')
  isProfileOpen.value = false
  router.push('/login')
}

const isCollapsed = ref(false)
const isProfileOpen = ref(false)
const userName = ref('')
const userEmail = ref('')
const userAvatar = ref('')
const userCredits = ref(null)

const displayName = computed(() => userName.value || 'InsightWrite 用户')
const avatarUrl = computed(() => userAvatar.value || '')
const avatarInitial = computed(() => displayName.value.trim().slice(0, 1).toUpperCase())
const userCreditsText = computed(() => typeof userCredits.value === 'number' ? userCredits.value : '--')

async function loadUserInfo() {
  if (!localStorage.getItem('token')) return
  try {
    const response = await getMe()
    userName.value = response.data.username || ''
    userEmail.value = response.data.email || ''
    userAvatar.value = response.data.avatar || ''
    userCredits.value = response.data.credits ?? null
  } catch {
    userName.value = ''
    userEmail.value = ''
    userAvatar.value = ''
    userCredits.value = null
  }
}

onMounted(() => {
  loadUserInfo()
  window.addEventListener('insightwrite:user-updated', loadUserInfo)
})

onBeforeUnmount(() => {
  window.removeEventListener('insightwrite:user-updated', loadUserInfo)
})

const navItems = [
  {
    label: '分析工作台',
    to: '/home',
    icon: '<svg viewBox="0 0 24 24"><path d="M3 11.5 12 4l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/></svg>'
  },
  {
    label: '历史记录',
    to: '/history',
    icon: '<svg viewBox="0 0 24 24"><path d="M12 8v5l3 2M4 12a8 8 0 1 0 2.34-5.66L4 8.68V4"/></svg>'
  },
  {
    label: '佳作精选',
    to: '/articles',
    icon: '<svg viewBox="0 0 24 24"><path d="M5 4h10l4 4v12H5zM14 4v5h5M8 13h8M8 17h6"/></svg>'
  },
  {
    label: '表达知识库',
    to: '/knowledge',
    icon: '<svg viewBox="0 0 24 24"><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21.5zM4 5.5v16M8 7h8M8 11h6"/></svg>'
  },
  {
    label: '我的收藏',
    to: '/favorites',
    icon: '<svg viewBox="0 0 24 24"><path d="m12 20-7-7a4.2 4.2 0 0 1 6-5.9l1 1 1-1a4.2 4.2 0 0 1 6 5.9z"/></svg>'
  },
  {
    label: '个人设置',
    to: '/profile',
    icon: '<svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 4-7 8-7s8 3 8 7"/></svg>'
  }
]
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.default-layout {
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: var(--sidebar-width) minmax(0, 1fr);
  color: var(--color-text);
  transition: grid-template-columns var(--duration-slow) var(--ease-out);
}

.default-layout::before {
  content: "";
  position: fixed;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(90deg, rgba(2, 10, 7, 0.32) 0%, rgba(2, 10, 7, 0.08) 28%, transparent 100%),
    radial-gradient(circle at 80% 12%, rgba(32, 243, 232, 0.08), transparent 34%);
}

.default-layout.is-collapsed {
  grid-template-columns: var(--sidebar-collapsed-width) minmax(0, 1fr);
}

.sidebar {
  position: sticky;
  top: 0;
  z-index: 6;
  height: 100vh;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(151, 210, 166, 0.14);
  background:
    linear-gradient(180deg, rgba(8, 28, 21, 0.74), rgba(4, 16, 11, 0.66)),
    rgba(7, 25, 18, 0.62);
  box-shadow: inset -1px 0 rgba(151, 210, 166, 0.08);
  backdrop-filter: blur(24px) saturate(1.2);
}

.sidebar__header,
.topbar {
  height: var(--topbar-height);
  display: flex;
  align-items: center;
}

.sidebar__header {
  justify-content: space-between;
  gap: var(--space-3);
  padding: 0 var(--space-4);
}

.brand {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: var(--space-3);
  color: var(--color-text);
}

.brand__mark {
  width: 2.5rem;
  height: 2.5rem;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  background: rgba(5, 15, 12, 0.82);
  color: #f5f5dc;
  font-family: var(--font-serif);
  font-weight: 700;
  line-height: 1;
}

.brand__text,
.nav-item__label,
.profile-trigger__name,
.usage-card {
  white-space: nowrap;
  overflow: hidden;
  opacity: 1;
  transition:
    opacity var(--duration-fast) var(--ease-out),
    width var(--duration-slow) var(--ease-out);
}

.is-collapsed .brand__text,
.is-collapsed .nav-item__label,
.is-collapsed .usage-card {
  width: 0;
  opacity: 0;
}

.icon-button {
  width: 2.25rem;
  height: 2.25rem;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  color: var(--gray-600);
  transition:
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
}

.icon-button:hover {
  background: var(--color-glass-strong);
  color: var(--gray-900);
}

.icon-button svg,
.profile-trigger svg {
  width: 1.25rem;
  height: 1.25rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.nav-list {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-4);
}

.nav-item {
  height: 2.75rem;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  color: rgba(236, 245, 226, 0.82);
  font-weight: 750;
  font-size: 1rem;
}

.nav-item:hover,
.nav-item.router-link-active {
  background: rgba(49, 92, 61, 0.42);
  box-shadow: inset 0 0 0 1px rgba(151, 210, 166, 0.18);
  color: #e9ffcf;
  backdrop-filter: blur(18px) saturate(1.16);
}

.nav-item__icon {
  width: 1.25rem;
  height: 1.25rem;
  flex: 0 0 auto;
}

.nav-item__icon :deep(svg) {
  width: 1.25rem;
  height: 1.25rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.sidebar__footer {
  margin-top: auto;
  padding: var(--space-4);
}

.sidebar-user {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
  border: 1px solid rgba(151, 210, 166, 0.2);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  background: rgba(8, 28, 21, 0.42);
  color: var(--color-text);
}

.sidebar-user:hover {
  border-color: rgba(32, 243, 232, 0.36);
  background: rgba(14, 45, 31, 0.64);
  color: #e9ffcf;
}

.sidebar-user__avatar {
  flex: 0 0 auto;
}

.sidebar-user__body {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.sidebar-user__body strong,
.sidebar-user__body small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-user__body strong {
  color: var(--color-text);
  font-size: var(--text-caption);
  font-weight: 900;
}

.sidebar-user__body small {
  color: var(--color-text-muted);
  font-size: var(--text-small);
  font-weight: 700;
}

.logout-button {
  width: 100%;
  height: 2.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  border: 1px solid rgba(239, 68, 68, 0.18);
  border-radius: var(--radius-md);
  background: rgba(120, 36, 36, 0.28);
  color: #ff9b9b;
  font-weight: 700;
  font-size: var(--text-caption);
  transition: background-color var(--duration-fast) var(--ease-out), border-color var(--duration-fast);
}
.logout-button:hover {
  background: rgba(120, 36, 36, 0.38);
  border-color: var(--color-danger);
}

.is-collapsed .logout-button__text {
  display: none;
}

.is-collapsed .sidebar-user {
  justify-content: center;
  padding-inline: var(--space-2);
}

.is-collapsed .sidebar-user__body {
  display: none;
}

.app-shell {
  min-width: 0;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 5;
  justify-content: space-between;
  gap: var(--space-4);
  border-bottom: 1px solid rgba(151, 210, 166, 0.14);
  padding: 0 clamp(var(--space-4), 3vw, var(--space-8));
  background:
    linear-gradient(90deg, rgba(8, 28, 21, 0.58), rgba(4, 16, 11, 0.34)),
    rgba(7, 25, 18, 0.42);
  box-shadow: inset 0 -1px rgba(151, 210, 166, 0.08);
  backdrop-filter: blur(24px) saturate(1.2);
}

.topbar__title {
  min-width: 0;
}

.topbar__eyebrow {
  display: block;
  color: rgba(210, 226, 202, 0.68);
  font-size: var(--text-small);
  line-height: 1.2;
}

.topbar__title strong {
  display: block;
  font-family: var(--font-serif);
  font-size: 1.25rem;
  line-height: 1.35;
  color: var(--color-text);
}

.profile-menu {
  position: relative;
}

.profile-trigger {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  min-height: 2.75rem;
  border: 1px solid rgba(151, 210, 166, 0.24);
  border-radius: var(--radius-lg);
  padding: 0 var(--space-3) 0 var(--space-2);
  background: rgba(8, 28, 21, 0.52);
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(22px);
}

.avatar {
  width: 2rem;
  height: 2rem;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
  color: white;
  font-weight: 750;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-dropdown {
  position: absolute;
  top: calc(100% + var(--space-2));
  right: 0;
  width: 14rem;
  display: grid;
  gap: var(--space-1);
  border: 1px solid rgba(151, 210, 166, 0.22);
  border-radius: var(--radius-lg);
  padding: var(--space-2);
  background: rgba(8, 28, 21, 0.72);
  backdrop-filter: blur(24px);
  box-shadow: var(--shadow-lg);
}

.profile-dropdown__credits {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
}

.profile-dropdown a,
.profile-dropdown button {
  width: 100%;
  display: block;
  border-radius: var(--radius-md);
  padding: var(--space-2) var(--space-3);
  color: var(--color-text-muted);
  text-align: left;
}

.profile-dropdown a:hover,
.profile-dropdown button:hover {
  background: rgba(14, 45, 31, 0.72);
  color: #e9ffcf;
}

.content-area {
  position: relative;
  z-index: 1;
  width: min(100%, var(--content-max-width));
  min-height: calc(100vh - var(--topbar-height));
  margin: 0 auto;
  padding: clamp(var(--space-4), 4vw, var(--space-10));
}

@media (max-width: 760px) {
  .default-layout,
  .default-layout.is-collapsed {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: sticky;
    z-index: 8;
    height: auto;
    border-right: 0;
    border-bottom: 1px solid var(--color-border);
  }

  .sidebar__header {
    height: 4rem;
  }

  .nav-list {
    grid-auto-flow: column;
    grid-auto-columns: max-content;
    overflow-x: auto;
    padding-top: 0;
  }

  .sidebar__footer,
  .sidebar__toggle,
  .topbar__eyebrow,
  .profile-trigger__name {
    display: none;
  }

  .topbar {
    height: 4rem;
  }
}
</style>
