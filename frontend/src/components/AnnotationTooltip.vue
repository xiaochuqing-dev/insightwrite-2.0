<template>
  <div class="annotation-tooltip" :style="tooltipStyle" role="tooltip">
    <div class="tooltip-head">
      <span class="type-dot" :class="`type-dot--${safeType}`" aria-hidden="true"></span>
      <strong>{{ typeLabel }}</strong>
    </div>

    <p>{{ annotation?.suggestion || '' }}</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  annotation: { type: Object, required: true },
  anchorRect: { type: Object, required: true }
})

const typeMap = {
  error: '严重错误',
  suggestion: '建议优化',
  highlight: '表达亮点',
  excellent: '非常出色'
}

const safeType = computed(() => props.annotation?.type || 'suggestion')
const typeLabel = computed(() => typeMap[safeType.value] || '标注')

const tooltipStyle = computed(() => {
  const w = 280
  const gap = 12
  const m = 8
  const r = props.anchorRect
  const vw = document.documentElement.clientWidth
  const vh = document.documentElement.clientHeight

  // 直接放在鼠标位置右下方
  let left = r.left + gap
  let top = r.top + gap

  // 右边不够就放鼠标左边
  if (left + w + m > vw) left = r.left - w - gap
  // 下面不够就放鼠标上方
  if (top + 120 + m > vh) top = r.top - 120 - gap
  // 边距保护
  left = Math.max(m, Math.min(left, vw - w - m))
  top = Math.max(m, top)

  return { left: `${left}px`, top: `${top}px` }
})
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.annotation-tooltip {
  position: fixed;
  z-index: 50;
  display: grid;
  gap: var(--space-2);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  background: rgba(15, 23, 42, 0.92);
  color: #e2e8f0;
  box-shadow: 0 8px 32px rgba(0,0,0,.35);
  pointer-events: none;
}

.tooltip-head {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.type-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.type-dot--error { background: #dc2626; }
.type-dot--suggestion { background: #d97706; }
.type-dot--highlight { background: #059669; }
.type-dot--excellent { background: #2563eb; }

.tooltip-head strong {
  font-size: var(--text-caption);
  color: white;
}

p {
  font-size: var(--text-small);
  line-height: var(--leading-relaxed);
  color: #cbd5e1;
}
</style>
