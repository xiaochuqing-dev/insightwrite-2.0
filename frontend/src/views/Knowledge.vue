<template>
  <section class="knowledge-view">
    <div class="page-head">
      <div>
        <p class="eyebrow">Expression Library</p>
        <h1>表达知识库</h1>
        <p class="page-desc">把分析中遇到的语法、结构和表达问题，沉淀成可反复查阅的写作能力。</p>
        <p class="fresh-note">每周不定时补充新表达、新结构和高频问题。</p>
      </div>
    </div>

    <div class="search-bar">
      <input v-model.trim="searchText" type="text" placeholder="搜索标题或标签..." @keyup.enter="doSearch" />
      <button type="button" @click="doSearch">搜索</button>
    </div>

    <div class="filter-tabs">
      <button v-for="c in categories" :key="c" type="button" :class="{ active: selectedCategory === c }" @click="selectCategory(c)">{{ c }}</button>
    </div>

    <div v-if="isLoading" class="state-card"><span class="pulse-ring" aria-hidden="true"></span><p>加载中</p></div>
    <div v-else-if="errorMsg" class="state-card state-card--failed"><strong>加载失败</strong><p>{{ errorMsg }}</p><button type="button" @click="load">重试</button></div>

    <div v-else-if="items.length === 0" class="state-card"><strong>没有找到相关内容</strong><p>换个关键词试试。</p></div>

    <div v-else class="entry-list">
      <article v-for="e in items" :key="e.id" class="entry-card" @click="goDetail(e.id)">
        <div class="card-top">
          <span class="card-category">{{ e.category }}</span>
          <span class="date-text">{{ formatDate(e.created_at) }}</span>
        </div>
        <h3>{{ e.title }}</h3>
        <p class="card-preview">{{ e.preview }}</p>
        <div class="card-tags">
          <span v-for="t in splitTags(e.tags)" :key="t" class="tag">{{ t }}</span>
        </div>
      </article>
    </div>

    <nav v-if="totalPages > 1" class="pager">
      <button type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
      <span>{{ page }} / {{ totalPages }}</span>
      <button type="button" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
    </nav>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getEntries } from '@/api/knowledge.js'
import { usePagedResource } from '@/composables/usePagedResource.js'
import { formatDate, splitTags } from '@/utils/display.js'

const router = useRouter()
const selectedCategory = ref('')
const searchText = ref('')
const {
  items,
  page,
  totalPages,
  isLoading,
  errorMsg,
  load,
  goPage,
  resetPage
} = usePagedResource(
  async currentPage => (await getEntries(currentPage, 20, selectedCategory.value, searchText.value)).data,
  { fallbackError: '\u52a0\u8f7d\u5931\u8d25' }
)

const categories = [
  '',
  '\u8bed\u6cd5',
  '\u8bcd\u6c47',
  '\u5199\u4f5c\u6280\u5de7',
  '\u6807\u70b9',
  '\u683c\u5f0f'
]

function doSearch() {
  resetPage()
  load()
}

function selectCategory(category) {
  selectedCategory.value = category
  resetPage()
  load()
}

function goDetail(id) {
  router.push(`/knowledge/${id}`)
}

