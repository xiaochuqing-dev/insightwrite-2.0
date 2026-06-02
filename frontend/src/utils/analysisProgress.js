const MODE_PHASES = {
  basic: ['分析语法结构', '评估表达准确度', '检查内容逻辑', '整理修改建议'],
  quality: ['梳理论证层次', '优化句式表达', '提升词汇品质', '整理润色方案'],
  deep: ['拆解篇章结构', '提炼高分表达', '分析长难句', '整理可模仿写法']
}

const MODE_DETAIL = {
  basic: {
    action: '写作诊断',
    result: '原文标注和分析结果'
  },
  quality: {
    action: '高阶润色',
    result: '原文标注和优化方案'
  },
  deep: {
    action: '佳作精读',
    result: '原文标注和赏析结果'
  }
}

export function countEnglishWords(value) {
  const text = value?.trim() || ''
  return text ? text.split(/\s+/).filter(Boolean).length : 0
}

export function getProcessingPhases(mode) {
  return MODE_PHASES[mode] || MODE_PHASES.basic
}

export function getProcessingDetail({ mode, wordCount }) {
  const copy = MODE_DETAIL[mode] || MODE_DETAIL.basic
  const count = Number(wordCount)
  if (Number.isFinite(count) && count > 0) {
    return `系统正在进行约 ${count} 词的${copy.action}，完成后自动展示${copy.result}。`
  }
  return `系统会持续轮询任务状态，完成后自动展示${copy.result}。`
}
