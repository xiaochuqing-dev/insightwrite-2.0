<template>
  <section class="result-view">
    <header class="result-topbar">
      <div>
        <button class="back-btn" type="button" @click="$router.back()">← 返回</button>
        <h1>分析结果</h1>
      </div>

      <div class="topbar-actions">
        <span class="status-chip" :class="`status-chip--${status}`">{{ statusLabel }}</span>
        <span class="created-at">{{ createdAtLabel }}</span>
        <button class="favorite-button" type="button" :class="{ 'is-active': isFavorite }" @click="toggleFavorite">
          <span aria-hidden="true">{{ isFavorite ? '♥' : '♡' }}</span>
          {{ isFavorite ? '已收藏' : '收藏' }}
        </button>
        <button
          v-if="status === 'completed'"
          class="export-button"
          type="button"
          :disabled="isExporting"
          @click="exportAnalysisImage"
        >
          {{ isExporting ? '正在导出...' : '导出图片' }}
        </button>
      </div>
    </header>


      <div v-if="status === 'pending'" class="state-card state-card--pending" aria-live="polite">
        <div class="waiting-visual waiting-visual--queue" aria-hidden="true">
          <div class="queue-orbit">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <div class="draft-preview">
            <span class="draft-preview__tag">draft received</span>
            <i v-for="line in previewLines" :key="line" :style="{ width: line }"></i>
          </div>
        </div>

        <div class="state-copy">
          <p class="state-kicker">任务已提交</p>
          <strong>你的稿件已经进入分析队列。</strong>
          <p>{{ queueMessage }}</p>
          <div class="queue-steps" aria-label="等待步骤">
            <span class="is-done">接收原文</span>
            <span class="is-active">排队分配</span>
            <span>进入分析</span>
          </div>
          <div class="state-meta">
            <span>预计等待：{{ waitEstimate }}</span>
            <span>已等待：{{ elapsedLabel }}</span>
          </div>
        </div>
      </div>

      <div v-else-if="status === 'processing'" class="state-card state-card--processing" aria-live="polite">
        <div class="waiting-visual waiting-visual--scan" aria-hidden="true">
          <span class="pulse-ring"></span>
          <div class="scan-board">
            <i v-for="line in scanLines" :key="line" :style="{ width: line }"></i>
            <span class="scan-beam"></span>
          </div>
        </div>

        <div class="state-copy">
          <p class="state-kicker">AI 正在分析</p>
          <strong>{{ processingMessage }}</strong>
          <p>{{ processingDetail }}</p>
          <div class="phase-list" aria-label="分析阶段">
            <span v-for="phase in processingPhases" :key="phase" :class="{ 'is-active': phase === processingMessage }">
              {{ phase }}
            </span>
          </div>
          <div class="state-meta">
            <span>已等待：{{ elapsedLabel }}</span>
            <span>轮询间隔：1.5 秒</span>
          </div>
        </div>
      </div>

      <div v-else-if="status === 'failed'" class="state-card state-card--failed" role="alert">
        <div class="soft-alert" aria-hidden="true">!</div>

        <div class="state-copy">
          <p class="state-kicker">分析未完成</p>
          <strong>这次任务遇到了一点问题。</strong>
          <p>{{ errorMessage }}</p>
          <div class="failed-actions">
            <button class="retry-button" type="button" :disabled="isRetrying || !canRetry" @click="retryAnalyze">
              {{ retryLabel }}
            </button>
            <RouterLink to="/home" class="back-link">← 返回</RouterLink>
          </div>
          <small v-if="!canRetry">当前页面缺少原文内容，无法直接重新分析。</small>
        </div>
      </div>

      <template v-else>
        <div class="completed-shell">
          <div class="result-grid">
            <section class="result-column result-column--source">
              <div class="column-head">
                <div class="column-head-row">
                  <h2>原文标注</h2>
                  <span>{{ annotations.length }} 处</span>
                </div>
                <div class="color-legend">
                  <span class="legend-item"><i class="legend-dot legend-dot--error"></i>严重错误</span>
                  <span class="legend-item"><i class="legend-dot legend-dot--suggestion"></i>建议优化</span>
                  <span class="legend-item"><i class="legend-dot legend-dot--highlight"></i>表达亮点</span>
                  <span class="legend-item"><i class="legend-dot legend-dot--excellent"></i>非常出色</span>
                </div>
              </div>
              <div class="scroll-panel">
                <ColorMarkup
                  :text="result.original_text"
                  :annotations="annotations"
                  :selected-id="selectedAnnotationId"
                  @select="handleAnnotationSelect"
                />
              </div>
            </section>

            <section class="result-column result-column--revised">
              <div class="column-head">
                <h2>分析结果</h2>
                <span>{{ wordCount }} words</span>
              </div>

              <div ref="revisedPanelRef" class="scroll-panel revised-panel">
                <div v-if="analysisCards.intro" class="analysis-intro">{{ analysisCards.intro }}</div>
                <section
                  v-for="(card, i) in analysisCards.cards"
                  :key="i"
                  class="analysis-card"
                  :style="{ background: card.bg, borderColor: card.accent }"
                >
                  <h3 class="analysis-card__title" :style="{ color: card.accent }">{{ card.title }}</h3>
                  <div class="analysis-card__body">
                    <p v-for="(line, li) in cardParagraphs(card.content)" :key="li">{{ line }}</p>
                  </div>
                </section>

                <div v-if="selectedAnnotation" ref="focusCardRef" class="focus-card">
                  <span>{{ typeLabel(selectedAnnotation.type) }}</span>
                  <strong>{{ selectedAnnotation.suggestion }}</strong>
                </div>
              </div>
            </section>
          </div>

          <footer class="stats-bar">
            <strong>共发现 {{ changedCount }} 处修改</strong>
            <span v-for="item in statItems" :key="item.type" :class="`stat stat--${item.type}`">
              {{ item.label }} {{ item.count }}
            </span>
          </footer>

          <p v-if="exportError" class="export-error" role="alert">{{ exportError }}</p>

          <article
            ref="reportExportRef"
            class="export-report"
            :class="{ 'is-export-capturing': isExportCapturing }"
            aria-hidden="true"
          >
            <header class="export-report__header">
              <div>
                <p class="export-report__eyebrow">InsightWrite 分析报告</p>
                <h2>{{ modeLabel }}</h2>
              </div>
              <div class="export-report__meta">
                <span>{{ createdAtLabel }}</span>
                <span>{{ wordCount }} words</span>
                <span>{{ changedCount }} 处标注</span>
              </div>
            </header>

            <section class="export-report__summary">
              <div class="export-score">
                <span>分析概览</span>
                <strong>{{ scoreText }}</strong>
              </div>
              <div class="export-stats">
                <span v-for="item in statItems" :key="`export-${item.type}`" :class="`export-stat export-stat--${item.type}`">
                  {{ item.label }} {{ item.count }}
                </span>
              </div>
            </section>

            <section class="export-section">
              <div class="export-section__head">
                <h3>原文标注</h3>
                <div class="color-legend">
                  <span class="legend-item"><i class="legend-dot legend-dot--error"></i>严重错误</span>
                  <span class="legend-item"><i class="legend-dot legend-dot--suggestion"></i>建议优化</span>
                  <span class="legend-item"><i class="legend-dot legend-dot--highlight"></i>表达亮点</span>
                  <span class="legend-item"><i class="legend-dot legend-dot--excellent"></i>非常出色</span>
                </div>
              </div>
              <div class="export-source">
                <div
                  v-for="column in exportSourceColumns"
                  :key="`export-source-${column.index}`"
                  class="export-source__column"
                >
                  <ColorMarkup
                    :text="column.text"
                    :annotations="column.annotations"
                    :selected-id="selectedAnnotationId"
                  />
                </div>
              </div>
            </section>

            <section class="export-section">
              <div class="export-section__head">
                <h3>分析结果</h3>
                <span>{{ analysisCards.cards.length }} 张卡片</span>
              </div>
              <div class="export-analysis">
                <div v-if="analysisCards.intro" class="export-intro">{{ analysisCards.intro }}</div>
                <div
                  v-for="(column, columnIndex) in exportAnalysisColumns"
                  :key="`export-column-${columnIndex}`"
                  class="export-analysis__column"
                >
                  <section
                    v-for="card in column"
                    :key="`export-card-${card.index}`"
                    class="export-analysis-card"
                  >
                    <h4>{{ card.title }}</h4>
                    <div>
                      <p v-for="(line, li) in cardParagraphs(card.content)" :key="`export-line-${card.index}-${li}`">{{ line }}</p>
                    </div>
                  </section>
                </div>
              </div>
            </section>
          </article>
        </div>
      </template>

  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { toPng } from 'html-to-image'
