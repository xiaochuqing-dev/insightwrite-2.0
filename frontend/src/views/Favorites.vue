<template>
  <section class="favorites-view">
    <div class="page-head">
      <div>
        <p class="eyebrow">Favorites</p>
        <h1>我的收藏</h1>
      </div>
    </div>

    <div class="favorite-tabs">
      <button type="button" :class="{ active: activeTab === 'analysis' }" @click="activeTab = 'analysis'">分析收藏</button>
      <button type="button" :class="{ active: activeTab === 'knowledge' }" @click="activeTab = 'knowledge'">知识库收藏</button>
    </div>

    <template v-if="activeTab === 'analysis'">
      <FavoriteState :loading="analysisLoading" :error="analysisError" :empty="analysisItems.length === 0" @retry="loadAnalysis">
        <template #empty>
          <strong>还没有分析收藏</strong>
          <p>在历史记录或分析结果中收藏值得复盘的文本。</p>
          <RouterLink to="/history">查看历史</RouterLink>
        </template>
      </FavoriteState>

      <div v-if="!analysisLoading && !analysisError && analysisItems.length" class="fav-list">
        <article v-for="item in analysisItems" :key="item.task_id" class="fav-card" @click="goAnalysis(item.task_id)">
          <div class="card-top">
            <span class="mode-tag" :class="`mode-tag--${item.mode}`">{{ modeLabel(item.mode) }}</span>
            <span class="date-text">{{ formatDate(item.created_at) }}</span>
          </div>
          <p class="card-preview">{{ item.preview }}</p>
          <div class="card-meta">
            <span>{{ item.word_count || 0 }} 词</span>
            <button type="button" @click.stop="unfavoriteAnalysis(item)">取消收藏</button>
          </div>
        </article>
      </div>

      <Pager :page="analysisPage" :total-pages="analysisTotalPages" @change="goAnalysisPage" />
    </template>

    <template v-else>
      <FavoriteState :loading="knowledgeLoading" :error="knowledgeError" :empty="knowledgeItems.length === 0" @retry="loadKnowledge">
        <template #empty>
          <strong>还没有知识库收藏</strong>
          <p>打开表达知识库条目后，可以收藏需要反复学习的内容。</p>
          <RouterLink to="/knowledge">浏览知识库</RouterLink>
        </template>
      </FavoriteState>

      <div v-if="!knowledgeLoading && !knowledgeError && knowledgeItems.length" class="fav-list">
        <article v-for="item in knowledgeItems" :key="item.knowledge_id" class="fav-card fav-card--knowledge" @click="goKnowledge(item.knowledge_id)">
          <div class="card-top">
            <span class="mode-tag mode-tag--knowledge">{{ item.category }}</span>
            <span class="date-text">{{ formatDate(item.created_at) }}</span>
          </div>
          <h3>{{ item.title }}</h3>
          <p class="card-preview">{{ item.preview }}</p>
          <div class="card-meta">
            <span>表达知识库</span>
            <button type="button" @click.stop="unfavoriteKnowledge(item)">取消收藏</button>
          </div>
        </article>
      </div>

      <Pager :page="knowledgePage" :total-pages="knowledgeTotalPages" @change="goKnowledgePage" />
    </template>
  </section>
</template>

<script setup>
import { defineComponent, h, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  getFavorites,
  getKnowledgeFavorites,
  removeHistoryFavorite,
  removeKnowledgeFavorite
} from '@/api/favorites.js'
import { formatDate, modeLabel } from '@/utils/display.js'

const FavoriteState = defineComponent({
  props: {
    loading: Boolean,
    error: String,
    empty: Boolean
  },
  emits: ['retry'],
  setup(props, { slots, emit }) {
    return () => {
      if (props.loading) {
        return h('div', { class: 'state-card' }, [
          h('span', { class: 'pulse-ring', 'aria-hidden': 'true' }),
          h('p', '加载中')
        ])
      }
      if (props.error) {
        return h('div', { class: 'state-card state-card--failed' }, [
          h('strong', '加载失败'),
          h('p', props.error),
          h('button', { type: 'button', onClick: () => emit('retry') }, '重试')
        ])
      }
      if (props.empty) {
        return h('div', { class: 'state-card' }, slots.empty?.())
      }
      return null
    }
  }
})

const Pager = defineComponent({
  props: {
    page: { type: Number, required: true },
    totalPages: { type: Number, required: true }
  },
  emits: ['change'],
  setup(props, { emit }) {
    return () => props.totalPages > 1
      ? h('nav', { class: 'pager' }, [
          h('button', { type: 'button', disabled: props.page <= 1, onClick: () => emit('change', props.page - 1) }, '上一页'),
          h('span', `${props.page} / ${props.totalPages}`),
          h('button', { type: 'button', disabled: props.page >= props.totalPages, onClick: () => emit('change', props.page + 1) }, '下一页')
        ])
      : null
  }
})

const router = useRouter()
const activeTab = ref('analysis')

const analysisItems = ref([])
const analysisPage = ref(1)
const analysisTotalPages = ref(1)
const analysisLoading = ref(false)
const analysisError = ref('')

const knowledgeItems = ref([])
const knowledgePage = ref(1)
const knowledgeTotalPages = ref(1)
const knowledgeLoading = ref(false)
const knowledgeError = ref('')

async function loadAnalysis() {
  analysisLoading.value = true
  analysisError.value = ''
  try {
    const response = await getFavorites(analysisPage.value)
    analysisItems.value = response.data?.items || []
    analysisTotalPages.value = response.data?.pages || 1
  } catch (e) {
    analysisError.value = e?.response?.data?.error || '加载失败'
  } finally {
    analysisLoading.value = false
  }
}

