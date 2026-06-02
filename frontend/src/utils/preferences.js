const FONT_SIZE_KEY = 'insightwrite:font-size'
const ANALYSIS_PREFERENCES_KEY = 'insightwrite:analysis-preferences'

export const FONT_SIZE_OPTIONS = [
  {
    id: 'compact',
    label: '紧凑',
    value: '15px',
    body: '0.98rem',
    caption: '0.86rem',
    small: '0.75rem',
    preview: '适合信息密度更高的历史、收藏和分析结果浏览。'
  },
  {
    id: 'standard',
    label: '标准',
    value: '17px',
    body: '1.0625rem',
    caption: '0.9375rem',
    small: '0.8125rem',
    preview: '保持当前 InsightWrite 的默认阅读节奏和界面比例。'
  },
  {
    id: 'large',
    label: '舒展',
    value: '18px',
    body: '1.15rem',
    caption: '1rem',
    small: '0.88rem',
    preview: '更适合长文本精读、逐句建议和复盘收藏内容。'
  },
  {
    id: 'xlarge',
    label: '大字',
    value: '20px',
    body: '1.25rem',
    caption: '1.08rem',
    small: '0.95rem',
    preview: '降低阅读压力，适合长时间查看分析报告。'
  }
]

export function getStoredFontSizeId() {
  const stored = localStorage.getItem(FONT_SIZE_KEY)
  return FONT_SIZE_OPTIONS.some(option => option.id === stored) ? stored : 'standard'
}

export function applyFontSizePreference(id = getStoredFontSizeId()) {
  const option = FONT_SIZE_OPTIONS.find(item => item.id === id) || FONT_SIZE_OPTIONS[1]
  const root = document.documentElement
  root.style.setProperty('--text-body', option.body)
  root.style.setProperty('--text-caption', option.caption)
  root.style.setProperty('--text-small', option.small)
  return option
}

export function saveFontSizePreference(id) {
  const option = applyFontSizePreference(id)
  localStorage.setItem(FONT_SIZE_KEY, option.id)
  return option
}

export const ANALYSIS_PREFERENCE_OPTIONS = {
  defaultMode: [
    { id: 'basic', label: '写作诊断', description: '默认检查语法、用词、结构和内容表达。' },
    { id: 'quality', label: '高阶润色', description: '默认强化论证、句式和表达层次。' },
    { id: 'deep', label: '佳作精读', description: '默认拆解范文、外刊和高质量段落。' }
  ],
  detailLevel: [
    { id: 'standard', label: '标准', description: '保持当前输出密度。' },
    { id: 'concise', label: '简洁', description: '压缩解释，只保留关键判断。' },
    { id: 'detailed', label: '细致', description: '在既有章节内补充更多理由。' }
  ],
  focus: [
    { id: 'balanced', label: '均衡', description: '不额外偏向单一维度。' },
    { id: 'grammar', label: '语法表达', description: '更关注语法、用词和句式准确度。' },
    { id: 'logic', label: '逻辑结构', description: '更关注段落推进、论证和衔接。' },
    { id: 'style', label: '表达风格', description: '更关注地道度、节奏和表达品质。' }
  ]
}

export const DEFAULT_ANALYSIS_PREFERENCES = {
  defaultMode: 'basic',
  detailLevel: 'standard',
  focus: 'balanced',
  generateEssay: true
}

function normalizeAnalysisPreferences(value = {}) {
  const next = { ...DEFAULT_ANALYSIS_PREFERENCES, ...value }
  for (const [key, options] of Object.entries(ANALYSIS_PREFERENCE_OPTIONS)) {
    if (!options.some(option => option.id === next[key])) {
      next[key] = DEFAULT_ANALYSIS_PREFERENCES[key]
    }
  }
  next.generateEssay = Boolean(next.generateEssay)
  return next
}

export function getStoredAnalysisPreferences() {
  try {
    return normalizeAnalysisPreferences(JSON.parse(localStorage.getItem(ANALYSIS_PREFERENCES_KEY) || '{}'))
  } catch {
    return { ...DEFAULT_ANALYSIS_PREFERENCES }
  }
}

export function saveAnalysisPreferences(preferences) {
  const next = normalizeAnalysisPreferences(preferences)
  localStorage.setItem(ANALYSIS_PREFERENCES_KEY, JSON.stringify(next))
  localStorage.setItem('insightwrite:essay-mode', next.defaultMode)
  return next
}
