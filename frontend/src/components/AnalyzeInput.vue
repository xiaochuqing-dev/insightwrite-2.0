<template>
  <section class="analyze-panel">
    <div class="topic-row">
      <label class="field-label" for="essay-topic">文本标题 / 作文题目（可选）</label>
      <input
        id="essay-topic"
        v-model.trim="topic"
        type="text"
        :maxlength="TOPIC_MAX_CHARS"
        placeholder="给文本一个标题，如：My View on Environmental Protection"
        :disabled="isSubmitting"
        @input="handleTopicInput"
      />
      <p class="limit-note">{{ topicWordCount }} / {{ TOPIC_MAX_WORDS }} 词，{{ topic.length }} / {{ TOPIC_MAX_CHARS }} 字符</p>
    </div>

    <div class="input-shell">
      <div class="input-shell__header">
        <label class="field-label input-header">英语文本</label>
        <span :class="{ 'is-warning': isOverWordLimit }">{{ wordCount }} / {{ modeWordLimit }} 词</span>
      </div>
      <textarea
        v-model="essayText"
        placeholder="粘贴你的作文、范文、外刊段落或高质量英语文章..."
        :disabled="isSubmitting"
        @input="handleTextInput"
      ></textarea>
      <div class="text-stats" :class="{ 'is-warning': isOverWordLimit }">
        <span>{{ characterCount }} 字符</span>
        <span>{{ wordCount }} / {{ modeWordLimit }} 词</span>
      </div>
    </div>

    <div class="mode-grid">
      <button
        v-for="m in modes"
        :key="m.value"
        class="mode-card"
        :class="{ 'is-selected': selectedMode === m.value }"
        type="button"
        :disabled="isSubmitting"
        @click="selectMode(m.value)"
      >
        <span class="mode-card__topline"><strong>{{ m.label }}</strong><em>{{ m.cost }} 积分</em></span>
        <span>{{ m.description }}</span>
        <small>最多 {{ m.wordLimit }} 词</small>
      </button>
    </div>

    <label v-if="selectedMode !== 'deep'" class="essay-toggle">
      <input type="checkbox" v-model="generateEssay" />
      <span>为我的作文生成参考范文（AI 将根据文本主题生成一篇同主题范文供对比学习）</span>
    </label>

    <label class="custom-field">
      <span class="field-label">自定义分析重点<em>可选（限120字）</em></span>
      <input
        v-model.trim="customRequirement"
        :maxlength="CUSTOM_MAX_CHARS"
        placeholder="如：重点看论证结构 / 拆解高分表达"
        :disabled="isSubmitting"
        @input="persistCustom"
      />
    </label>

    <p class="credit-note" :class="{ 'is-warning': (!canAfford && isLoggedIn) || isOverWordLimit || isTopicOverLimit }">
      <template v-if="isTopicOverLimit">标题/作文题目最多 {{ TOPIC_MAX_WORDS }} 词，请精简后提交。</template>
      <template v-else-if="isOverWordLimit">当前模式最多 {{ modeWordLimit }} 词，请精简文本或选择更合适的模式。</template>
      <template v-else-if="isLoggedIn">当前积分 {{ credits }}，本次分析消耗 {{ selectedModeMeta.cost }} 积分。<span v-if="!canAfford">积分不足，明天自动补充。</span></template>
      <template v-else>请先登录后再使用写作诊断或佳作精读。</template>
    </p>

    <div class="submit-center">
      <RouterLink v-if="hasActiveTask && activeTaskId" :to="`/analyze/${activeTaskId}`" class="view-result-btn">查看结果</RouterLink>
      <button class="clear-button" type="button" :disabled="isSubmitting || isDraftEmpty" @click="clearDraft">清空输入</button>
      <button class="submit-button" type="button" :disabled="isDisabled" @click="handleSubmit">
        <span v-if="isSubmitting" class="spinner"></span>
        {{ submitLabel }}
      </button>
    </div>

    <p v-if="errorMessage" class="input-error">{{ errorMessage }}</p>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { getStoredAnalysisPreferences } from '@/utils/preferences.js'

