<template>
  <section class="history-view">
    <div class="page-head">
      <div>
        <p class="eyebrow">History</p>
        <h1>分析历史</h1>
      </div>
      <span v-if="total > 0" class="count-badge">{{ total }} 条记录</span>
    </div>

    <div v-if="isLoading" class="state-card">
      <span class="pulse-ring" aria-hidden="true"></span>
      <p>正在加载历史记录</p>
    </div>

    <div v-else-if="errorMsg" class="state-card state-card--failed">
      <strong>加载失败</strong>
      <p>{{ errorMsg }}</p>
      <button type="button" @click="loadHistory">重试</button>
    </div>

    <div v-else-if="items.length === 0" class="state-card">
      <strong>还没有分析记录</strong>
      <p>去分析工作台粘贴一篇英语文章试试吧。</p>
      <RouterLink to="/home">开始分析</RouterLink>
    </div>

    <template v-else>
      <div class="history-list">
        <article
          v-for="item in items"
          :key="item.id"
          class="history-card"
          @click="goDetail(item.id)"
        >
          <div class="card-top">
            <span class="mode-tag" :class="`mode-tag--${item.mode}`">{{ modeLabel(item.mode) }}</span>
            <span v-if="isActiveStatus(item.status)" class="status-badge status-badge--pending">分析中</span>
            <span class="date-text">{{ formatDate(item.created_at) }}</span>
          </div>
          <p class="card-preview">{{ item.preview }}</p>
          <div class="card-meta">
            <span>{{ item.word_count || 0 }} 词</span>
            <span v-if="item.is_favorite" class="fav-mark">♥ 已收藏</span>
          </div>
          <button
            class="favorite-action"
            type="button"
            :class="{ 'is-active': item.is_favorite }"
            @click.stop="toggleFavorite(item)"
          >
            {{ item.is_favorite ? '取消收藏' : '收藏' }}
          </button>
        </article>
      </div>

      <nav class="pager" v-if="totalPages > 1">
        <button type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button type="button" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
      </nav>
    </template>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { getHistory } from '@/api/history.js'
import { addHistoryFavorite, removeHistoryFavorite } from '@/api/favorites.js'
import { usePagedResource } from '@/composables/usePagedResource.js'
import { formatDate, modeLabel } from '@/utils/display.js'

const router = useRouter()
const {
  items,
  page,
  total,
  totalPages,
  isLoading,
  errorMsg,
  load: loadHistory,
  goPage
} = usePagedResource(
  async currentPage => (await getHistory(currentPage)).data,
  { fallbackError: '\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5' }
)

function isActiveStatus(status) {
  return status === 'pending' || status === 'processing'
}

function goDetail(id) {
  const item = items.value.find(entry => entry.id === id)
  router.push(item && isActiveStatus(item.status) ? `/analyze/${id}` : `/history/${id}`)
}

async function toggleFavorite(item) {
  const response = item.is_favorite
    ? await removeHistoryFavorite(item.id)
    : await addHistoryFavorite(item.id)
  item.is_favorite = Boolean(response.data?.is_favorite)
}

onMounted(loadHistory)
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.history-view {
  display: grid; gap: var(--space-6);
}

.page-head { display: flex; align-items: end; justify-content: space-between; gap: var(--space-4); }
.eyebrow { color: #f2c94c; font-size: 1.08rem; font-weight: 850; text-transform: uppercase; }
h1 { font-family: var(--font-serif); font-size: clamp(2rem, 3vw, 3.4rem); line-height: var(--leading-tight); color: var(--color-text); }
.count-badge { color: var(--color-text-muted); font-size: var(--text-caption); }

.state-card {
  min-height: 22rem; display: grid; place-items: center; gap: var(--space-4);
  border: 1px solid var(--color-border); border-radius: var(--radius-lg);
  padding: var(--space-8); background: var(--color-surface); text-align: center; box-shadow: var(--shadow-md); backdrop-filter: blur(24px) saturate(1.18);
}
.state-card--failed strong { color: var(--color-danger); }
.state-card a, .state-card button { font-weight: 800; color: var(--color-primary); }
.pulse-ring { width: 2.5rem; height: 2.5rem; border: 3px solid var(--color-primary-100); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 900ms linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.history-list { display: grid; gap: var(--space-3); }
.history-card {
  position: relative;
  display: grid; gap: var(--space-3); border: 1px solid var(--color-border); border-radius: var(--radius-lg);
  padding: var(--space-5) 8.5rem var(--space-5) var(--space-5); background: var(--color-surface); box-shadow: var(--shadow-sm); backdrop-filter: blur(24px) saturate(1.18);
  cursor: pointer; transition: box-shadow var(--duration-fast) var(--ease-out);
}
.history-card:hover { box-shadow: var(--shadow-md); }
.card-top { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); }
.mode-tag { border-radius: 999px; padding: 2px 10px; font-size: var(--text-small); font-weight: 800; }
.mode-tag--basic { background: var(--color-primary-50); color: var(--color-primary); }
.mode-tag--quality { background: var(--color-secondary-50); color: var(--color-secondary); }
.mode-tag--deep { background: #f5f3ff; color: #7c3aed; }
.date-text { color: var(--color-text-muted); font-size: var(--text-small); }
.card-preview { color: var(--color-text); font-size: 1.02rem; font-weight: 650; line-height: var(--leading-relaxed); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-meta { display: flex; gap: var(--space-4); color: var(--color-text-muted); font-size: var(--text-small); }
.fav-mark { color: var(--color-danger); }
.favorite-action { position: absolute; right: var(--space-5); top: 50%; transform: translateY(-50%); height: 2.45rem; min-width: 5.8rem; padding: 0 var(--space-3); border: 1px solid rgba(151,210,166,.28); border-radius: var(--radius-md); background: rgba(7,25,18,.48); color: var(--color-text-muted); font-size: var(--text-small); font-weight: 850; }
.favorite-action.is-active { border-color: rgba(242, 201, 76, .46); background: rgba(242, 201, 76, .14); color: #f2c94c; }
.favorite-action:hover { box-shadow: var(--shadow-sm); }

.pager { display: flex; align-items: center; justify-content: center; gap: var(--space-4); }
.pager button { height: 2.5rem; padding: 0 var(--space-4); border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface); font-weight: 700; color: var(--color-text); }
.pager button:disabled { opacity: .4; cursor: not-allowed; }
.pager span { color: var(--color-text-muted); font-size: var(--text-caption); }
@media (max-width: 700px) {
  .history-card { padding: var(--space-5); }
  .favorite-action { position: static; transform: none; width: max-content; }
}
</style>
.status-badge{border-radius:999px;padding:1px 8px;font-size:var(--text-small);font-weight:800}.status-badge--pending{background:#fef3c7;color:#d97706;animation:pulse 2s infinite}@keyframes pulse{0%,100%{opacity:1}50%{opacity:.6}}
