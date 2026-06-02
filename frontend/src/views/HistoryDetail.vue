<template>
  <section class="detail-view">
    <header class="page-head">
      <div>
        <RouterLink to="/history" class="back-link">← 返回</RouterLink>
        <h1>分析结果</h1>
      </div>
      <div class="page-actions">
        <span class="date-text">{{ formatDate(createdAt) }}</span>
        <button class="export-button" type="button" :disabled="isExporting" @click="exportAnalysisImage">
          {{ isExporting ? '正在导出...' : '导出图片' }}
        </button>
      </div>
    </header>

    <div v-if="isLoading" class="state-card">
      <span class="pulse-ring" aria-hidden="true"></span>
      <p>正在加载分析结果</p>
    </div>

    <div v-else-if="errorMsg" class="state-card state-card--failed">
      <strong>加载失败</strong>
      <p>{{ errorMsg }}</p>
      <RouterLink to="/history" class="back-link">← 返回</RouterLink>
    </div>

    <template v-else>
      <div class="result-grid">
        <section class="result-column">
          <div class="column-head">
	            <div class="column-head-row">
	              <h2>原文标注</h2>
	              <span>{{ annCount }} 处</span>
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
              :text="originalText"
              :annotations="annotations"
              :selected-id="selectedId"
              @select="handleSelect"
            />
          </div>
        </section>
        <section class="result-column">
          <div class="column-head"><h2>分析结果</h2><span>{{ wordCount }} 词</span></div>
          <div ref="revisedRef" class="scroll-panel revised-panel">
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
            <div v-if="selectedAnn" class="focus-card">
              <span>{{ typeLabel(selectedAnn.type) }}</span>
              <strong>{{ selectedAnn.suggestion }}</strong>
            </div>
          </div>
        </section>
      </div>

      <footer class="stats-bar">
        <strong>共 {{ annCount }} 处修改</strong>
        <span class="stat" v-for="s in statItems" :key="s.type" :class="`stat--${s.type}`">{{ s.label }} {{ s.count }}</span>
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
            <h2>{{ modeLabel(mode) || '分析结果' }}</h2>
          </div>
          <div class="export-report__meta">
            <span>{{ formatDate(createdAt) }}</span>
            <span>{{ wordCount }} 词</span>
            <span>{{ annCount }} 处标注</span>
          </div>
        </header>

        <section class="export-report__summary">
          <div class="export-score">
            <span>分析概览</span>
            <strong>{{ scoreText }}</strong>
          </div>
          <div class="export-stats">
            <span v-for="s in statItems" :key="`export-${s.type}`" :class="`export-stat export-stat--${s.type}`">
              {{ s.label }} {{ s.count }}
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
                :selected-id="selectedId"
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
    </template>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { toPng } from 'html-to-image'
import { getTaskStatus } from '@/api/analyze.js'
import ColorMarkup from '@/components/ColorMarkup.vue'
import { formatDate, modeLabel } from '@/utils/display.js'

const route = useRoute()
const taskId = computed(() => route.params.taskId)

const isLoading = ref(true)
const errorMsg = ref('')
const originalText = ref('')
const revisedText = ref('')
const mode = ref('')
const createdAt = ref('')
const wordCount = ref(0)
const annotations = ref([])
const selectedAnn = ref(null)
const selectedId = ref('')
const revisedRef = ref(null)
const reportExportRef = ref(null)
const isExporting = ref(false)
const isExportCapturing = ref(false)
const exportError = ref('')
const summary = ref({})

