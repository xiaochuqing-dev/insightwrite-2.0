<template>
  <section class="sub-page">
    <RouterLink to="/profile" class="back-link">← 返回</RouterLink>
    <h1>积分流水</h1>

    <div v-if="loading" class="state-card"><span class="pulse-ring"></span><p>加载中</p></div>
    <div v-else-if="error" class="state-card"><p>{{ error }}</p></div>
    <div v-else-if="items.length === 0" class="state-card"><p>暂无积分变动记录</p></div>
    <div v-else class="tx-list">
      <div v-for="t in items" :key="t.id" class="tx-row">
        <div><span class="tx-reason">{{ reasonLabel(t.reason) }}</span><span class="tx-date">{{ formatDate(t.created_at) }}</span></div>
        <span class="tx-amount" :class="t.amount > 0 ? 'is-plus' : 'is-minus'">{{ t.amount > 0 ? '+' : '' }}{{ t.amount }}</span>
        <span class="tx-balance">余额 {{ t.balance_after }}</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getTransactions } from '@/api/credits.js'
import { formatDate } from '@/utils/display.js'

const items = ref([])
const loading = ref(true)
const error = ref('')

function reasonLabel(r) {
  const m = { deduct_basic: '写作诊断消耗', deduct_quality: '高阶润色消耗', deduct_deep: '佳作精读消耗', refund_basic: '退款', refund_quality: '退款', refund_deep: '退款' }
  return m[r] || r
}

onMounted(async () => {
  try { const res = await getTransactions(1); items.value = res.data.items || [] }
  catch (e) { error.value = '加载失败' }
  finally { loading.value = false }
})
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.sub-page {
  display: grid; gap: var(--space-6); max-width: 40rem;
}
h1 { font-family: var(--font-serif); font-size: clamp(1.6rem, 3vw, 2.4rem); color: var(--gray-900); }

.state-card { min-height: 12rem; display: grid; place-items: center; gap: var(--space-3); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: var(--space-6); background: var(--color-surface); text-align: center; }
.pulse-ring { width: 2rem; height: 2rem; border: 3px solid var(--color-primary-100); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 900ms linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.tx-list { display: grid; gap: 2px; background: var(--color-border); border-radius: var(--radius-lg); overflow: hidden; }
.tx-row { display: grid; grid-template-columns: 1fr auto auto; align-items: center; gap: var(--space-4); padding: var(--space-4); background: var(--color-surface); }
.tx-reason { display: block; font-weight: 700; color: var(--color-text); font-size: 0.9rem; }
.tx-date { display: block; color: var(--color-text-muted); font-size: var(--text-small); }
.tx-amount { font-weight: 900; font-size: 1rem; }
.tx-amount.is-plus { color: var(--color-success); }
.tx-amount.is-minus { color: var(--color-danger); }
.tx-balance { color: var(--color-text-muted); font-size: var(--text-small); }
</style>
