<template>
  <section class="articles-view">
    <div class="page-head">
      <div>
        <p class="eyebrow">Reading Bank</p>
        <h1>佳作精选</h1>
        <p class="page-desc">阅读高质量英语文章，也可以复制到工作台用“佳作精读”拆解表达、长难句和写作技巧。</p>
        <p class="fresh-note">内容每周不定时更新，适合反复回来看新题材和新表达。</p>
      </div>
    </div>

    <div class="filter-tabs">
      <button v-for="c in categories" :key="c.value" type="button"
        :class="{ active: selectedCategory === c.value }" @click="selectCategory(c.value)">
        {{ c.label }}
      </button>
    </div>

    <div v-if="isLoading" class="state-card"><span class="pulse-ring" aria-hidden="true"></span><p>加载中</p></div>
    <div v-else-if="errorMsg" class="state-card state-card--failed"><strong>加载失败</strong><p>{{ errorMsg }}</p><button type="button" @click="load">重试</button></div>

    <div v-else class="article-grid">
      <article v-for="a in items" :key="a.id" class="article-card" @click="goDetail(a.id)">
        <span class="card-category">{{ a.category }}</span>
        <h3>{{ a.title }}</h3>
        <p class="card-preview">{{ a.preview }}</p>
        <div class="card-meta">
          <span>{{ a.source }}</span>
          <span>{{ a.word_count || 0 }} 词</span>
          <span>{{ a.view_count || 0 }} 次阅读</span>
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
import { getArticles } from '@/api/articles.js'
import { usePagedResource } from '@/composables/usePagedResource.js'

const router = useRouter()
const selectedCategory = ref('')
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
  async currentPage => (await getArticles(currentPage, 20, selectedCategory.value)).data,
  { fallbackError: '\u52a0\u8f7d\u5931\u8d25' }
)

const categories = [
  { value: '', label: '\u5168\u90e8' },
  { value: '\u8003\u7814\u82f1\u8bed', label: '\u8003\u7814\u82f1\u8bed' },
  { value: '\u56db\u516d\u7ea7', label: '\u56db\u516d\u7ea7' },
  { value: '\u96c5\u601d', label: '\u96c5\u601d' },
  { value: '\u6258\u798f', label: '\u6258\u798f' },
  { value: 'AI\u4e0e\u6559\u80b2', label: 'AI\u4e0e\u6559\u80b2' },
  { value: '\u73af\u5883\u4fdd\u62a4', label: '\u73af\u5883\u4fdd\u62a4' }
]

function selectCategory(category) {
  selectedCategory.value = category
  resetPage()
  load()
}

function goDetail(id) {
  router.push(`/articles/${id}`)
}

onMounted(load)
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.articles-view {
  display: grid; gap: var(--space-6);
}
.page-head { display: flex; align-items: end; justify-content: space-between; }
.eyebrow { color: #f2c94c; font-size: 1.08rem; font-weight: 850; text-transform: uppercase; }
h1 { font-family: var(--font-serif); font-size: clamp(2rem, 3vw, 3.4rem); color: var(--color-text); }
.page-desc { max-width: 43rem; margin-top: var(--space-2); color: var(--color-text-muted); font-size: 1rem; line-height: var(--leading-relaxed); font-weight: 650; }
.fresh-note { width: max-content; max-width: 100%; margin-top: var(--space-3); border: 1px solid rgba(242, 201, 76, 0.28); border-radius: 999px; padding: 0.3rem 0.85rem; background: rgba(7, 25, 18, 0.48); color: #f2c94c; font-size: var(--text-small); font-weight: 850; box-shadow: inset 0 1px rgba(255,255,255,.06); }

.filter-tabs { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.filter-tabs button { height: 2.25rem; padding: 0 var(--space-4); border: 1px solid var(--color-border); border-radius: 999px; background: var(--color-surface); font-size: var(--text-small); font-weight: 700; color: var(--color-text-muted); transition: all var(--duration-fast) var(--ease-out); }
.filter-tabs button.active { background: rgba(49,92,61,.62); border-color: rgba(233,255,207,.48); color: var(--color-text); }

.state-card { min-height: 20rem; display: grid; place-items: center; gap: var(--space-4); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: var(--space-8); background: var(--color-surface); text-align: center; box-shadow: var(--shadow-md); backdrop-filter: blur(24px) saturate(1.18); }
.state-card--failed strong { color: var(--color-danger); }
.pulse-ring { width: 2.5rem; height: 2.5rem; border: 3px solid var(--color-primary-100); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 900ms linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.article-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(18rem, 1fr)); gap: var(--space-4); }
.article-card { display: grid; gap: var(--space-3); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: var(--space-5); box-shadow: var(--shadow-sm); cursor: pointer; transition: box-shadow var(--duration-fast); backdrop-filter: blur(24px) saturate(1.18); }
.article-card:nth-child(5n+1) { background: rgba(16, 185, 129, 0.35); }
.article-card:nth-child(5n+2) { background: rgba(37, 99, 235, 0.3); }
.article-card:nth-child(5n+3) { background: rgba(245, 158, 11, 0.3); }
.article-card:nth-child(5n+4) { background: rgba(139, 92, 246, 0.3); }
.article-card:nth-child(5n+5) { background: rgba(236, 72, 153, 0.28); }
.article-card:hover { box-shadow: var(--shadow-md); }
.card-category { justify-self: start; border-radius: 999px; padding: 2px 10px; font-size: var(--text-small); font-weight: 800; background: var(--color-primary-50); color: var(--color-primary); }
h3 { font-family: var(--font-serif); font-size: 1.18rem; line-height: var(--leading-tight); color: var(--color-text); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-preview { color: var(--color-text-muted); font-size: 1rem; font-weight: 620; line-height: var(--leading-relaxed); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-meta { display: flex; flex-wrap: wrap; gap: var(--space-3); color: var(--color-text-subtle); font-size: var(--text-small); }

.pager { display: flex; align-items: center; justify-content: center; gap: var(--space-4); }
.pager button { height: 2.5rem; padding: 0 var(--space-4); border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface); font-weight: 700; color: var(--color-text); }
.pager button:disabled { opacity: .4; cursor: not-allowed; }
.pager span { color: var(--color-text-muted); font-size: var(--text-caption); }
</style>