import http from '@/api/index.js'
import { getTaskStatus, submitAnalyze } from '@/api/analyze.js'
import ColorMarkup from '@/components/ColorMarkup.vue'
import { countEnglishWords, getProcessingDetail, getProcessingPhases } from '@/utils/analysisProgress.js'

const route = useRoute()
const router = useRouter()
const pollDelay = 1500
const maxPollMs = 10 * 60 * 1000 // 10分钟兜底
const storageKey = 'insightwrite:essay-draft'

const status = ref('pending')
const result = ref({
  original_text: '',
  revised_text: '',
  annotations: [],
  summary: {}
})
const mode = ref('quality')
const createdAt = ref('')
const queuePosition = ref(null)
const estimatedSeconds = ref(null)
const errorMessage = ref('')
const taskWordCount = ref(null)
const isFavorite = ref(false)
const isRetrying = ref(false)
const selectedAnnotation = ref(null)
const revisedPanelRef = ref(null)
const focusCardRef = ref(null)
const reportExportRef = ref(null)
const isExporting = ref(false)
const isExportCapturing = ref(false)
const exportError = ref('')
const waitStartedAt = ref(Date.now())
const now = ref(Date.now())
let pollTimer = null
let clockTimer = null

const processingPhases = computed(() => getProcessingPhases(mode.value))

// 模拟原文预览线（排队中展示用）
const previewLines = computed(() => {
  const text = result.value.original_text || localStorage.getItem(storageKey) || ''
  const words = text.split(/\s+/)
  if (words.length < 3) return ['60%', '45%', '72%', '38%']
  const seed = text.length
  return words.slice(0, 6).map((_, i) => `${35 + ((seed * (i + 1)) % 50)}%`)
})
// 模拟扫描行（分析中展示用）
const scanLines = computed(() => {
  const lines = []
  for (let i = 0; i < 8; i++) lines.push(`${40 + (i * 7) % 50}%`)
  return lines
})
const processingDetail = computed(() => {
  return getProcessingDetail({ mode: mode.value, wordCount: progressWordCount.value })
})

