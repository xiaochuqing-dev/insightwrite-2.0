<template>
  <article class="markup-document" @mouseleave="hideTooltip">
    <template v-for="(segment, index) in segments" :key="`${segment.start}-${segment.end}-${index}`">
      <span
        v-if="segment.annotations.length"
        class="mark"
        :class="markClasses(segment)"
        :data-stack="segment.annotations.length > 1 ? segment.annotations.length : null"
        @mouseenter="showTooltip(segment.annotations[0], $event)"
        @mousemove="moveTooltip"
        @click="selectAnnotation(segment.annotations[0], $event)"
      >{{ segment.text }}</span>
      <span v-else>{{ segment.text }}</span>
    </template>

    <Teleport to="body">
      <AnnotationTooltip
        v-if="activeAnnotation && tooltipRect"
        :annotation="activeAnnotation"
        :anchor-rect="tooltipRect"
      />
    </Teleport>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'
import AnnotationTooltip from '@/components/AnnotationTooltip.vue'

const props = defineProps({
  text: {
    type: String,
    default: ''
  },
  annotations: {
    type: Array,
    default: () => []
  },
  selectedId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['select'])

const activeAnnotation = ref(null)
const tooltipRect = ref(null)

const normalizedAnnotations = computed(() =>
  props.annotations
    .map((annotation, index) => ({
      ...annotation,
      id: annotation.id || `${annotation.start}-${annotation.end}-${annotation.type}-${index}`,
      start: clampPosition(annotation.start),
      end: clampPosition(annotation.end),
      severity: annotation.severity || 'low',
      type: annotation.type || 'suggestion'
    }))
    .filter(annotation => annotation.end > annotation.start)
    .sort((a, b) => a.start - b.start || b.end - a.end)
)

const segments = computed(() => {
  const boundaries = new Set([0, props.text.length])

  normalizedAnnotations.value.forEach(annotation => {
    boundaries.add(annotation.start)
    boundaries.add(annotation.end)
  })

  const points = [...boundaries].sort((a, b) => a - b)

  return points
    .slice(0, -1)
    .map((start, index) => {
      const end = points[index + 1]
      const text = props.text.slice(start, end)
      const annotations = normalizedAnnotations.value.filter(annotation => annotation.start < end && annotation.end > start)

      return {
        start,
        end,
        text,
        annotations
      }
    })
    .filter(segment => segment.text)
})

function clampPosition(position) {
  const value = Number(position)
  if (!Number.isFinite(value)) return 0
  return Math.max(0, Math.min(value, props.text.length))
}

function markClasses(segment) {
  const primary = segment.annotations[0]
  return [
    `mark--${primary.type}`,
    `mark--${primary.severity}`,
    {
      'is-selected': primary.id === props.selectedId,
      'is-stacked': segment.annotations.length > 1
    }
  ]
}

function showTooltip(annotation, event) {
  activeAnnotation.value = annotation
  tooltipRect.value = getTooltipAnchorRect(event)
}

function hideTooltip() {
  activeAnnotation.value = null
  tooltipRect.value = null
}

function moveTooltip(event) {
  if (!activeAnnotation.value) return
  tooltipRect.value = getTooltipAnchorRect(event)
}

function selectAnnotation(annotation, event) {
  showTooltip(annotation, event)
  emit('select', annotation)
}

function getTooltipAnchorRect(event) {
  if (Number.isFinite(event.clientX) && Number.isFinite(event.clientY)) {
    const size = 8
    return {
      left: event.clientX - size / 2,
      right: event.clientX + size / 2,
      top: event.clientY - size / 2,
      bottom: event.clientY + size / 2,
      width: size,
      height: size
    }
  }

  const rects = Array.from(event.currentTarget.getClientRects())
  const visibleRect = rects.find(rect => rect.width > 0 && rect.height > 0)
  return visibleRect || event.currentTarget.getBoundingClientRect()
}
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.markup-document {
  white-space: pre-wrap;
  color: var(--color-text);
  font-family: var(--font-mono);
  font-size: 0.95rem;
  line-height: 2;
}

.mark {
  cursor: pointer;
  color: inherit;
  text-decoration-line: underline;
  text-decoration-style: solid;
  text-underline-offset: 0.24em;
}

.mark--error {
  text-decoration-color: #dc2626;
}

.mark--suggestion {
  text-decoration-color: #d97706;
}

.mark--highlight {
  text-decoration-color: #059669;
}

.mark--excellent {
  text-decoration-color: #2563eb;
}

.mark--low {
  text-decoration-thickness: 2px;
}

.mark--medium {
  text-decoration-thickness: 3px;
}

.mark--high {
  color: var(--color-text);
  text-decoration-thickness: 3px;
}

.mark--high {
  text-decoration-thickness: 3px;
}

.mark.is-selected {
  box-shadow:
    0 0 0 2px rgba(233, 255, 207, 0.28),
    0 10px 28px rgba(0, 0, 0, 0.22),
    inset 0 -0.12rem rgba(233, 255, 207, 0.12);
}

.mark.is-stacked::after {
  content: attr(data-stack);
  position: absolute;
  top: -0.7rem;
  right: -0.35rem;
  min-width: 1rem;
  height: 1rem;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: var(--gray-900);
  color: white;
  font-family: var(--font-sans);
  font-size: 0.62rem;
  font-weight: 900;
  line-height: 1;
}
</style>
