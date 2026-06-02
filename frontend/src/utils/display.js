const modeLabels = {
  basic: '写作诊断',
  quality: '高阶润色',
  deep: '佳作精读'
}

export function modeLabel(mode) {
  return modeLabels[mode] || mode
}

export function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

export function splitTags(value) {
  return value ? value.split(/[,\uFF0C]/).map(item => item.trim()).filter(Boolean) : []
}