const taskId = computed(() => route.params.taskId || route.params.id)
const annotations = computed(() =>
  (result.value.annotations || []).map((annotation, index) => ({
    ...annotation,
    id: annotation.id || `${annotation.start}-${annotation.end}-${annotation.type}-${index}`
  }))
)
const selectedAnnotationId = computed(() => selectedAnnotation.value?.id || '')
const statusLabel = computed(() => {
  const labels = {
    pending: '排队中',
    processing: '分析中',
    completed: '已完成',
    failed: '失败'
  }
  return labels[status.value] || '读取中'
})
const modeLabel = computed(() => {
  const labels = {
    basic: '写作诊断',
    quality: '高阶润色',
    deep: '佳作精读'
  }
  return labels[mode.value] || '分析结果'
})
const createdAtLabel = computed(() => {
  if (!createdAt.value) return '创建时间读取中'
  const date = new Date(createdAt.value)
  return Number.isNaN(date.getTime()) ? createdAt.value : date.toLocaleString()
})
const elapsedSeconds = computed(() => Math.max(0, Math.floor((now.value - waitStartedAt.value) / 1000)))
const elapsedLabel = computed(() => formatDuration(elapsedSeconds.value))
const queueMessage = computed(() => {
  if (Number.isFinite(queuePosition.value) && queuePosition.value > 0) {
    return `前面还有 ${queuePosition.value} 个任务，我们会在轮到你时自动开始。`
  }
  return '队列正在向前推进，稍等片刻即可进入分析阶段。'
})
const waitEstimate = computed(() => {
  if (Number.isFinite(estimatedSeconds.value) && estimatedSeconds.value > 0) {
    return `约 ${formatDuration(estimatedSeconds.value)}`
  }
  if (Number.isFinite(queuePosition.value) && queuePosition.value > 0) {
    return `约 ${Math.max(1, queuePosition.value * 2)} 分钟内`
  }
  return '通常不超过数分钟'
})
const processingMessage = computed(() => {
  const index = Math.floor(elapsedSeconds.value / 4) % processingPhases.value.length
  return processingPhases.value[index]
})
const revisedText = computed(() => result.value.revised_text || '')
const scoreText = computed(() => {
  const summary = result.value.summary || {}
  const score = summary.score ?? summary.total_score ?? summary.overall_score
  const numericScore = Number(score)
  if (Number.isFinite(numericScore) && numericScore > 0) return `${numericScore}/100`
  return `${wordCount.value} words`
})

const analysisCards = computed(() => {
  try {
    const t = revisedText.value
    if (!t.trim()) return { intro: '', cards: [] }
    const cards = []
    const parts = t.split(/(?=【)/)
    let intro = ''
    for (const part of parts) {
      const m = part.match(/^【(.+?)】\s*/)
      if (m) { cards.push({ title: m[1], content: part.slice(m[0].length).trim() }) }
      else { intro += part }
    }
    const palette = [
      { bg: 'rgba(16, 185, 129, 0.12)', accent: 'rgba(52, 211, 153, 0.5)' },
      { bg: 'rgba(56, 189, 248, 0.12)', accent: 'rgba(56, 189, 248, 0.5)' },
      { bg: 'rgba(168, 85, 247, 0.12)', accent: 'rgba(167, 139, 250, 0.5)' },
      { bg: 'rgba(251, 191, 36, 0.12)', accent: 'rgba(251, 191, 36, 0.5)' },
      { bg: 'rgba(244, 114, 182, 0.12)', accent: 'rgba(244, 114, 182, 0.45)' },
      { bg: 'rgba(6, 182, 212, 0.12)', accent: 'rgba(34, 211, 238, 0.5)' },
      { bg: 'rgba(132, 204, 22, 0.1)', accent: 'rgba(163, 230, 53, 0.45)' },
      { bg: 'rgba(239, 68, 68, 0.1)', accent: 'rgba(252, 129, 129, 0.4)' },
    ]
    let ci = 0
    const result = cards.map(c => {
      let color
      if (c.title.includes('总分') || c.title.includes('/')) {
        color = { bg: 'rgba(251, 191, 36, 0.15)', accent: 'rgba(251, 191, 36, 0.55)' }
      } else {
        color = palette[ci % palette.length]
        ci++
      }
      return { ...c, ...color }
    })
    // 没解析到任何卡片时，把intro整体包成一张卡片
    const introText = intro.trim()
    if (result.length === 0 && introText) {
      return { intro: '', cards: [{ title: '分析结果', content: introText, bg: 'transparent', accent: 'rgba(255,255,255,0.3)' }] }
    }
    return { intro: introText, cards: result }
  } catch (e) {
    const text = revisedText.value
    return { intro: '', cards: text ? [{ title: '分析结果', content: text, bg: 'transparent', accent: 'rgba(255,255,255,0.3)' }] : [] }
  }
})

const exportSourceColumns = computed(() => buildSourceColumns(result.value.original_text, annotations.value, 3))
const exportAnalysisColumns = computed(() => distributeCards(analysisCards.value.cards, 3))

function cardParagraphs(text) {
  if (!text) return []
  return text.split(/\n+/).map(l => l.trim()).filter(Boolean)
}