const props = defineProps({
  initialText: { type: String, default: '' },
  credits: { type: Number, default: null },
  isSubmitting: { type: Boolean, default: false },
  hasActiveTask: { type: Boolean, default: false },
  activeTaskId: { type: String, default: '' },
  errorMessage: { type: String, default: '' }
})

const emit = defineEmits(['submit', 'text-change'])

const TOPIC_MAX_CHARS = 160
const TOPIC_MAX_WORDS = 30
const CUSTOM_MAX_CHARS = 120
const storageKey = 'insightwrite:essay-draft'
const topicKey = 'insightwrite:essay-topic'
const customKey = 'insightwrite:essay-custom'
const modeKey = 'insightwrite:essay-mode'
const analysisPreferences = getStoredAnalysisPreferences()

const modes = [
  { value: 'basic', label: '写作诊断', description: '适合自己的作文：检查语法、用词、结构和内容表达，并给出修改建议', cost: 25, wordLimit: 800 },
  { value: 'quality', label: '高阶润色', description: '适合已有初稿：深入优化论证、句式、词汇品质和表达层次', cost: 35, wordLimit: 900 },
  { value: 'deep', label: '佳作精读', description: '适合外刊、范文和名篇：拆解高分表达、长难句与可模仿写法', cost: 50, wordLimit: 1200 }
]

const topic = ref(localStorage.getItem(topicKey) || '')
const essayText = ref(props.initialText)
const selectedMode = ref(localStorage.getItem(modeKey) || analysisPreferences.defaultMode)
const customRequirement = ref(localStorage.getItem(customKey) || '')
const generateEssay = ref(analysisPreferences.generateEssay)

watch(() => props.initialText, v => {
  essayText.value = v
  enforceTextLimit()
})
watch(selectedMode, v => {
  localStorage.setItem(modeKey, v)
  enforceTextLimit()
})

const selectedModeMeta = computed(() => modes.find(m => m.value === selectedMode.value) || modes[0])
const trimmed = computed(() => essayText.value.trim())
const words = computed(() => trimmed.value ? trimmed.value.split(/\s+/).filter(Boolean) : [])
const topicWords = computed(() => topic.value.trim() ? topic.value.trim().split(/\s+/).filter(Boolean) : [])
const topicWordCount = computed(() => topicWords.value.length)
const characterCount = computed(() => essayText.value.length)
const wordCount = computed(() => words.value.length)
const modeWordLimit = computed(() => selectedModeMeta.value.wordLimit)
const isOverWordLimit = computed(() => wordCount.value > modeWordLimit.value)
const isTopicOverLimit = computed(() => topicWordCount.value > TOPIC_MAX_WORDS || topic.value.length > TOPIC_MAX_CHARS)
const isLoggedIn = computed(() => typeof props.credits === 'number')
const canAfford = computed(() => !isLoggedIn.value || props.credits >= selectedModeMeta.value.cost)
const hasText = computed(() => Boolean(trimmed.value))
const isDraftEmpty = computed(() => !essayText.value && !topic.value && !customRequirement.value)
const isDisabled = computed(() => props.hasActiveTask || props.isSubmitting || !hasText.value || !isLoggedIn.value || !canAfford.value || isOverWordLimit.value || isTopicOverLimit.value)
const submitLabel = computed(() => {
  if (props.hasActiveTask) return '请等待上一个分析完成'
  if (props.isSubmitting) return '提交中...'
  if (!hasText.value) return '请输入英语文本'
  if (isOverWordLimit.value) return `最多 ${modeWordLimit.value} 词`
  if (isTopicOverLimit.value) return '标题过长'
  if (!isLoggedIn.value) return '请先登录'
  if (!canAfford.value) return '积分不足'
  return selectedMode.value === 'deep' ? '开始佳作精读' : '开始写作分析'
})

function truncateWords(value, maxWords) {
  const list = value.trim().split(/\s+/).filter(Boolean)
  return list.length > maxWords ? list.slice(0, maxWords).join(' ') : value
}