async function loadKnowledge() {
  knowledgeLoading.value = true
  knowledgeError.value = ''
  try {
    const response = await getKnowledgeFavorites(knowledgePage.value)
    knowledgeItems.value = response.data?.items || []
    knowledgeTotalPages.value = response.data?.pages || 1
  } catch (e) {
    knowledgeError.value = e?.response?.data?.error || '加载失败'
  } finally {
    knowledgeLoading.value = false
  }
}

function goAnalysis(id) {
  router.push(`/history/${id}`)
}

function goKnowledge(id) {
  router.push(`/knowledge/${id}`)
}

async function unfavoriteAnalysis(item) {
  await removeHistoryFavorite(item.task_id)
  analysisItems.value = analysisItems.value.filter(entry => entry.task_id !== item.task_id)
}

async function unfavoriteKnowledge(item) {
  await removeKnowledgeFavorite(item.knowledge_id)
  knowledgeItems.value = knowledgeItems.value.filter(entry => entry.knowledge_id !== item.knowledge_id)
}

function goAnalysisPage(nextPage) {
  analysisPage.value = nextPage
  loadAnalysis()
}

function goKnowledgePage(nextPage) {
  knowledgePage.value = nextPage
  loadKnowledge()
}

onMounted(() => {
  loadAnalysis()
  loadKnowledge()
})
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.favorites-view { display: grid; gap: var(--space-6); }
.page-head { display: flex; align-items: end; justify-content: space-between; }
.eyebrow { color: #f2c94c; font-size: 1.08rem; font-weight: 850; text-transform: uppercase; }
h1 { font-family: var(--font-serif); font-size: clamp(2rem, 3vw, 3.4rem); color: var(--color-text); }

.favorite-tabs { display: inline-flex; width: max-content; max-width: 100%; gap: var(--space-2); border: 1px solid rgba(151, 210, 166, 0.22); border-radius: var(--radius-lg); padding: var(--space-2); background: rgba(7, 25, 18, 0.48); box-shadow: var(--shadow-sm); backdrop-filter: blur(20px); }
.favorite-tabs button { height: 2.45rem; padding: 0 var(--space-4); border-radius: var(--radius-md); color: var(--color-text-muted); font-size: var(--text-caption); font-weight: 850; }
.favorite-tabs button.active { background: rgba(49, 92, 61, 0.62); color: var(--color-text); box-shadow: inset 0 0 0 1px rgba(233, 255, 207, 0.22); }

.state-card { min-height: 20rem; display: grid; place-items: center; gap: var(--space-4); border: 1px solid rgba(151, 210, 166, 0.26); border-radius: var(--radius-lg); padding: var(--space-8); background: rgba(7, 25, 18, 0.58); text-align: center; box-shadow: var(--shadow-md); backdrop-filter: blur(24px) saturate(1.18); }
.state-card--failed strong { color: var(--color-danger); }
.state-card a, .state-card button { font-weight: 850; color: var(--color-primary); }
.pulse-ring { width: 2.5rem; height: 2.5rem; border: 3px solid var(--color-primary-100); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 900ms linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.fav-list { display: grid; gap: var(--space-3); }
.fav-card { display: grid; gap: var(--space-3); border: 1px solid rgba(151, 210, 166, 0.24); border-radius: var(--radius-lg); padding: var(--space-5); background: rgba(7, 25, 18, 0.54); box-shadow: var(--shadow-sm); cursor: pointer; transition: box-shadow var(--duration-fast), background-color var(--duration-fast), border-color var(--duration-fast); backdrop-filter: blur(24px) saturate(1.18); }
.fav-card:hover { border-color: rgba(32, 243, 232, 0.32); background: rgba(14, 45, 31, 0.62); box-shadow: var(--shadow-md); }
.fav-card--knowledge { background: rgba(9, 39, 35, 0.56); }
.card-top { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); }
.mode-tag { border-radius: 999px; padding: 2px 10px; font-size: var(--text-small); font-weight: 800; }
.mode-tag--basic { border: 1px solid rgba(32, 243, 232, 0.28); background: rgba(32, 243, 232, 0.14); color: #8ffcf5; }
.mode-tag--quality { border: 1px solid rgba(34, 197, 94, 0.3); background: rgba(34, 197, 94, 0.14); color: #9fffc6; }
.mode-tag--deep { border: 1px solid rgba(196, 181, 253, 0.3); background: rgba(139, 92, 246, 0.18); color: #ddd6fe; }
.mode-tag--knowledge { border: 1px solid rgba(242, 201, 76, 0.32); background: rgba(242, 201, 76, 0.14); color: #f2c94c; }
.date-text { color: var(--color-text-muted); font-size: var(--text-small); }
h3 { color: var(--color-text); font-family: var(--font-serif); font-size: 1.18rem; line-height: var(--leading-tight); }
.card-preview { color: var(--color-text); line-height: var(--leading-relaxed); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-meta { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); color: var(--color-text-muted); font-size: var(--text-small); }
.card-meta button { height: 2.25rem; padding: 0 var(--space-3); border: 1px solid rgba(242, 201, 76, 0.34); border-radius: var(--radius-md); background: rgba(242, 201, 76, 0.12); color: #f2c94c; font-weight: 850; }

.pager { display: flex; align-items: center; justify-content: center; gap: var(--space-4); }
.pager button { height: 2.5rem; padding: 0 var(--space-4); border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface); font-weight: 700; color: var(--color-text); }
.pager button:disabled { opacity: .4; cursor: not-allowed; }
.pager span { color: var(--color-text-muted); font-size: var(--text-caption); }

@media (max-width: 620px) {
  .favorite-tabs { width: 100%; display: grid; grid-template-columns: 1fr 1fr; }
  .card-meta { align-items: flex-start; flex-direction: column; }
}
</style>