onMounted(load)
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.knowledge-view {
  display: grid; gap: var(--space-5);
}
.page-head { display: flex; align-items: end; justify-content: space-between; }
.eyebrow { color: #f2c94c; font-size: 1.08rem; font-weight: 850; text-transform: uppercase; }
h1 { font-family: var(--font-serif); font-size: clamp(2rem, 3vw, 3.4rem); color: var(--color-text); }
.page-desc { max-width: 43rem; margin-top: var(--space-2); color: var(--color-text-muted); font-size: 1rem; line-height: var(--leading-relaxed); font-weight: 650; }
.fresh-note { width: max-content; max-width: 100%; margin-top: var(--space-3); border: 1px solid rgba(242, 201, 76, 0.28); border-radius: 999px; padding: 0.3rem 0.85rem; background: rgba(7, 25, 18, 0.48); color: #f2c94c; font-size: var(--text-small); font-weight: 850; box-shadow: inset 0 1px rgba(255,255,255,.06); }

.search-bar { display: flex; gap: var(--space-2); }
.search-bar input { flex: 1; height: 2.75rem; padding: 0 var(--space-4); border: 1px solid var(--color-border); border-radius: var(--radius-md); outline: none; font-size: var(--text-body); }
.search-bar input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(37,99,235,.12); }
.search-bar button { height: 2.75rem; padding: 0 var(--space-5); border-radius: var(--radius-md); background: rgba(49,92,61,.72); color: var(--color-text); font-weight: 800; }

.filter-tabs { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.filter-tabs button { height: 2.25rem; padding: 0 var(--space-4); border: 1px solid var(--color-border); border-radius: 999px; background: var(--color-surface); font-size: var(--text-small); font-weight: 700; color: var(--color-text-muted); }
.filter-tabs button.active { background: rgba(49,92,61,.62); border-color: rgba(233,255,207,.48); color: var(--color-text); }

.state-card { min-height: 20rem; display: grid; place-items: center; gap: var(--space-4); border: 1px solid rgba(151, 210, 166, 0.26); border-radius: var(--radius-lg); padding: var(--space-8); background: rgba(7, 25, 18, 0.58); text-align: center; box-shadow: var(--shadow-md); backdrop-filter: blur(24px) saturate(1.18); }
.state-card--failed strong { color: var(--color-danger); }
.pulse-ring { width: 2.5rem; height: 2.5rem; border: 3px solid var(--color-primary-100); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 900ms linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.entry-list { display: grid; gap: var(--space-3); }
.entry-card { display: grid; gap: var(--space-2); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: var(--space-5); box-shadow: var(--shadow-sm); cursor: pointer; transition: box-shadow var(--duration-fast); backdrop-filter: blur(24px) saturate(1.18); }
.entry-card:nth-child(5n+1) { background: rgba(14, 165, 233, 0.32); }
.entry-card:nth-child(5n+2) { background: rgba(168, 85, 247, 0.32); }
.entry-card:nth-child(5n+3) { background: rgba(34, 197, 94, 0.3); }
.entry-card:nth-child(5n+4) { background: rgba(249, 115, 22, 0.28); }
.entry-card:nth-child(5n+5) { background: rgba(6, 182, 212, 0.28); }
.entry-card:hover { box-shadow: var(--shadow-md); }
.card-top { display: flex; align-items: center; justify-content: space-between; }
.card-category { border: 1px solid rgba(32, 243, 232, 0.28); border-radius: 999px; padding: 2px 10px; font-size: var(--text-small); font-weight: 800; background: rgba(32, 243, 232, 0.14); color: #9ffcf6; }
.date-text { color: var(--color-text-muted); font-size: var(--text-small); }
h3 { font-family: var(--font-serif); font-size: 1.18rem; color: var(--color-text); }
.card-preview { color: var(--color-text-muted); font-size: 1rem; font-weight: 620; line-height: var(--leading-relaxed); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-tags { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.tag { border: 1px solid rgba(226, 239, 217, 0.2); border-radius: 999px; padding: 1px 8px; font-size: var(--text-small); background: rgba(7, 25, 18, 0.42); color: rgba(243, 248, 236, 0.86); box-shadow: inset 0 1px rgba(255, 255, 255, 0.06); }

.pager { display: flex; align-items: center; justify-content: center; gap: var(--space-4); }
.pager button { height: 2.5rem; padding: 0 var(--space-4); border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface); font-weight: 700; color: var(--color-text); }
.pager button:disabled { opacity: .4; cursor: not-allowed; }
.pager span { color: var(--color-text-muted); font-size: var(--text-caption); }
</style>
