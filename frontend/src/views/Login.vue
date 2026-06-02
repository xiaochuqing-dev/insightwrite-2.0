<template>
  <form class="auth-form" novalidate @submit.prevent="handleSubmit">
    <div class="auth-heading">
      <p>欢迎回来</p>
      <h1>继续你的写作诊断与佳作精读。</h1>
    </div>

    <p v-if="serverError" class="form-alert form-alert--error">{{ serverError }}</p>

    <label class="field">
      <span>邮箱</span>
      <input
        v-model.trim="form.email"
        type="email"
        autocomplete="email"
        placeholder="请输入邮箱"
        :aria-invalid="Boolean(errors.email)"
        @blur="validateField('email')"
      />
      <small v-if="errors.email">{{ errors.email }}</small>
    </label>

    <label class="field">
      <span>密码</span>
      <div class="password-field">
        <input
          v-model="form.password"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="current-password"
          placeholder="请输入密码"
          :aria-invalid="Boolean(errors.password)"
          @blur="validateField('password')"
        />
        <button type="button" @click="showPassword = !showPassword">
          {{ showPassword ? '隐藏' : '显示' }}
        </button>
      </div>
      <small v-if="errors.password">{{ errors.password }}</small>
    </label>

    <button class="primary-action" type="submit" :disabled="isSubmitting">
      {{ isSubmitting ? '登录中...' : '登录' }}
    </button>

    <div class="form-links">
      <RouterLink to="/forgot-password">忘记密码？</RouterLink>
      <span>
        没有账号？
        <RouterLink to="/register">注册</RouterLink>
      </span>
    </div>
  </form>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { login } from '@/api/auth.js'

const router = useRouter()
const form = reactive({
  email: '',
  password: ''
})
const errors = reactive({})
const serverError = ref('')
const isSubmitting = ref(false)
const showPassword = ref(false)

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validateField(field) {
  errors[field] = ''

  if (field === 'email') {
    if (!form.email) errors.email = '请输入邮箱'
    else if (!emailPattern.test(form.email)) errors.email = '邮箱格式不正确'
  }

  if (field === 'password' && !form.password) {
    errors.password = '请输入密码'
  }
}

function validateForm() {
  validateField('email')
  validateField('password')
  return !errors.email && !errors.password
}

function getErrorMessage(error) {
  return error?.response?.data?.error || '登录失败，请检查邮箱和密码后重试'
}

async function handleSubmit() {
  serverError.value = ''
  if (!validateForm()) return

  isSubmitting.value = true
  try {
    const response = await login(form.email, form.password)
    const token = response.data?.token
    if (!token) throw new Error('Missing token')
    localStorage.setItem('token', token)
    await router.push('/home')
  } catch (error) {
    serverError.value = getErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}
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
  color: var(--gray-900);
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
  border-color: rgba(255, 255, 255, 0.48);
  background: rgba(248, 252, 255, 0.82);
  color: var(--gray-900);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
  transition:
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}

.field input::placeholder {
  color: rgba(71, 85, 105, 0.56);
}

.field input:focus {
  border-color: var(--color-primary);
  background: rgba(255, 255, 255, 0.92);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.36),
    0 0 0 4px rgba(32, 243, 232, 0.16);
}

.field input[aria-invalid="true"] {
  border-color: var(--color-danger);
}

.field small {
  color: var(--color-danger);
  font-size: var(--text-small);
}

.password-field {
  position: relative;
}

.password-field input {
  padding-right: 4.75rem;
}

.password-field button {
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

.password-field button:hover {
  background: var(--color-primary-50);
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

.primary-action {
  height: 3.125rem;
  border-radius: var(--radius-md);
  background: var(--gray-900);
  color: white;
  font-weight: 800;
  box-shadow: var(--shadow-md);
  transition:
    transform var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out);
}

.primary-action:hover:not(:disabled) {
  transform: translateY(-1px);
  background: var(--color-primary-700);
}

.form-links {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  color: var(--color-text-muted);
  font-size: var(--text-caption);
}
</style>
