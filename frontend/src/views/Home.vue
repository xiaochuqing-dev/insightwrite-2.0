<template>
  <section class="home-view">
    <div class="home-hero">
      <div>
        <p class="eyebrow">Text Analysis Studio</p>
        <h1>写作诊断与佳作精读，都从一篇英语文本开始。</h1>
      </div>
      <div class="status-tile" :class="{ 'is-muted': !isLoggedIn }">
        <span>{{ isLoggedIn ? '可用积分' : '未登录' }}</span>
        <strong>{{ isLoggedIn ? credits : '--' }}</strong>
        <small>{{ creditCaption }}</small>
      </div>
    </div>

    <!-- 活跃任务提示 -->
    <div v-if="activeTaskId" class="active-banner">
      <span>⏳ 有一个分析任务正在进行中</span>
      <RouterLink :to="`/analyze/${activeTaskId}`">查看进度</RouterLink>
    </div>

    <AnalyzeInput
      :initial-text="draftText"
      :credits="isLoggedIn ? credits : null"
      :is-submitting="isSubmitting"
      :has-active-task="!!activeTaskId"
      :active-task-id="activeTaskId"
      :error-message="errorMessage"
      @submit="handleAnalyze"
      @text-change="draftText = $event"
    />
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AnalyzeInput from '@/components/AnalyzeInput.vue'
import { submitAnalyze } from '@/api/analyze.js'
import { getCredits } from '@/api/credits.js'

const router = useRouter()
const storageKey = 'insightwrite:essay-draft'
const activeKey = 'insightwrite:active-task'

const draftText = ref(localStorage.getItem(storageKey) || '')
const activeTaskId = ref(localStorage.getItem(activeKey) || '')
const credits = ref(0)
const dailyResetAt = ref('')
const isLoggedIn = ref(Boolean(localStorage.getItem('token')))
const isSubmitting = ref(false)
const errorMessage = ref('')

const creditCaption = computed(() => {
  if (!isLoggedIn.value) return '登录后可分析作文、范文或外刊文章。'
  if (!dailyResetAt.value) return '今日可用于文本分析。'
  const resetDate = new Date(dailyResetAt.value)
  return Number.isNaN(resetDate.getTime()) ? '今日可用于文本分析。' : `下次重置：${resetDate.toLocaleString()}`
})

function getErrorMessage(error) {
  return error?.response?.data?.error || '提交文本分析失败，请稍后重试。'
}

async function loadCredits() {
  if (!isLoggedIn.value) return

  try {
    const response = await getCredits()
    credits.value = Number(response.data?.credits ?? 0)
    dailyResetAt.value = response.data?.daily_reset_at || ''
  } catch (error) {
    if (error?.response?.status === 401) {
      isLoggedIn.value = false
      localStorage.removeItem('token')
      return
    }
    errorMessage.value = '暂时无法加载分析额度信息。'
  }
}

async function handleAnalyze(payload) {
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    const response = await submitAnalyze(
      payload.essayText,
      payload.mode,
      payload.customRequirement,
      payload.topic,
      payload.generateEssay,
      payload.analysisPreferences
    )
    const taskId = response.data?.task_id
    if (!taskId) throw new Error('Missing task id')
    localStorage.setItem(storageKey, payload.essayText)
    localStorage.removeItem('insightwrite:essay-topic')
    localStorage.removeItem('insightwrite:essay-custom')
    localStorage.removeItem('insightwrite:essay-mode')
    localStorage.setItem(activeKey, taskId)
    activeTaskId.value = taskId
    await router.push(`/analyze/${taskId}`)
  } catch (error) {
    errorMessage.value = getErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

onMounted(async () => {
  loadCredits()
  // 检查活跃任务是否已完成
  if (activeTaskId.value) {
    try {
      const { getTaskStatus } = await import('@/api/analyze.js')
      const res = await getTaskStatus(activeTaskId.value)
      const s = res.data?.status
      if (s === 'completed' || s === 'failed') {
        localStorage.removeItem(activeKey)
        activeTaskId.value = ''
      }
    } catch (e) { /* 忽略 */ }
  }
})
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.home-view {
  display: grid; gap: var(--space-6);
}

.active-banner { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); border: 1px solid rgba(151,210,166,.24); border-radius: var(--radius-lg); padding: var(--space-4) var(--space-5); background: rgba(7,25,18,.52); font-size: var(--text-caption); box-shadow: var(--shadow-sm); backdrop-filter: blur(24px) saturate(1.18); color: var(--color-text); font-weight: 700; }
.active-banner a { font-weight: 800; color: var(--color-primary); white-space: nowrap; }

.home-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(13rem, 18rem);
  gap: var(--space-6);
  align-items: end;
}

.eyebrow {
  color: #f2c94c;
  font-size: 1.12rem;
  font-weight: 850;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1 {
  max-width: 49rem;
  margin-top: var(--space-2);
  color: var(--color-text);
  font-family: var(--font-serif);
  font-size: clamp(2rem, 4vw, 4rem);
  line-height: var(--leading-tight);
}

.status-tile {
  display: grid;
  gap: var(--space-1);
  border: 1px solid rgba(151,210,166,.24);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  background:
    linear-gradient(135deg, rgba(18,48,33,.68), rgba(3,14,10,.52)),
    rgba(7,25,18,.58);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(26px) saturate(1.2);
}

.status-tile.is-muted {
  background: rgba(7,25,18,.48);
}

.status-tile span {
  color: var(--color-text-muted);
  font-size: var(--text-small);
  font-weight: 800;
  text-transform: uppercase;
}

.status-tile strong {
  font-family: var(--font-sans);
  font-weight: 900;
  font-size: 2.75rem;
  line-height: 1;
  color: var(--color-text);
}

.status-tile small {
  color: var(--color-text-muted);
  line-height: var(--leading-normal);
}

@media (max-width: 780px) {
  .home-hero {
    grid-template-columns: 1fr;
  }

  .status-tile {
    max-width: 22rem;
  }
}
</style>
