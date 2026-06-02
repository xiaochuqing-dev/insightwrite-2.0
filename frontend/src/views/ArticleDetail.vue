<template>
  <section class="article-detail">
    <RouterLink to="/articles" class="back-link">← 返回</RouterLink>

    <div v-if="isLoading" class="state-card"><span class="pulse-ring" aria-hidden="true"></span><p>加载中</p></div>
    <div v-else-if="errorMsg" class="state-card state-card--failed"><strong>加载失败</strong><p>{{ errorMsg }}</p><RouterLink to="/articles" class="back-link">← 返回</RouterLink></div>

    <template v-else>
      <header class="article-header">
        <span class="card-category">{{ article.category }}</span>
        <h1>{{ article.title }}</h1>
        <div class="article-meta">
          <span>{{ article.source }}</span>
          <span>{{ article.word_count || 0 }} 词</span>
          <span>{{ article.view_count || 0 }} 次阅读</span>
          <span>{{ formatDate(article.created_at) }}</span>
        </div>
      </header>
      <div class="reading-tip">
        <span>想学习这篇文章的表达方式？可直接带入“佳作精读”模式拆解。</span>
        <button type="button" @click="analyzeArticle">一键分析</button>
      </div>
      <article class="article-body">{{ article.content }}</article>
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { getArticleDetail } from '@/api/articles.js'
import { formatDate } from '@/utils/display.js'

const route = useRoute()
const router = useRouter()
const article = ref({})
const isLoading = ref(true)
const errorMsg = ref('')

function analyzeArticle() {
  localStorage.setItem('insightwrite:essay-draft', article.value.content || '')
  localStorage.setItem('insightwrite:essay-topic', article.value.title || '')
  localStorage.setItem('insightwrite:essay-mode', 'deep')
  localStorage.removeItem('insightwrite:essay-custom')
  router.push('/home')
}

onMounted(async () => {
  try {
    const res = await getArticleDetail(route.params.id)
    article.value = res.data || {}
  } catch (e) {
    errorMsg.value = e?.response?.data?.error || '加载失败'
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.article-detail {
  display: grid; gap: var(--space-6); max-width: 58rem;
}

.state-card { min-height: 20rem; display: grid; place-items: center; gap: var(--space-4); border: 1px solid rgba(151, 210, 166, 0.26); border-radius: var(--radius-lg); padding: var(--space-8); background: rgba(7, 25, 18, 0.58); text-align: center; box-shadow: var(--shadow-md); backdrop-filter: blur(24px) saturate(1.18); }
.state-card--failed strong { color: var(--color-danger); }
.pulse-ring { width: 2.5rem; height: 2.5rem; border: 3px solid var(--color-primary-100); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 900ms linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.reading-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  border: 1px solid rgba(32, 243, 232, 0.24);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  background: rgba(7, 25, 18, 0.48);
  color: var(--color-text-muted);
  font-size: var(--text-caption);
  font-weight: 700;
}
.reading-tip button {
  flex: 0 0 auto;
  height: 2.4rem;
  padding: 0 var(--space-4);
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgba(32, 243, 232, 0.28), rgba(34, 197, 94, 0.22));
  color: #e9ffcf;
  font-weight: 850;
}
.reading-tip button:hover { box-shadow: var(--shadow-sm); transform: translateY(-1px); }

.article-header { display: grid; gap: var(--space-3); padding: var(--space-5) var(--space-6); border: 1px solid rgba(151, 210, 166, 0.2); border-radius: var(--radius-lg); background: linear-gradient(135deg, rgba(7, 25, 18, 0.56), rgba(12, 49, 40, 0.32)); box-shadow: var(--shadow-sm); backdrop-filter: blur(22px) saturate(1.18); }
.card-category { justify-self: start; border: 1px solid rgba(32, 243, 232, 0.28); border-radius: 999px; padding: 2px 10px; font-size: var(--text-small); font-weight: 800; background: rgba(32, 243, 232, 0.13); color: #8ffcf5; }
h1 { font-family: var(--font-serif); font-size: clamp(1.8rem, 3vw, 2.8rem); color: var(--color-text); line-height: var(--leading-tight); text-shadow: 0 2px 18px rgba(0, 0, 0, 0.32); }
.article-meta { display: flex; flex-wrap: wrap; gap: var(--space-4); color: var(--color-text-muted); font-size: var(--text-caption); }
.article-body { border: 1px solid rgba(151, 210, 166, 0.24); border-radius: var(--radius-lg); padding: clamp(var(--space-5), 3vw, var(--space-8)); background: linear-gradient(135deg, rgba(7, 25, 18, 0.58), rgba(13, 46, 34, 0.4)); color: var(--color-text); font-family: var(--font-serif); font-size: 1.1rem; line-height: 2; white-space: pre-wrap; box-shadow: var(--shadow-md); backdrop-filter: blur(28px) saturate(1.2); }
@media (max-width: 620px) {
  .reading-tip { align-items: stretch; flex-direction: column; }
}
</style>
