<template>
  <section class="detail-view">
    <RouterLink to="/knowledge" class="back-link">← 返回</RouterLink>

    <div v-if="isLoading" class="state-card"><span class="pulse-ring" aria-hidden="true"></span><p>加载中</p></div>
    <div v-else-if="errorMsg" class="state-card state-card--failed"><strong>加载失败</strong><p>{{ errorMsg }}</p><RouterLink to="/knowledge" class="back-link">← 返回</RouterLink></div>

    <template v-else>
      <header class="detail-header">
        <div class="detail-header__top">
          <span class="card-category">{{ entry.category }}</span>
          <button class="favorite-button" type="button" :class="{ 'is-active': isFavorite }" @click="toggleFavorite">
            {{ isFavorite ? '取消收藏' : '收藏学习' }}
          </button>
        </div>
        <h1>{{ entry.title }}</h1>
        <div class="tag-row">
          <span v-for="t in splitTags(entry.tags)" :key="t" class="tag">{{ t }}</span>
          <span class="view-count">{{ entry.view_count || 0 }} 次阅读</span>
        </div>
      </header>
      <article class="detail-body">{{ entry.content }}</article>
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { getEntryDetail } from '@/api/knowledge.js'
import { addKnowledgeFavorite, removeKnowledgeFavorite } from '@/api/favorites.js'
import { splitTags } from '@/utils/display.js'

const route = useRoute()
const entry = ref({})
const isFavorite = ref(false)
const isLoading = ref(true)
const errorMsg = ref('')
const isTogglingFavorite = ref(false)

async function toggleFavorite() {
  if (isTogglingFavorite.value || !entry.value.id) return
  isTogglingFavorite.value = true
  try {
    const response = isFavorite.value
      ? await removeKnowledgeFavorite(entry.value.id)
      : await addKnowledgeFavorite(entry.value.id)
    isFavorite.value = Boolean(response.data?.is_favorite)
  } catch (e) {
    errorMsg.value = e?.response?.data?.error || '收藏操作失败'
  } finally {
    isTogglingFavorite.value = false
  }
}


onMounted(async () => {
  try {
    const res = await getEntryDetail(route.params.id)
    entry.value = res.data || {}
    isFavorite.value = Boolean(res.data?.is_favorite)
  } catch (e) {
    errorMsg.value = e?.response?.data?.error || '加载失败'
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.detail-view {
  display: grid; gap: var(--space-6); max-width: 56rem;
}

.state-card { min-height: 20rem; display: grid; place-items: center; gap: var(--space-4); border: 1px solid rgba(151, 210, 166, 0.26); border-radius: var(--radius-lg); padding: var(--space-8); background: rgba(7, 25, 18, 0.58); text-align: center; box-shadow: var(--shadow-md); backdrop-filter: blur(24px) saturate(1.18); }
.state-card--failed strong { color: var(--color-danger); }
.pulse-ring { width: 2.5rem; height: 2.5rem; border: 3px solid var(--color-primary-100); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 900ms linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.detail-header { display: grid; gap: var(--space-3); padding: var(--space-5) var(--space-6); border: 1px solid rgba(151, 210, 166, 0.22); border-radius: var(--radius-lg); background: linear-gradient(135deg, rgba(7, 25, 18, 0.62), rgba(10, 54, 43, 0.36)); box-shadow: var(--shadow-sm); backdrop-filter: blur(24px) saturate(1.2); }
.detail-header__top { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); }
.card-category { justify-self: start; border: 1px solid rgba(32, 243, 232, 0.28); border-radius: 999px; padding: 2px 10px; font-size: var(--text-small); font-weight: 800; background: rgba(32, 243, 232, 0.13); color: #8ffcf5; }
.favorite-button { height: 2.35rem; padding: 0 var(--space-4); border: 1px solid rgba(151, 210, 166, 0.26); border-radius: var(--radius-md); background: rgba(7, 25, 18, 0.5); color: var(--color-text-muted); font-size: var(--text-small); font-weight: 850; }
.favorite-button.is-active { border-color: rgba(242, 201, 76, 0.46); background: rgba(242, 201, 76, 0.14); color: #f2c94c; }
.favorite-button:hover { transform: translateY(-1px); box-shadow: var(--shadow-sm); }
h1 { font-family: var(--font-serif); font-size: clamp(1.6rem, 3vw, 2.6rem); color: var(--color-text); line-height: var(--leading-tight); text-shadow: 0 2px 18px rgba(0, 0, 0, 0.32); }
.tag-row { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2); }
.tag { border: 1px solid rgba(226, 239, 217, 0.22); border-radius: 999px; padding: 1px 8px; font-size: var(--text-small); background: rgba(7, 25, 18, 0.46); color: rgba(243, 248, 236, 0.88); box-shadow: inset 0 1px rgba(255, 255, 255, 0.06); }
.view-count { margin-left: auto; color: var(--color-text-muted); font-size: var(--text-small); }
.detail-body { border: 1px solid rgba(151, 210, 166, 0.24); border-radius: var(--radius-lg); padding: clamp(var(--space-5), 3vw, var(--space-8)); background: linear-gradient(135deg, rgba(7, 25, 18, 0.6), rgba(11, 43, 31, 0.42)); color: var(--color-text); font-size: 1.05rem; line-height: 2; white-space: pre-wrap; box-shadow: var(--shadow-md); backdrop-filter: blur(28px) saturate(1.2); }
</style>