function distributeCards(cards, columnCount) {
  const columns = Array.from({ length: columnCount }, () => [])
  const columnWeights = Array.from({ length: columnCount }, () => 0)

  cards.forEach((card, index) => {
    const target = columnWeights.indexOf(Math.min(...columnWeights))
    const nextCard = { ...card, index }
    columns[target].push(nextCard)
    columnWeights[target] += estimateCardWeight(nextCard)
  })

  return columns
}

function estimateCardWeight(card) {
  const paragraphs = cardParagraphs(card.content)
  const textLength = paragraphs.join('').length
  return 3 + Math.ceil((card.title?.length || 0) / 18) + paragraphs.length * 1.8 + Math.ceil(textLength / 72)
}

function buildSourceColumns(text, sourceAnnotations, columnCount) {
  const value = text || ''
  const ranges = buildTextRanges(value, columnCount)

  return ranges.map(([start, end], index) => ({
    index,
    text: value.slice(start, end),
    annotations: sourceAnnotations
      .filter(annotation => annotation.start < end && annotation.end > start)
      .map(annotation => ({
        ...annotation,
        start: Math.max(annotation.start, start) - start,
        end: Math.min(annotation.end, end) - start
      }))
  }))
}

function buildTextRanges(text, columnCount) {
  if (!text) return Array.from({ length: columnCount }, () => [0, 0])

  const ranges = []
  let start = 0

  for (let i = 1; i < columnCount; i++) {
    const target = Math.round((text.length * i) / columnCount)
    const end = findReadableSplit(text, target, start)
    ranges.push([start, end])
    start = skipSplitWhitespace(text, end)
  }

  ranges.push([start, text.length])
  return ranges
}

function findReadableSplit(text, target, minStart) {
  const windowSize = 180
  const lower = Math.max(minStart + 1, target - windowSize)
  const upper = Math.min(text.length - 1, target + windowSize)
  const splitPattern = /[\s,.;:!?)]/
  let best = target
  let bestDistance = Number.POSITIVE_INFINITY

  for (let i = lower; i <= upper; i++) {
    if (!splitPattern.test(text[i])) continue
    const distance = Math.abs(i - target)
    if (distance < bestDistance) {
      best = i + 1
      bestDistance = distance
    }
  }

  return Math.max(minStart + 1, Math.min(best, text.length))
}

function skipSplitWhitespace(text, index) {
  let next = index
  while (next < text.length && /\s/.test(text[next])) next++
  return next
}

const wordCount = computed(() => {
  const summaryCount = Number(result.value.summary?.word_count)
  if (Number.isFinite(summaryCount) && summaryCount > 0) return summaryCount
  const responseCount = Number(taskWordCount.value)
  if (Number.isFinite(responseCount) && responseCount > 0) return responseCount
  return countEnglishWords(result.value.original_text)
})
const progressSourceText = computed(() => result.value.original_text || localStorage.getItem(storageKey) || '')
const progressWordCount = computed(() => {
  const count = wordCount.value
  return count > 0 ? count : countEnglishWords(progressSourceText.value)
})
const changedCount = computed(() => annotations.value.length)
const statItems = computed(() => [
  { type: 'error', label: '严重错误', count: countByType('error') },
  { type: 'suggestion', label: '建议优化', count: countByType('suggestion') },
  { type: 'highlight', label: '表达亮点', count: countByType('highlight') },
  { type: 'excellent', label: '非常出色', count: countByType('excellent') }
])
const retrySourceText = computed(() => result.value.original_text || localStorage.getItem(storageKey) || '')
const canRetry = computed(() => Boolean(retrySourceText.value.trim()))
const retryLabel = computed(() => {
  if (isRetrying.value) return '正在重新提交...'
  if (!canRetry.value) return '无法重新分析'
  return '重新分析'
})

function countByType(type) {
  return annotations.value.filter(annotation => annotation.type === type).length
}

function typeLabel(type) {
  const labels = {
    error: '严重错误',
    suggestion: '建议优化',
    highlight: '表达亮点',
    excellent: '非常出色'
  }
  return labels[type] || '标注'
}

function formatDuration(totalSeconds) {
  if (totalSeconds < 60) return `${totalSeconds} 秒`
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return seconds ? `${minutes} 分 ${seconds} 秒` : `${minutes} 分钟`
}

function readNumber(data, keys) {
  for (const key of keys) {
    const value = Number(data?.[key])
    if (Number.isFinite(value)) return value
  }
  return null
}

function normalizeResponse(data) {
  const nextStatus = data.status || 'pending'
  status.value = nextStatus

  if (data.mode) mode.value = data.mode
  if (data.created_at) createdAt.value = data.created_at
  if (typeof data.is_favorite === 'boolean') isFavorite.value = data.is_favorite
  const responseWordCount = Number(data.word_count)
  if (Number.isFinite(responseWordCount) && responseWordCount > 0) taskWordCount.value = responseWordCount

  queuePosition.value = readNumber(data, ['queue_position', 'queue_count', 'queue_ahead'])
  estimatedSeconds.value = readNumber(data, ['estimated_seconds', 'estimated_wait_seconds', 'eta_seconds'])

  if (data.result) {
    result.value = {
      original_text: data.result.original_text || result.value.original_text,
      revised_text: data.result.revised_text || result.value.revised_text,
      annotations: data.result.annotations || result.value.annotations,
      summary: data.result.summary || result.value.summary
    }
    if (data.result.original_text) localStorage.setItem(storageKey, data.result.original_text)
  }

  if (nextStatus === 'completed') {
    localStorage.removeItem('insightwrite:active-task')
    stopPolling()
  }

  if (nextStatus === 'failed') {
    errorMessage.value = data.error_message || '任务处理失败，请稍后重试。'
    localStorage.removeItem('insightwrite:active-task')
    stopPolling()
  }
}