function enforceTextLimit() {
  const next = truncateWords(essayText.value, modeWordLimit.value)
  if (next !== essayText.value) essayText.value = next
  persistText()
}

function handleTextInput() {
  enforceTextLimit()
}

function handleTopicInput() {
  if (topicWordCount.value > TOPIC_MAX_WORDS) {
    topic.value = topicWords.value.slice(0, TOPIC_MAX_WORDS).join(' ')
  }
  persistTopic()
}

function selectMode(mode) {
  selectedMode.value = mode
}

function persistText() {
  localStorage.setItem(storageKey, essayText.value)
  emit('text-change', essayText.value)
}

function persistTopic() { localStorage.setItem(topicKey, topic.value) }
function persistCustom() { localStorage.setItem(customKey, customRequirement.value) }

function clearDraft() {
  essayText.value = ''
  topic.value = ''
  customRequirement.value = ''
  localStorage.removeItem(storageKey)
  localStorage.removeItem(topicKey)
  localStorage.removeItem(customKey)
  emit('text-change', '')
}

function handleSubmit() {
  if (isDisabled.value) return
  emit('submit', {
    essayText: trimmed.value,
    mode: selectedMode.value,
    customRequirement: customRequirement.value,
    topic: topic.value,
    generateEssay: generateEssay.value,
    analysisPreferences
  })
}

enforceTextLimit()
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.analyze-panel { display: grid; gap: var(--space-5); }

.field-label { color: var(--color-text); font-size: 1.18rem; font-weight: 800; display: block; margin-bottom: var(--space-2); }

.topic-row input {
  width: 100%; height: 2.75rem; padding: 0 var(--space-4);
  border: 1px solid rgba(151,210,166,.24); border-radius: var(--radius-lg); outline: none; font-size: 1.05rem;
  background: rgba(7,25,18,.56);
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(24px) saturate(1.18);
  color: var(--color-text);
  font-weight: 650;
}
.topic-row input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(37,99,235,.12); }
.limit-note { margin-top: var(--space-2); color: var(--color-text-subtle); font-size: var(--text-small); font-weight: 700; text-align: right; }

.input-shell {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(151, 210, 166, 0.24);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, rgba(18, 48, 33, 0.68), rgba(3, 14, 10, 0.52)),
    rgba(7, 25, 18, 0.58);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(28px) saturate(1.22);
}
.input-shell__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-6) var(--space-2);
}
.input-shell__header span {
  flex: 0 0 auto;
  border: 1px solid rgba(151,210,166,.2);
  border-radius: 999px;
  padding: var(--space-1) var(--space-3);
  background: rgba(7,25,18,.42);
  color: var(--color-text-muted);
  font-size: var(--text-small);
  font-weight: 750;
}
.input-shell__header span.is-warning { border-color: rgba(239,68,68,.38); color: #ffb4b4; }
.input-header { margin: 0; }
textarea {
  width: 100%;
  min-height: 22rem;
  display: block;
  border: 0;
  padding: var(--space-3) var(--space-6) var(--space-10);
  background: transparent;
  color: var(--color-text);
  font-size: 1.08rem;
  font-weight: 650;
  line-height: var(--leading-relaxed);
  outline: none;
  resize: vertical;
}
textarea::placeholder { color: rgba(226, 239, 217, 0.58); font-size: 1rem; }
.text-stats { position: absolute; right: var(--space-4); bottom: var(--space-4); display: inline-flex; gap: var(--space-2); color: var(--color-text-muted); font-size: var(--text-small); }
.text-stats span { border: 1px solid rgba(151,210,166,.2); border-radius: 999px; padding: var(--space-1) var(--space-3); background: rgba(7,25,18,.42); backdrop-filter: blur(18px); color: var(--color-text-muted); font-weight: 750; }
.text-stats.is-warning span { border-color: rgba(239,68,68,.38); color: #ffb4b4; }

.mode-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--space-4); }
.mode-card { min-height: 7rem; display: grid; align-content: space-between; gap: var(--space-3); border: 1px solid rgba(151,210,166,.24); border-radius: var(--radius-lg); padding: var(--space-4); background: rgba(7,25,18,.48); color: var(--color-text-muted); text-align: left; box-shadow: var(--shadow-sm); backdrop-filter: blur(24px) saturate(1.18); transition: transform var(--duration-fast), border-color var(--duration-fast), box-shadow var(--duration-fast), background-color var(--duration-fast); }
.mode-card:hover:not(:disabled), .mode-card.is-selected { transform: translateY(-2px); border-color: rgba(233,255,207,.58); box-shadow: var(--shadow-md); background: rgba(14,45,31,.62); }
.mode-card.is-selected { color: var(--color-text); }
.mode-card__topline { display: grid; gap: var(--space-2); }
.mode-card strong { color: var(--color-text); font-family: var(--font-serif); font-size: 1.22rem; }
.mode-card em, .custom-field em { color: #f2c94c; font-size: var(--text-small); font-style: normal; font-weight: 800; }
.mode-card small { color: var(--color-text-subtle); font-size: var(--text-small); font-weight: 800; }

.custom-field input { width: 100%; height: 2.75rem; padding: 0 var(--space-4); border: 1px solid rgba(151,210,166,.24); border-radius: var(--radius-lg); outline: none; background: rgba(7,25,18,.56); box-shadow: var(--shadow-sm); backdrop-filter: blur(24px) saturate(1.18); color: var(--color-text); font-weight: 650; }
.custom-field input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(37,99,235,.12); }