const annCount = computed(() => annotations.value.length)
const scoreText = computed(() => {
  const score = summary.value.score ?? summary.value.total_score ?? summary.value.overall_score
  const numericScore = Number(score)
  if (Number.isFinite(numericScore) && numericScore > 0) return `${numericScore}/100`
  return `${wordCount.value} 词`
})
const analysisCards = computed(() => {
  const t = revisedText.value || ''
  if (!t.trim()) return { intro: '', cards: [] }

  const cards = []
  const parts = t.split(/(?=【)/)
  let intro = ''
  for (const part of parts) {
    const m = part.match(/^【(.+?)】\s*/)
    if (m) {
      cards.push({ title: m[1], content: part.slice(m[0].length).trim() })
    } else {
      intro += part
    }
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
  const introText = intro.trim()
  if (result.length === 0 && introText) {
    return { intro: '', cards: [{ title: '分析结果', content: introText, bg: 'transparent', accent: 'rgba(255,255,255,0.3)' }] }
  }
  return { intro: introText, cards: result }
})

const exportSourceColumns = computed(() => buildSourceColumns(originalText.value, annotations.value, 3))
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

const statItems = computed(() => [
  { type: 'error', label: '严重错误', count: annotations.value.filter(a => a.type === 'error').length },
  { type: 'suggestion', label: '建议优化', count: annotations.value.filter(a => a.type === 'suggestion').length },
  { type: 'highlight', label: '表达亮点', count: annotations.value.filter(a => a.type === 'highlight').length },
  { type: 'excellent', label: '非常出色', count: annotations.value.filter(a => a.type === 'excellent').length },
])

function typeLabel(t) {
  const m = { error: '严重错误', suggestion: '建议优化', highlight: '表达亮点', excellent: '非常出色' }
  return m[t] || '修改建议'
}

async function handleSelect(ann) {
  selectedAnn.value = ann
  selectedId.value = ann.id || ''
  await nextTick()
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

onMounted(async () => {
  try {
    const res = await getTaskStatus(taskId.value)
    const d = res.data
    mode.value = d.mode || ''
    createdAt.value = d.created_at || ''
    if (d.result) {
      originalText.value = d.result.original_text || ''
      revisedText.value = d.result.revised_text || ''
      annotations.value = d.result.annotations || []
      summary.value = d.result.summary || {}
      wordCount.value = d.result.summary?.word_count || 0
    }
  } catch (e) {
    errorMsg.value = e?.response?.data?.error || '加载失败'
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.detail-view {
  display: grid; gap: var(--space-6);
}
.page-head { display: flex; align-items: end; justify-content: space-between; gap: var(--space-4); }
h1 { font-family: var(--font-serif); font-size: clamp(1.6rem, 3vw, 2.8rem); color: var(--gray-900); margin-top: var(--space-1); }
.date-text { color: var(--color-text-muted); font-size: var(--text-caption); }
.page-actions { display: flex; align-items: center; gap: var(--space-3); }
.export-button {
  min-height: 2.75rem;
  display: inline-flex;
  align-items: center;
  border: 1px solid rgba(151, 210, 166, 0.26);
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: linear-gradient(135deg, #e9ffcf, #20f3e8);
  color: #07140f;
  font-weight: 850;
  box-shadow: var(--shadow-sm);
}
.export-button:disabled { cursor: wait; opacity: 0.68; }

.state-card { min-height: 22rem; display: grid; place-items: center; gap: var(--space-4); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: var(--space-8); background: var(--color-surface); text-align: center; box-shadow: var(--shadow-md); }
.state-card--failed strong { color: var(--color-danger); }
.pulse-ring { width: 2.5rem; height: 2.5rem; border: 3px solid var(--color-primary-100); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 900ms linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.result-grid { display: grid; grid-template-columns: 47fr 53fr; align-items: start; gap: var(--space-5); }
.result-column { min-width: 0; overflow: hidden; border: 1px solid rgba(151,210,166,.24); border-radius: var(--radius-lg); background: var(--color-surface); box-shadow: var(--shadow-md); backdrop-filter: blur(34px) saturate(1.28); }
.column-head { min-height: 4rem; display: flex; flex-direction: column; gap: var(--space-2); padding: var(--space-3) var(--space-5); border-bottom: 1px solid var(--color-border); background: rgba(7,25,18,.42); }
.column-head-row { display: flex; align-items: center; justify-content: space-between; }
.column-head h2 { font-family: var(--font-serif); font-size: var(--text-heading); }
.column-head-row span { color: var(--color-text-muted); font-size: var(--text-caption); font-weight: 800; }
.color-legend { display: flex; flex-wrap: wrap; gap: var(--space-3); }
.legend-item { display: inline-flex; align-items: center; gap: 4px; font-size: 0.75rem; font-weight: 700; color: var(--color-text-muted); }
.legend-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.legend-dot--error { background: #dc2626; }
.legend-dot--suggestion { background: #d97706; }
.legend-dot--highlight { background: #059669; }
.legend-dot--excellent { background: #2563eb; }
.scroll-panel { max-height: calc(100vh - 17rem); min-height: 28rem; overflow-y: auto; padding: var(--space-6); }
.revised-panel { display: grid; align-content: start; gap: var(--space-4); }
.analysis-intro { color: rgba(255, 255, 255, 0.6); font-size: 0.92rem; line-height: var(--leading-relaxed); padding: 0 var(--space-1); }
.analysis-card { display: grid; gap: var(--space-3); border-radius: var(--radius-md); border: 1px solid; padding: var(--space-4); backdrop-filter: blur(20px) saturate(1.1); }
.analysis-card__title { font-family: var(--font-sans); font-size: 0.95rem; font-weight: 800; margin: 0; }
.analysis-card__body { color: rgba(255, 255, 255, 0.85); font-family: var(--font-serif); font-size: 0.95rem; line-height: var(--leading-relaxed); }
.analysis-card__body p { margin: 0; }
.analysis-card__body p + p { margin-top: var(--space-3); }
.focus-card { display: grid; gap: var(--space-2); border: 1px solid rgba(32,243,232,.24); border-radius: var(--radius-lg); padding: var(--space-4); background: var(--color-primary-50); margin-top: var(--space-4); }
.focus-card span { color: var(--color-primary); font-size: var(--text-small); font-weight: 900; }
.focus-card strong { color: var(--color-text); line-height: var(--leading-relaxed); }

.stats-bar { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-3); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: var(--space-4) var(--space-5); background: var(--color-surface); box-shadow: var(--shadow-sm); backdrop-filter: blur(28px) saturate(1.22); }
.stats-bar strong { margin-right: auto; }
.export-error { color: var(--color-danger); font-size: var(--text-caption); font-weight: 800; }
.stat { border-radius: 999px; padding: var(--space-1) var(--space-3); font-size: var(--text-small); font-weight: 900; }
.stat--error { background: #fee2e2; color: #dc2626; }
.stat--suggestion { background: #fef3c7; color: #d97706; }
.stat--highlight { background: #d1fae5; color: #059669; }
.stat--excellent { background: #dbeafe; color: #2563eb; }

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
.export-report.is-export-capturing { transform: none; }
.export-report *,
.export-report *::before,
.export-report *::after { box-sizing: border-box; }
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
.export-score { display: grid; gap: 4px; }
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
.export-section { display: grid; gap: 16px; }
.export-section__head { justify-content: space-between; }
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
.export-analysis-card p + p { margin-top: 10px; }

@media (max-width: 980px) { .result-grid { grid-template-columns: 1fr; } .scroll-panel { max-height: none; min-height: 18rem; } }
@media (max-width: 720px) {
  .page-head,
  .page-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