async function fetchStatus() {
  if (!taskId.value) {
    status.value = 'failed'
    errorMessage.value = '缺少任务 ID，无法读取分析结果。'
    return
  }

  // 超过最大等待时间，兜底报错
  if (Date.now() - waitStartedAt.value > maxPollMs) {
    status.value = 'failed'
    errorMessage.value = '分析超时（超过10分钟），请返回重试。'
    stopPolling()
    return
  }

  try {
    const response = await getTaskStatus(taskId.value)
    normalizeResponse(response.data || {})
  } catch (error) {
    status.value = 'failed'
    errorMessage.value = error?.response?.data?.error || '读取分析结果失败。'
    stopPolling()
  }
}

function startPolling() {
  stopPolling()
  waitStartedAt.value = Date.now()
  now.value = Date.now()
  selectedAnnotation.value = null
  fetchStatus()
  pollTimer = window.setInterval(fetchStatus, pollDelay)
}

function stopPolling() {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

function handleVisibilityChange() {
  if (document.visibilityState === 'visible' && status.value !== 'completed' && status.value !== 'failed') {
    fetchStatus()
  }
}

async function handleAnnotationSelect(annotation) {
  selectedAnnotation.value = annotation
  await nextTick()
  focusCardRef.value?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
}

async function retryAnalyze() {
  if (!canRetry.value || isRetrying.value) return

  isRetrying.value = true
  errorMessage.value = ''

  try {
    const response = await submitAnalyze(retrySourceText.value.trim(), mode.value)
    const nextTaskId = response.data?.task_id
    if (!nextTaskId) throw new Error('Missing task id')
    status.value = 'pending'
    taskWordCount.value = null
    await router.replace(`/analyze/${nextTaskId}`)
  } catch (error) {
    status.value = 'failed'
    errorMessage.value = error?.response?.data?.error || '重新提交失败，请稍后再试。'
  } finally {
    isRetrying.value = false
  }
}

async function toggleFavorite() {
  if (!taskId.value) return
  const nextState = !isFavorite.value
  isFavorite.value = nextState

  try {
    const response = nextState
      ? await http.post(`/history/${taskId.value}/favorite`)
      : await http.delete(`/history/${taskId.value}/favorite`)
    if (typeof response.data?.is_favorite === 'boolean') {
      isFavorite.value = response.data.is_favorite
    }
  } catch (error) {
    isFavorite.value = !nextState
  }
}

async function exportAnalysisImage() {
  if (!reportExportRef.value || isExporting.value) return

  isExporting.value = true
  exportError.value = ''

  try {
    isExportCapturing.value = true
    await nextTick()
    await document.fonts?.ready
    const node = reportExportRef.value
    const exportWidth = Math.ceil(node.scrollWidth)
    const exportHeight = Math.ceil(node.scrollHeight)
    const dataUrl = await toPng(node, {
      backgroundColor: '#07140f',
      cacheBust: true,
      pixelRatio: getExportPixelRatio(exportHeight),
      width: exportWidth,
      height: exportHeight,
      style: {
        width: `${exportWidth}px`,
        height: 'auto'
      }
    })

    const link = document.createElement('a')
    link.download = buildExportFilename()
    link.href = dataUrl
    link.click()
  } catch (error) {
    exportError.value = '图片导出失败，请稍后重试。'
  } finally {
    isExportCapturing.value = false
    isExporting.value = false
  }
}

function buildExportFilename() {
  const date = new Date()
  const stamp = [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0')
  ].join('')
  return `InsightWrite-analysis-${taskId.value || stamp}.png`
}

function getExportPixelRatio(height) {
  if (!Number.isFinite(height) || height <= 0) return 2
  return Math.min(2, Math.max(1, 28000 / height))
}

watch(taskId, () => {
  status.value = 'pending'
  result.value = {
    original_text: '',
    revised_text: '',
    annotations: [],
    summary: {}
  }
  taskWordCount.value = null
  startPolling()
})

function cancelTask() {
  localStorage.removeItem('insightwrite:active-task')
  stopPolling()
}

onMounted(() => {
  startPolling()
  clockTimer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onBeforeUnmount(() => {
  stopPolling()
  if (clockTimer) window.clearInterval(clockTimer)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.result-view {
  display: grid;
  gap: var(--space-6);
}

.result-topbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: var(--space-6);
}

.eyebrow {
  color: var(--color-primary);
  font-size: var(--text-caption);
  font-weight: 850;
  text-transform: uppercase;
}

h1,
h2 {
  font-family: var(--font-serif);
  line-height: var(--leading-tight);
}

h1 {
  margin-top: var(--space-1);
  font-size: clamp(1.8rem, 3vw, 3.3rem);
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.status-chip,
.favorite-button,
.export-button {
  min-height: 2.75rem;
  display: inline-flex;
  align-items: center;
  border: 1px solid rgba(151, 210, 166, 0.26);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: rgba(7, 25, 18, 0.58);
  font-weight: 800;
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(18px) saturate(1.14);
}

.status-chip {
  color: var(--color-text-muted);
}

.status-chip--completed {
  background: var(--color-secondary-50);
  color: var(--color-secondary);
}

.status-chip--processing,
.status-chip--pending {
  background: var(--color-primary-50);
  color: var(--color-primary);
}

.status-chip--failed {
  background: var(--color-danger-50);
  color: var(--color-danger);
}

.created-at {
  color: var(--color-text-muted);
  font-size: var(--text-caption);
}

.favorite-button {
  gap: var(--space-2);
  color: var(--color-text-muted);
}

.favorite-button.is-active {
  color: var(--color-danger);
}

.export-button {
  color: #07140f;
  background: linear-gradient(135deg, #e9ffcf, #20f3e8);
}

.export-button:disabled {
  cursor: wait;
  opacity: 0.68;
}

.state-card {
  min-height: 30rem;
  display: grid;
  grid-template-columns: auto minmax(0, 34rem);
  align-items: center;
  justify-content: center;
  gap: var(--space-8);
  border: 1px solid rgba(151, 210, 166, 0.26);
  border-radius: var(--radius-lg);
  padding: var(--space-10);
  background:
    linear-gradient(135deg, rgba(7, 25, 18, 0.62), rgba(12, 66, 57, 0.44)),
    rgba(7, 25, 18, 0.52);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(28px) saturate(1.2);
}

.state-card--pending {
  background:
    linear-gradient(135deg, rgba(7, 25, 18, 0.62), rgba(11, 86, 55, 0.42)),
    rgba(7, 25, 18, 0.54);
}

.state-card--failed {
  background:
    linear-gradient(135deg, rgba(7, 25, 18, 0.64), rgba(88, 25, 29, 0.42)),
    rgba(7, 25, 18, 0.54);
}

.state-copy {
  display: grid;
  gap: var(--space-3);
}

.state-kicker {
  color: var(--color-primary);
  font-size: var(--text-caption);
  font-weight: 900;
  text-transform: uppercase;
}

.state-copy strong {
  display: block;
  font-family: var(--font-serif);
  font-size: clamp(1.5rem, 3vw, 2.25rem);
  line-height: var(--leading-tight);
}

.state-copy p {
  color: var(--color-text-muted);
}

.state-meta,
.failed-actions,
.phase-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.state-meta span,
.phase-list span {
  border: 1px solid rgba(226, 239, 217, 0.2);
  border-radius: 999px;
  padding: var(--space-1) var(--space-3);
  background: rgba(7, 25, 18, 0.42);
  color: rgba(243, 248, 236, 0.86);
  font-size: var(--text-small);
  font-weight: 800;
}

.phase-list span.is-active {
  border-color: rgba(32, 243, 232, 0.42);
  background: rgba(32, 243, 232, 0.14);
  color: #8ffcf5;
}

.queue-orbit {
  position: relative;
  width: 8.5rem;
  height: 8.5rem;
  border: 1px solid rgba(16, 185, 129, 0.22);
  border-radius: 50%;
  background: rgba(7, 25, 18, 0.42);
}

.queue-orbit span {
  position: absolute;
  width: 1rem;
  height: 1rem;
  border-radius: 50%;
  background: var(--color-secondary);
  animation: queuePulse 2.4s var(--ease-out) infinite;
}

.queue-orbit span:nth-child(1) {
  top: 1.1rem;
  left: 3.7rem;
}

.queue-orbit span:nth-child(2) {
  right: 1.25rem;
  bottom: 2rem;
  animation-delay: 180ms;
}

.queue-orbit span:nth-child(3) {
  bottom: 2rem;
  left: 1.25rem;
  animation-delay: 360ms;
}

.pulse-ring {
  width: 6.25rem;
  height: 6.25rem;
  border: 0.42rem solid var(--color-primary-100);
  border-top-color: var(--color-primary);
  border-right-color: var(--color-secondary);
  border-radius: 50%;
  animation: spin 900ms linear infinite;
}

/* GPT Day 11 新增：等待视觉增强 */
.waiting-visual { flex-shrink: 0; display: grid; place-items: center; }
.waiting-visual--queue { gap: var(--space-5); }
.waiting-visual--scan { gap: var(--space-4); }

.draft-preview {
  display: grid; gap: 6px; width: 100%; max-width: 16rem;
  border: 1px solid rgba(151, 210, 166, 0.24); border-radius: var(--radius-md);
  padding: var(--space-4); background: rgba(7, 25, 18, 0.46);
  box-shadow: inset 0 1px rgba(255, 255, 255, 0.06);
}
.draft-preview__tag {
  color: var(--color-primary); font-size: var(--text-small); font-weight: 800;
  text-transform: uppercase; margin-bottom: 2px;
}
.draft-preview i {
  display: block; height: 6px; border-radius: 999px;
  background: linear-gradient(90deg, rgba(32, 243, 232, 0.4), rgba(226, 239, 217, 0.18));
}

.queue-steps {
  display: flex; gap: var(--space-2); align-items: center;
}
.queue-steps span {
  font-size: var(--text-small); color: rgba(243, 248, 236, 0.86);
  padding: 2px 8px; border-radius: 999px;
  border: 1px solid rgba(226, 239, 217, 0.2); background: rgba(7, 25, 18, 0.42);
}
.queue-steps span.is-done {
  color: #9fffc6; border-color: rgba(34, 197, 94, 0.42); background: rgba(34, 197, 94, 0.14);
}
.queue-steps span.is-active {
  color: #8ffcf5; border-color: rgba(32, 243, 232, 0.42); background: rgba(32, 243, 232, 0.14); font-weight: 800;
}

.scan-board {
  position: relative; overflow: hidden; width: 100%; max-width: 18rem;
  display: grid; gap: 5px; border: 1px solid rgba(151, 210, 166, 0.24);
  border-radius: var(--radius-md); padding: var(--space-4); background: rgba(7, 25, 18, 0.46);
  box-shadow: inset 0 1px rgba(255, 255, 255, 0.06);
}
.scan-board i {
  display: block; height: 5px; border-radius: 999px;
  background: rgba(226, 239, 217, 0.28); opacity: .8;
}
.scan-beam {
  position: absolute; left: 0; top: 0; width: 100%; height: 3px;
  background: linear-gradient(90deg, transparent, var(--color-primary), transparent);
  animation: scanDown 2.2s var(--ease-out) infinite;
}
@keyframes scanDown {
  0% { top: 0; opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { top: calc(100% - 3px); opacity: 0; }
}

.soft-alert {
  width: 6.25rem;
  height: 6.25rem;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--color-danger-50);
  color: var(--color-danger);
  font-family: var(--font-serif);
  font-size: 3rem;
  font-weight: 900;
}

.retry-button {
  min-height: 3rem;
  border-radius: var(--radius-md);
  padding: 0 var(--space-5);
  background: var(--gray-900);
  color: white;
  font-weight: 850;
  box-shadow: var(--shadow-md);
}

.failed-actions a {
  font-weight: 800;
}

.state-copy small {
  color: var(--color-text-subtle);
}

.completed-shell {
  display: grid;
  gap: var(--space-6);
}

.result-grid {
  display: grid;
  grid-template-columns: 47fr 53fr;
  align-items: start;
  gap: var(--space-5);
}

.result-column {
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(151, 210, 166, 0.24);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(34px) saturate(1.28);
}

.column-head {
  min-height: 4.25rem;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  border-bottom: 1px solid var(--color-border);
  padding: var(--space-3) var(--space-5);
  background: rgba(7, 25, 18, 0.42);
}

.column-head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.column-head h2 {
  font-size: var(--text-heading);
  color: var(--color-text);
}

.column-head-row span {
  color: var(--color-text-muted);
  font-size: var(--text-caption);
  font-weight: 800;
}

.color-legend {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-dot--error { background: #dc2626; }
.legend-dot--suggestion { background: #d97706; }
.legend-dot--highlight { background: #059669; }
.legend-dot--excellent { background: #2563eb; }

.scroll-panel {
  max-height: calc(100vh - 17.5rem);
  min-height: 34rem;
  overflow-y: auto;
  padding: var(--space-6);
}

.revised-panel {
  display: grid;
  align-content: start;
  gap: var(--space-4);
}

.analysis-intro {
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.92rem;
  line-height: var(--leading-relaxed);
  padding: 0 var(--space-1);
}

.analysis-card {
  display: grid;
  gap: var(--space-3);
  border-radius: var(--radius-md);
  border: 1px solid;
  padding: var(--space-4);
  backdrop-filter: blur(20px) saturate(1.1);
}

.analysis-card__title {
  font-family: var(--font-sans);
  font-size: 0.95rem;
  font-weight: 800;
  margin: 0;
}

.analysis-card__body {
  color: rgba(255, 255, 255, 0.85);
  font-family: var(--font-serif);
  font-size: 0.95rem;
  line-height: var(--leading-relaxed);
}

.analysis-card__body p { margin: 0; }
.analysis-card__body p + p { margin-top: var(--space-3); }

.focus-card {
  display: grid;
  gap: var(--space-2);
  border: 1px solid rgba(32, 243, 232, 0.24);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  background: var(--color-primary-50);
}

.focus-card span {
  color: var(--color-primary);
  font-size: var(--text-small);
  font-weight: 900;
}

.focus-card strong {
  color: var(--color-text);
  line-height: var(--leading-relaxed);
}

.stats-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-5);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(28px) saturate(1.22);
}

.stats-bar strong {
  margin-right: auto;
}

.export-error {
  color: var(--color-danger);
  font-size: var(--text-caption);
  font-weight: 800;
}

.stat {
  border-radius: 999px;
  padding: var(--space-1) var(--space-3);
  font-size: var(--text-small);
  font-weight: 900;
}

.stat--error {
  background: #fee2e2;
  color: #dc2626;
}

.stat--suggestion {
  background: #fef3c7;
  color: #d97706;
}

.stat--highlight {
  background: #d1fae5;
  color: #059669;
}

.stat--excellent {
  background: #dbeafe;
  color: #2563eb;
}

.export-report {
  position: fixed;
  left: 0;
  top: 0;
  z-index: -1;
  width: 1840px;
  display: grid;
  gap: 26px;
  padding: 46px 54px 58px;
  background:
    radial-gradient(circle at 8% 0%, rgba(32, 243, 232, 0.14), transparent 24rem),
    linear-gradient(135deg, rgba(233, 255, 207, 0.055), transparent 42rem),
    #07140f;
  color: #f3f8ec;
  font-family: var(--font-sans);
  overflow: visible;
  pointer-events: none;
  transform: translateX(-150vw);
}

.export-report.is-export-capturing {
  transform: none;
}

.export-report *,
.export-report *::before,
.export-report *::after {
  box-sizing: border-box;
}

.export-report__header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 28px;
  border-bottom: 2px solid rgba(32, 243, 232, 0.26);
  padding-bottom: 24px;
}

.export-report__eyebrow {
  margin: 0 0 6px;
  color: #20f3e8;
  font-size: 19px;
  font-weight: 900;
  letter-spacing: 0;
}

.export-report__header h2 {
  margin: 0;
  color: #f3f8ec;
  font-family: var(--font-serif);
  font-size: 58px;
}

.export-report__meta,
.export-report__summary,
.export-stats,
.export-section__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.export-report__meta span,
.export-section__head span {
  color: rgba(226, 239, 217, 0.78);
  font-size: 19px;
  font-weight: 800;
}

.export-report__summary {
  justify-content: space-between;
  border: 1px solid rgba(151, 210, 166, 0.24);
  border-radius: var(--radius-md);
  padding: 20px 24px;
  background: rgba(7, 25, 18, 0.76);
}

.export-score {
  display: grid;
  gap: 4px;
}

.export-score span {
  color: rgba(226, 239, 217, 0.72);
  font-size: 18px;
  font-weight: 800;
}

.export-score strong {
  color: #f9d65c;
  font-size: 34px;
  font-weight: 950;
}

.export-stat {
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.06);
  font-size: 18px;
  font-weight: 900;
}

.export-stat--error { color: #fca5a5; }
.export-stat--suggestion { color: #fcd34d; }
.export-stat--highlight { color: #6ee7b7; }
.export-stat--excellent { color: #93c5fd; }

.export-section {
  display: grid;
  gap: 16px;
}

.export-section__head {
  justify-content: space-between;
}

.export-section__head h3 {
  margin: 0;
  border-left: 4px solid #20f3e8;
  padding-left: 12px;
  color: #f3f8ec;
  font-family: var(--font-serif);
  font-size: 34px;
}

.export-source,
.export-analysis-card,
.export-intro {
  border: 1px solid rgba(151, 210, 166, 0.22);
  border-radius: var(--radius-md);
  background: rgba(7, 25, 18, 0.72);
  box-shadow: inset 0 1px rgba(255, 255, 255, 0.04);
}

.export-source {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 34px;
  padding: 28px 30px;
  border-color: rgba(32, 243, 232, 0.26);
  background:
    linear-gradient(90deg, rgba(32, 243, 232, 0.07), transparent 34%),
    rgba(7, 25, 18, 0.76);
}

.export-source__column {
  min-width: 0;
}

.export-source__column + .export-source__column {
  border-left: 1px solid rgba(151, 210, 166, 0.18);
  padding-left: 34px;
}

.export-source__column :deep(.markup-document) {
  font-size: 21px;
  line-height: 1.78;
}

.export-analysis {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: 18px;
  align-items: start;
}

.export-analysis__column {
  display: grid;
  align-content: start;
  gap: 18px;
  min-width: 0;
}

.export-intro {
  grid-column: 1 / -1;
  padding: 20px 24px;
  color: rgba(243, 248, 236, 0.82);
  font-size: 20px;
  line-height: 1.7;
}

.export-analysis-card {
  display: grid;
  width: 100%;
  gap: 12px;
  padding: 20px 22px 22px;
  background:
    linear-gradient(180deg, rgba(32, 243, 232, 0.055), transparent 5rem),
    rgba(7, 25, 18, 0.72);
}

.export-analysis-card h4 {
  margin: 0;
  color: #20f3e8;
  font-size: 23px;
  font-weight: 950;
}

.export-analysis-card p {
  margin: 0;
  color: rgba(243, 248, 236, 0.86);
  font-family: var(--font-serif);
  font-size: 20px;
  line-height: 1.68;
}

.export-analysis-card p + p {
  margin-top: 10px;
}

.state-fade-enter-active,
.state-fade-leave-active {
  transition:
    opacity var(--duration-slow) var(--ease-out),
    transform var(--duration-slow) var(--ease-out);
}

.state-fade-enter-from,
.state-fade-leave-to {
  opacity: 0;
  transform: translateY(0.35rem);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes queuePulse {
  0%,
  100% {
    transform: scale(0.72);
    opacity: 0.42;
  }

  50% {
    transform: scale(1);
    opacity: 1;
  }
}

@media (max-width: 980px) {
  .result-grid {
    grid-template-columns: 1fr;
  }

  .scroll-panel {
    max-height: none;
    min-height: 24rem;
  }
}

@media (max-width: 720px) {
  .result-topbar,
  .topbar-actions,
  .state-card {
    align-items: stretch;
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .topbar-actions {
    display: grid;
  }

  .state-card {
    justify-content: stretch;
    padding: var(--space-6);
  }

  .queue-orbit,
  .pulse-ring,
  .soft-alert {
    justify-self: center;
  }
}

.cancel-link {
  display: inline-block;
  margin-top: var(--space-3);
  color: var(--color-text-muted);
  font-size: var(--text-small);
  text-decoration: underline;
}

.back-btn {
  height: 2.5rem;
  padding: 0 var(--space-4);
  border: 1px solid rgba(151, 210, 166, 0.26);
  border-radius: var(--radius-md);
  background: rgba(7, 25, 18, 0.58);
  color: var(--color-text);
  font-weight: 700;
  font-size: var(--text-caption);
  margin-bottom: var(--space-1);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(18px) saturate(1.18);
}

.back-btn:hover {
  border-color: rgba(32, 243, 232, 0.42);
  background: rgba(14, 45, 31, 0.72);
  color: #e9ffcf;
}
</style>