.essay-toggle { display: flex; align-items: center; gap: var(--space-2); color: var(--color-text-muted); font-size: var(--text-caption); cursor: pointer; }
.essay-toggle input { width: 1rem; height: 1rem; accent-color: var(--color-primary); }

.credit-note { color: var(--color-text-muted); font-size: var(--text-caption); text-align: center; }
.credit-note.is-warning { color: #ffb4b4; }

.submit-center { display: flex; flex-wrap: wrap; justify-content: center; gap: var(--space-4); }
.view-result-btn, .clear-button { height: 3.75rem; padding: 0 var(--space-6); display: inline-flex; align-items: center; justify-content: center; border-radius: var(--radius-md); font-size: 1.05rem; font-weight: 800; transition: transform var(--duration-fast), box-shadow var(--duration-fast), background-color var(--duration-fast); backdrop-filter: blur(18px) saturate(1.16); }
.view-result-btn { border: 2px solid rgba(32,243,232,.42); background: rgba(7,25,18,.58); color: #8ffcf5; }
.view-result-btn:hover, .clear-button:hover:not(:disabled) { background: rgba(14,45,31,.72); color: #e9ffcf; transform: translateY(-2px); box-shadow: var(--shadow-md); }
.clear-button { border: 1px solid rgba(151,210,166,.28); background: rgba(7,25,18,.46); color: var(--color-text-muted); }
.submit-button { min-width: 16rem; height: 3.75rem; display: inline-flex; align-items: center; justify-content: center; gap: var(--space-2); border-radius: var(--radius-md); background: linear-gradient(135deg, var(--gray-900), var(--color-primary-700)); color: white; font-size: 1.05rem; font-weight: 850; box-shadow: var(--shadow-md); transition: transform var(--duration-fast), box-shadow var(--duration-fast); }
.submit-button:hover:not(:disabled) { transform: translateY(-2px); box-shadow: var(--shadow-lg); }
.spinner { width: 1rem; height: 1rem; border: 2px solid rgba(255,255,255,.36); border-top-color: white; border-radius: 50%; animation: spin 720ms linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.input-error { border: 1px solid rgba(239, 68, 68, 0.28); border-radius: var(--radius-md); padding: var(--space-3) var(--space-4); background: rgba(88, 25, 29, 0.42); color: #ffb4b4; font-size: var(--text-caption); text-align: center; }

@media (max-width: 920px) { .mode-grid { grid-template-columns: 1fr; } }
@media (max-width: 620px) {
  .input-shell__header { padding-inline: var(--space-4); }
  textarea { min-height: 15rem; padding: var(--space-3) var(--space-4) var(--space-12); }
}
</style>
