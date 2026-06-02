<template>
  <form class="auth-form" novalidate @submit.prevent="handleSubmit">
    <div class="auth-heading">
      <p>找回密码</p>
      <h1>{{ step === 1 ? '验证你的邮箱。' : '设置新密码。' }}</h1>
    </div>

    <div class="steps" aria-label="找回密码进度">
      <span :class="{ active: step === 1 }">1</span>
      <i></i>
      <span :class="{ active: step === 2 }">2</span>
    </div>

    <p v-if="serverError" class="form-alert form-alert--error">{{ serverError }}</p>
    <p v-if="successMessage" class="form-alert form-alert--success">{{ successMessage }}</p>

    <template v-if="step === 1">
      <label class="field">
        <span>邮箱</span>
        <input v-model.trim="form.email" type="email" autocomplete="email" @blur="validateField('email')" />
        <small v-if="errors.email">{{ errors.email }}</small>
      </label>

      <label class="field">
        <span>验证码</span>
        <div class="code-field">
          <input v-model.trim="form.code" inputmode="numeric" maxlength="6" @blur="validateField('code')" />
          <button type="button" :disabled="isSendingCode || countdown > 0" @click="handleSendCode">
            {{ countdown > 0 ? `${countdown}秒后重发` : isSendingCode ? '发送中...' : '发送验证码' }}
          </button>
        </div>
        <small v-if="errors.code">{{ errors.code }}</small>
      </label>

      <button class="primary-action" type="submit">下一步</button>
    </template>

    <template v-else>
      <label class="field">
        <span>新密码</span>
        <div class="password-field">
          <input
            v-model="form.newPassword"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="new-password"
            @blur="validateField('newPassword')"
          />
          <button type="button" @click="showPassword = !showPassword">
            {{ showPassword ? '隐藏' : '显示' }}
          </button>
        </div>
        <small v-if="errors.newPassword">{{ errors.newPassword }}</small>
      </label>

      <label class="field">
        <span>确认新密码</span>
        <input
          v-model="form.confirmPassword"
          type="password"
          autocomplete="new-password"
          @blur="validateField('confirmPassword')"
        />
        <small v-if="errors.confirmPassword">{{ errors.confirmPassword }}</small>
      </label>

      <div class="action-row">
        <button class="secondary-action" type="button" @click="step = 1">返回上一步</button>
        <button class="primary-action" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '重置中...' : '重置密码' }}
        </button>
      </div>
    </template>

    <RouterLink class="back-link" to="/login">返回登录</RouterLink>
  </form>
</template>

<script setup>
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { forgotPassword, sendCode } from '@/api/auth.js'

const router = useRouter()
const step = ref(1)
const form = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})
const errors = reactive({})
const serverError = ref('')
const successMessage = ref('')
const isSendingCode = ref(false)
const isSubmitting = ref(false)
const showPassword = ref(false)
const countdown = ref(0)
let timerId = null
let redirectTimerId = null

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const codePattern = /^\d{6}$/

function validateField(field) {
  errors[field] = ''

  if (field === 'email') {
    if (!form.email) errors.email = '请输入邮箱。'
    else if (!emailPattern.test(form.email)) errors.email = '请输入有效的邮箱地址。'
  }

  if (field === 'code') {
    if (!form.code) errors.code = '请输入验证码。'
    else if (!codePattern.test(form.code)) errors.code = '验证码需为 6 位数字。'
  }

  if (field === 'newPassword') {
    if (!form.newPassword) errors.newPassword = '请输入新密码。'
    else if (form.newPassword.length < 6) errors.newPassword = '密码至少需要 6 个字符。'
    if (form.confirmPassword) validateField('confirmPassword')
  }

  if (field === 'confirmPassword') {
    if (!form.confirmPassword) errors.confirmPassword = '请再次输入新密码。'
    else if (form.confirmPassword !== form.newPassword) errors.confirmPassword = '两次输入的密码不一致。'
  }
}

function validateStepOne() {
  ;['email', 'code'].forEach(validateField)
  return !errors.email && !errors.code
}

function validateStepTwo() {
  ;['newPassword', 'confirmPassword'].forEach(validateField)
  return !errors.newPassword && !errors.confirmPassword
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
    await sendCode(form.email, 'forgot_password')
    startCountdown()
  } catch (error) {
    serverError.value = getErrorMessage(error, '验证码发送失败，请稍后再试。')
  } finally {
    isSendingCode.value = false
  }
}

async function handleSubmit() {
  serverError.value = ''
  successMessage.value = ''

  if (step.value === 1) {
    if (validateStepOne()) step.value = 2
    return
  }

  if (!validateStepTwo()) return

  isSubmitting.value = true
  try {
    await forgotPassword(form.email, form.code, form.newPassword)
    successMessage.value = '密码重置成功，正在返回登录页...'
    redirectTimerId = setTimeout(() => router.push('/login'), 2000)
  } catch (error) {
    serverError.value = getErrorMessage(error, '密码重置失败，请检查验证码后重试。')
  } finally {
    isSubmitting.value = false
  }
}

onBeforeUnmount(() => {
  clearInterval(timerId)
  clearTimeout(redirectTimerId)
})
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.auth-form {
  display: grid;
  gap: var(--space-5);
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

.steps {
  display: grid;
  grid-template-columns: 2rem 1fr 2rem;
  align-items: center;
  gap: var(--space-3);
}

.steps span {
  height: 2rem;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--gray-100);
  color: var(--gray-500);
  font-weight: 800;
}

.steps span.active {
  background: var(--gray-900);
  color: white;
}

.steps i {
  height: 2px;
  background: var(--gray-200);
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
  font-size: var(--text-caption);
}

.form-alert--error {
  background: var(--color-danger-50);
  color: var(--color-danger);
}

.form-alert--success {
  background: var(--color-success-50);
  color: var(--color-success);
}

.primary-action,
.secondary-action {
  height: 3.125rem;
  border-radius: var(--radius-md);
  padding: 0 var(--space-5);
  font-weight: 800;
}

.primary-action {
  background: var(--gray-900);
  color: white;
  box-shadow: var(--shadow-md);
}

.primary-action:hover:not(:disabled) {
  background: var(--color-primary-700);
}

.secondary-action {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--gray-700);
}

.action-row {
  display: grid;
  grid-template-columns: 0.7fr 1fr;
  gap: var(--space-3);
}

</style>
