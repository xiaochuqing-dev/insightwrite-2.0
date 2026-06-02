<template>
  <section class="sub-page">
    <RouterLink to="/profile" class="back-link">← 返回</RouterLink>
    <h1>修改密码</h1>

    <div class="section">
      <div class="form-row">
        <label>验证码</label>
        <div class="input-group">
          <input v-model="code" type="text" maxlength="6" placeholder="6位数字" />
          <button type="button" :disabled="sending || countdown > 0" @click="sendCode">{{ countdown > 0 ? `${countdown}秒` : sending ? '发送中' : '发送验证码' }}</button>
        </div>
      </div>
      <div class="form-row">
        <label>新密码</label>
        <input v-model="pwd" type="password" placeholder="至少6位" />
      </div>
      <div class="form-row">
        <label>确认密码</label>
        <input v-model="pwd2" type="password" placeholder="再次输入" />
      </div>
      <button class="save-btn" type="button" :disabled="saving" @click="save">{{ saving ? '修改中' : '修改密码' }}</button>
      <p v-if="msg" :class="ok ? 'ok' : 'err'">{{ msg }}</p>
    </div>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { sendCode as apiSendCode, forgotPassword } from '@/api/auth.js'
import http from '@/api/index.js'

const email = ref('')
const code = ref('')
const pwd = ref('')
const pwd2 = ref('')
const sending = ref(false)
const countdown = ref(0)
const saving = ref(false)
const msg = ref('')
const ok = ref(false)
let timer = null

async function sendCode() {
  sending.value = true
  try { await apiSendCode(email.value, 'reset_password'); countdown.value = 60; timer = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(timer) }, 1000) }
  catch { msg.value = '发送失败'; ok.value = false }
  finally { sending.value = false }
}

async function save() {
  msg.value = ''
  if (pwd.value.length < 6) { msg.value = '密码至少6位'; ok.value = false; return }
  if (pwd.value !== pwd2.value) { msg.value = '两次输入不一致'; ok.value = false; return }
  if (!code.value) { msg.value = '请输入验证码'; ok.value = false; return }
  saving.value = true
  try { await forgotPassword(email.value, code.value, pwd.value); msg.value = '密码修改成功'; ok.value = true }
  catch (e) { msg.value = e?.response?.data?.error || '修改失败'; ok.value = false }
  finally { saving.value = false }
}

onMounted(async () => {
  const res = await http.get('/auth/me')
  email.value = res.data.email || ''
})
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.sub-page {
  display: grid; gap: var(--space-6); max-width: 32rem;
}
h1 { font-family: var(--font-serif); font-size: clamp(1.6rem, 3vw, 2.4rem); color: var(--gray-900); }

.section { border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: var(--space-5); background: var(--color-surface); box-shadow: var(--shadow-sm); display: grid; gap: var(--space-3); }
.form-row { display: flex; align-items: center; gap: var(--space-3); }
.form-row label { width: 4.5rem; color: var(--color-text-muted); font-size: var(--text-caption); font-weight: 700; flex-shrink: 0; }
.form-row input { flex: 1; height: 2.5rem; padding: 0 var(--space-3); border: 1px solid var(--color-border); border-radius: var(--radius-md); outline: none; font-size: 0.95rem; }
.form-row input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(37,99,235,.12); }
.input-group { display: flex; gap: var(--space-2); flex: 1; }
.input-group input { flex: 1; height: 2.5rem; padding: 0 var(--space-3); border: 1px solid var(--color-border); border-radius: var(--radius-md); outline: none; font-size: 0.95rem; }
.input-group input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(37,99,235,.12); }
.input-group button { height: 2.5rem; padding: 0 var(--space-3); border-radius: var(--radius-md); background: var(--gray-900); color: white; font-weight: 700; font-size: var(--text-small); white-space: nowrap; }
.input-group button:disabled, .save-btn:disabled { opacity: .4; }
.save-btn { height: 2.75rem; padding: 0 var(--space-4); border-radius: var(--radius-md); background: var(--gray-900); color: white; font-weight: 700; justify-self: start; }
.ok { color: var(--color-success); font-size: var(--text-caption); }
.err { color: var(--color-danger); font-size: var(--text-caption); }
</style>
