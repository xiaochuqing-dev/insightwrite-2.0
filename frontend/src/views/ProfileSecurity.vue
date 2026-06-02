<template>
  <section class="security-view">
    <RouterLink to="/profile" class="back-link">← 返回</RouterLink>

    <header class="page-head">
      <p class="eyebrow">Security</p>
      <h1>账号安全</h1>
      <p>集中管理登录密码、积分流水和当前设备的登录状态。</p>
    </header>

    <div class="security-grid">
      <section class="security-panel security-panel--wide">
        <div class="section-head">
          <div>
            <p class="section-kicker">Password</p>
            <h2>修改密码</h2>
          </div>
          <span>{{ email || '当前账号邮箱' }}</span>
        </div>

        <div class="form-stack">
          <label>
            <span>验证码</span>
            <div class="input-group">
              <input v-model.trim="code" type="text" maxlength="6" inputmode="numeric" placeholder="6位数字" />
              <button type="button" :disabled="sending || countdown > 0 || !email" @click="sendResetCode">
                {{ countdown > 0 ? `${countdown}秒` : sending ? '发送中' : '发送验证码' }}
              </button>
            </div>
          </label>

          <label>
            <span>新密码</span>
            <input v-model="password" type="password" autocomplete="new-password" placeholder="至少6位" />
          </label>

          <label>
            <span>确认密码</span>
            <input v-model="password2" type="password" autocomplete="new-password" placeholder="再次输入" />
          </label>

          <button class="save-btn" type="button" :disabled="saving" @click="savePassword">
            {{ saving ? '修改中' : '修改密码' }}
          </button>
          <p v-if="message" :class="isOk ? 'ok' : 'err'">{{ message }}</p>
        </div>
      </section>

      <RouterLink to="/profile/transactions" class="security-panel security-link">
        <div>
          <p class="section-kicker">Credits</p>
          <h2>积分流水</h2>
          <p>查看积分消耗、返还和每日重置记录。</p>
        </div>
        <span aria-hidden="true">→</span>
      </RouterLink>

      <button type="button" class="security-panel security-link security-link--danger" @click="logout">
        <div>
          <p class="section-kicker">Session</p>
          <h2>退出登录</h2>
          <p>清除本机登录状态并返回登录页。</p>
        </div>
        <span aria-hidden="true">→</span>
      </button>
    </div>
  </section>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { forgotPassword, sendCode } from '@/api/auth.js'
import http from '@/api/index.js'

const router = useRouter()

const email = ref('')
const code = ref('')
const password = ref('')
const password2 = ref('')
const sending = ref(false)
const countdown = ref(0)
const saving = ref(false)
const message = ref('')
const isOk = ref(false)
let timer = null

async function sendResetCode() {
  sending.value = true
  message.value = ''
  try {
    await sendCode(email.value, 'reset_password')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    message.value = e?.response?.data?.error || '发送失败'
    isOk.value = false
  } finally {
    sending.value = false
  }
}

async function savePassword() {
  message.value = ''
  if (password.value.length < 6) {
    message.value = '密码至少6位'
    isOk.value = false
    return
  }
  if (password.value !== password2.value) {
    message.value = '两次输入不一致'
    isOk.value = false
    return
  }
  if (!code.value) {
    message.value = '请输入验证码'
    isOk.value = false
    return
  }
  saving.value = true
  try {
    await forgotPassword(email.value, code.value, password.value)
    message.value = '密码修改成功'
    isOk.value = true
    code.value = ''
    password.value = ''
    password2.value = ''
  } catch (e) {
    message.value = e?.response?.data?.error || '修改失败'
    isOk.value = false
  } finally {
    saving.value = false
  }
}

function logout() {
  localStorage.removeItem('token')
  router.push('/login')
}

onMounted(async () => {
  const response = await http.get('/auth/me')
  email.value = response.data.email || ''
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.security-view {
  display: grid;
  gap: var(--space-6);
}

.page-head {
  display: grid;
  gap: var(--space-2);
}

.eyebrow,
.section-kicker {
  color: #f2c94c;
  font-size: var(--text-small);
  font-weight: 850;
  letter-spacing: 0;
  text-transform: uppercase;
}

h1,
h2 {
  color: var(--color-text);
  font-family: var(--font-serif);
  line-height: var(--leading-tight);
}

h1 {
  font-size: clamp(2rem, 4vw, 3.5rem);
}

h2 {
  font-size: clamp(1.35rem, 2vw, 1.85rem);
}

.page-head p:last-child,
.security-link p {
  color: var(--color-text-muted);
  font-size: var(--text-caption);
  font-weight: 720;
}

.security-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-5);
}

.security-panel {
  border: 1px solid rgba(151, 210, 166, 0.2);
  border-radius: var(--radius-lg);
  padding: clamp(var(--space-5), 3vw, var(--space-6));
  background: rgba(7, 25, 18, 0.56);
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(24px) saturate(1.14);
}

.security-panel--wide {
  grid-column: 1 / -1;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.section-head > span {
  color: var(--color-text-muted);
  font-size: var(--text-small);
  font-weight: 800;
  overflow-wrap: anywhere;
}

.form-stack {
  display: grid;
  gap: var(--space-4);
  max-width: 42rem;
}

.form-stack label {
  display: grid;
  gap: var(--space-2);
}

.form-stack label > span {
  color: var(--color-text-muted);
  font-size: var(--text-caption);
  font-weight: 800;
}

.input-group {
  display: flex;
  gap: var(--space-2);
}

.form-stack input {
  width: 100%;
  min-width: 0;
  height: 2.75rem;
  border: 1px solid rgba(151, 210, 166, 0.26);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: rgba(4, 16, 11, 0.48);
  color: var(--color-text);
  outline: none;
}

.form-stack input:focus {
  border-color: rgba(32, 243, 232, 0.48);
  box-shadow: 0 0 0 3px rgba(32, 243, 232, 0.12);
}

.input-group button,
.save-btn {
  height: 2.75rem;
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: rgba(32, 243, 232, 0.16);
  color: #9ffcf6;
  font-size: var(--text-caption);
  font-weight: 900;
  white-space: nowrap;
}

.save-btn {
  justify-self: start;
}

.ok,
.err {
  font-size: var(--text-small);
  font-weight: 850;
}

.ok {
  color: #9fffc6;
}

.err {
  color: #ffb4b4;
}

.security-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-5);
  color: inherit;
  text-align: left;
}

.security-link:hover {
  border-color: rgba(32, 243, 232, 0.36);
  background: rgba(14, 45, 31, 0.64);
}

.security-link--danger:hover {
  border-color: rgba(239, 68, 68, 0.36);
  background: rgba(88, 25, 29, 0.44);
}

.security-link > span {
  width: 2.6rem;
  height: 2.6rem;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  background: rgba(32, 243, 232, 0.1);
  color: #9ffcf6;
  font-size: 1.35rem;
  font-weight: 900;
}

@media (max-width: 760px) {
  .security-grid {
    grid-template-columns: 1fr;
  }

  .section-head,
  .security-link {
    align-items: flex-start;
    flex-direction: column;
  }

  .input-group {
    flex-direction: column;
  }
}
</style>
