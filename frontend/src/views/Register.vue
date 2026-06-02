<template>
  <form class="auth-form" novalidate @submit.prevent="handleSubmit">
    <div class="auth-heading">
      <p>创建账号</p>
      <h1>开启英语写作诊断与佳作精读。</h1>
    </div>

    <p v-if="serverError" class="form-alert">{{ serverError }}</p>

    <label class="field">
      <span>用户名</span>
      <input v-model.trim="form.username" type="text" autocomplete="username" placeholder="请输入用户名" @blur="validateField('username')" />
      <small v-if="errors.username">{{ errors.username }}</small>
    </label>

    <label class="field">
      <span>邮箱</span>
      <input v-model.trim="form.email" type="email" autocomplete="email" placeholder="请输入邮箱" @blur="validateField('email')" />
      <small v-if="errors.email">{{ errors.email }}</small>
    </label>

    <label class="field">
      <span>密码</span>
      <div class="password-field">
        <input
          v-model="form.password"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="new-password"
          @blur="validateField('password')"
        />
        <button type="button" @click="showPassword = !showPassword">
          {{ showPassword ? '隐藏' : '显示' }}
        </button>
      </div>
      <small v-if="errors.password">{{ errors.password }}</small>
    </label>

    <label class="field">
      <span>确认密码</span>
      <input
        v-model="form.confirmPassword"
        type="password"
        autocomplete="new-password"
        @blur="validateField('confirmPassword')"
      />
      <small v-if="errors.confirmPassword">{{ errors.confirmPassword }}</small>
    </label>

    <label class="field">
      <span>验证码</span>
      <div class="code-field">
        <input
          v-model.trim="form.code"
          inputmode="numeric"
          maxlength="6"
          placeholder="6 位数字"
          @blur="validateField('code')"
        />
        <button type="button" :disabled="isSendingCode || countdown > 0" @click="handleSendCode">
          {{ countdown > 0 ? `${countdown}秒后重发` : isSendingCode ? '发送中...' : '发送验证码' }}
        </button>
      </div>
      <small v-if="errors.code">{{ errors.code }}</small>
    </label>

    <button class="primary-action" type="submit" :disabled="isSubmitting">
      {{ isSubmitting ? '创建中...' : '创建账号' }}
    </button>

    <div class="form-links">
      <span>
        已有账号？
        <RouterLink to="/login">去登录</RouterLink>
      </span>
    </div>
  </form>
</template>

<script setup>
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { register, sendCode } from '@/api/auth.js'

const router = useRouter()
const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  code: ''
})
const errors = reactive({})
const serverError = ref('')
const isSubmitting = ref(false)
const isSendingCode = ref(false)
const showPassword = ref(false)
const countdown = ref(0)
let timerId = null

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const codePattern = /^\d{6}$/

function validateField(field) {
  errors[field] = ''

  if (field === 'username') {
    if (!form.username) errors.username = '请输入用户名。'
    else if (form.username.length < 2 || form.username.length > 20) errors.username = '用户名长度需为 2-20 个字符。'
  }

  if (field === 'email') {
    if (!form.email) errors.email = '请输入邮箱。'
    else if (!emailPattern.test(form.email)) errors.email = '请输入有效的邮箱地址。'
  }

  if (field === 'password') {
    if (!form.password) errors.password = '请输入密码。'
    else if (form.password.length < 6) errors.password = '密码至少需要 6 个字符。'
    if (form.confirmPassword) validateField('confirmPassword')
  }

  if (field === 'confirmPassword') {
    if (!form.confirmPassword) errors.confirmPassword = '请再次输入密码。'
    else if (form.confirmPassword !== form.password) errors.confirmPassword = '两次输入的密码不一致。'
  }

  if (field === 'code') {
    if (!form.code) errors.code = '请输入验证码。'
    else if (!codePattern.test(form.code)) errors.code = '验证码需为 6 位数字。'
  }
}

function validateForm() {
  ;['username', 'email', 'password', 'confirmPassword', 'code'].forEach(validateField)
  return Object.values(errors).every(error => !error)
}

function startCountdown() {
  countdown.value = 60
  clearInterval(timerId)
  timerId = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(timerId)
  }, 1000)
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.error || fallback
}

async function handleSendCode() {
  serverError.value = ''
  validateField('email')
  if (errors.email) return

  isSendingCode.value = true
  try {
    await sendCode(form.email, 'register')
    startCountdown()
  } catch (error) {
    serverError.value = getErrorMessage(error, '验证码发送失败，请稍后再试。')
  } finally {
    isSendingCode.value = false
  }
}

async function handleSubmit() {
  serverError.value = ''
  if (!validateForm()) return

  isSubmitting.value = true
  try {
    const response = await register(form.email, form.username, form.password, form.code)
    const token = response.data?.token
    if (!token) throw new Error('Missing token')
    localStorage.setItem('token', token)
    await router.push('/home')
  } catch (error) {
    serverError.value = getErrorMessage(error, '账号创建失败，请检查信息后重试。')
  } finally {
    isSubmitting.value = false
  }
}

onBeforeUnmount(() => clearInterval(timerId))
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.auth-form {
  display: grid;
  gap: var(--space-4);
}

.auth-heading {
  display: grid;
  gap: var(--space-2);
}

.auth-heading p {
  color: var(--color-primary);
  font-size: var(--text-caption);
  font-weight: 750;
  text-transform: uppercase;
}

.auth-heading h1 {
  font-family: var(--font-serif);
  font-size: var(--text-title);
  line-height: var(--leading-tight);
}

.field {
  display: grid;
  gap: var(--space-2);
}

.field span {
  color: var(--gray-700);
  font-size: var(--text-caption);
  font-weight: 700;
}

.field input {
  width: 100%;
  height: 3rem;
  padding: 0 var(--space-4);
  outline: none;
}

.field input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

.field small {
  color: var(--color-danger);
  font-size: var(--text-small);
}

.password-field,
.code-field {
  position: relative;
}

.password-field input {
  padding-right: 4.75rem;
}

.code-field input {
  padding-right: 8.5rem;
}

.password-field button,
.code-field button {
  position: absolute;
  top: 50%;
  right: var(--space-2);
  transform: translateY(-50%);
  border-radius: var(--radius-sm);
  padding: var(--space-1) var(--space-2);
  color: var(--color-primary);
  font-size: var(--text-small);
  font-weight: 750;
}

.form-alert {
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  background: var(--color-danger-50);
  color: var(--color-danger);
  font-size: var(--text-caption);
}

.primary-action {
  height: 3.125rem;
  border-radius: var(--radius-md);
  background: var(--gray-900);
  color: white;
  font-weight: 800;
  box-shadow: var(--shadow-md);
}

.primary-action:hover:not(:disabled) {
  background: var(--color-primary-700);
}

.form-links {
  color: var(--color-text-muted);
  font-size: var(--text-caption);
}
</style>
