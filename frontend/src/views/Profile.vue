<template>
  <section class="profile-view">
    <header class="profile-hero">
      <div class="profile-identity">
        <label class="avatar-uploader" :class="{ 'is-uploading': avatarUploading }">
          <input type="file" accept="image/png,image/jpeg,image/webp" @change="uploadProfileAvatar" />
          <img v-if="avatarUrl" :src="avatarUrl" alt="用户头像" />
          <span v-else>{{ avatarInitial }}</span>
          <em>{{ avatarUploading ? '上传中' : '更换头像' }}</em>
        </label>
        <div>
          <p class="eyebrow">Settings</p>
          <h1>个人设置</h1>
          <p class="hero-copy">管理账号档案、阅读舒适度、分析偏好和安全入口。</p>
        </div>
      </div>

      <div class="hero-stats" aria-label="账号概览">
        <div>
          <span>当前积分</span>
          <strong>{{ credits }}</strong>
        </div>
        <div>
          <span>注册时间</span>
          <strong>{{ createdAtText }}</strong>
        </div>
        <div>
          <span>上次登录</span>
          <strong>{{ lastLoginText }}</strong>
        </div>
      </div>
    </header>

    <div v-if="loadError" class="notice notice--error">{{ loadError }}</div>
    <div v-if="avatarError" class="notice notice--error">{{ avatarError }}</div>

    <div class="settings-grid">
      <section class="settings-section settings-section--wide">
        <div class="section-head">
          <div>
            <p class="section-kicker">Profile</p>
            <h2>个人概览</h2>
          </div>
          <span class="section-badge">{{ resetHint || '积分状态正常' }}</span>
        </div>

        <div class="profile-fields">
          <div class="form-row">
            <label for="nickname">昵称</label>
            <div class="input-group">
              <input id="nickname" v-model="nickname" type="text" maxlength="20" />
              <button type="button" :disabled="saving" @click="saveNickname">
                {{ saving ? '保存中' : '保存' }}
              </button>
            </div>
          </div>

          <div class="profile-line">
            <span>邮箱</span>
            <strong>{{ email || '未获取' }}</strong>
          </div>

          <p v-if="saveMessage" class="form-message form-message--ok">{{ saveMessage }}</p>
          <p v-if="saveError" class="form-message form-message--error">{{ saveError }}</p>
        </div>
      </section>

      <section class="settings-section">
        <div class="section-head section-head--compact">
          <div>
            <p class="section-kicker">Reading</p>
            <h2>字体大小</h2>
          </div>
        </div>

        <div class="option-grid option-grid--font" role="radiogroup" aria-label="字体大小">
          <button
            v-for="option in fontSizeOptions"
            :key="option.id"
            type="button"
            role="radio"
            :aria-checked="fontSizeId === option.id"
            :class="{ active: fontSizeId === option.id }"
            @click="setFontSize(option.id)"
          >
            <strong>{{ option.label }}</strong>
            <small>{{ option.value }}</small>
          </button>
        </div>
      </section>

      <RouterLink to="/profile/security" class="settings-section settings-link">
        <div>
          <p class="section-kicker">Security</p>
          <h2>账号安全</h2>
          <p>修改密码、查看积分流水、退出登录，后续安全功能也会放在这里。</p>
        </div>
        <span aria-hidden="true">→</span>
      </RouterLink>

      <section class="settings-section settings-section--wide">
        <div class="section-head">
          <div>
            <p class="section-kicker">Analysis</p>
            <h2>分析偏好</h2>
          </div>
          <span class="section-badge">不改变三种分析模式的输出格式</span>
        </div>

        <div class="preference-block">
          <div class="preference-row">
            <span>默认模式</span>
            <div class="option-grid" role="radiogroup" aria-label="默认分析模式">
              <button
                v-for="option in analysisOptions.defaultMode"
                :key="option.id"
                type="button"
                :class="{ active: analysisPreferences.defaultMode === option.id }"
                @click="setAnalysisPreference('defaultMode', option.id)"
              >
                <strong>{{ option.label }}</strong>
                <small>{{ option.description }}</small>
              </button>
            </div>
          </div>

          <div class="preference-row">
            <span>输出详略</span>
            <div class="option-grid" role="radiogroup" aria-label="输出详略">
              <button
                v-for="option in analysisOptions.detailLevel"
                :key="option.id"
                type="button"
                :class="{ active: analysisPreferences.detailLevel === option.id }"
                @click="setAnalysisPreference('detailLevel', option.id)"
              >
                <strong>{{ option.label }}</strong>
                <small>{{ option.description }}</small>
              </button>
            </div>
          </div>

          <div class="preference-row">
            <span>关注重点</span>
            <div class="option-grid option-grid--four" role="radiogroup" aria-label="关注重点">
              <button
                v-for="option in analysisOptions.focus"
                :key="option.id"
                type="button"
                :class="{ active: analysisPreferences.focus === option.id }"
                @click="setAnalysisPreference('focus', option.id)"
              >
                <strong>{{ option.label }}</strong>
                <small>{{ option.description }}</small>
              </button>
            </div>
          </div>

          <label class="switch-row">
            <span>
              <strong>默认生成参考范文</strong>
              <small>仅用于写作诊断和高阶润色，佳作精读不会生成范文。</small>
            </span>
            <input
              type="checkbox"
              :checked="analysisPreferences.generateEssay"
              @change="setAnalysisPreference('generateEssay', $event.target.checked)"
            />
          </label>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { getCredits } from '@/api/credits.js'
import { uploadAvatar } from '@/api/auth.js'
import http from '@/api/index.js'
import {
  ANALYSIS_PREFERENCE_OPTIONS,
  FONT_SIZE_OPTIONS,
  applyFontSizePreference,
  getStoredAnalysisPreferences,
  getStoredFontSizeId,
  saveAnalysisPreferences,
  saveFontSizePreference
} from '@/utils/preferences.js'
import { formatDate } from '@/utils/display.js'

const credits = ref(0)
const dailyResetAt = ref('')
const email = ref('')
const nickname = ref('')
const avatar = ref('')
const createdAt = ref('')
const lastLoginAt = ref('')
const saving = ref(false)
const avatarUploading = ref(false)
const saveMessage = ref('')
const saveError = ref('')
const loadError = ref('')
const avatarError = ref('')
const fontSizeId = ref(getStoredFontSizeId())
const fontSizeOptions = FONT_SIZE_OPTIONS
const analysisOptions = ANALYSIS_PREFERENCE_OPTIONS
const analysisPreferences = ref(getStoredAnalysisPreferences())

const avatarInitial = computed(() => {
  const source = nickname.value || email.value || 'I'
  return source.trim().slice(0, 1).toUpperCase()
})

const avatarUrl = computed(() => {
  if (!avatar.value) return ''
  return avatar.value
})

const resetHint = computed(() => {
  if (!dailyResetAt.value) return ''
  const d = new Date(dailyResetAt.value)
  return Number.isNaN(d.getTime()) ? '' : `下次重置：${d.toLocaleString()}`
})

const createdAtText = computed(() => formatDate(createdAt.value) || '暂无记录')
const lastLoginText = computed(() => formatDate(lastLoginAt.value) || '暂无记录')

function broadcastUserUpdate() {
  window.dispatchEvent(new CustomEvent('insightwrite:user-updated'))
}

function setFontSize(id) {
  fontSizeId.value = saveFontSizePreference(id).id
}

function setAnalysisPreference(key, value) {
  analysisPreferences.value = saveAnalysisPreferences({
    ...analysisPreferences.value,
    [key]: value
  })
}

async function saveNickname() {
  saving.value = true
  saveMessage.value = ''
  saveError.value = ''
  try {
    await http.put('/auth/me', { username: nickname.value })
    saveMessage.value = '保存成功'
    broadcastUserUpdate()
  } catch (e) {
    saveError.value = e?.response?.data?.error || '保存失败'
  } finally {
    saving.value = false
  }
}

async function uploadProfileAvatar(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  avatarError.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    avatarError.value = '仅支持 JPG、PNG 或 WebP 图片'
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    avatarError.value = '头像图片不能超过 2MB'
    return
  }

  avatarUploading.value = true
  try {
    const response = await uploadAvatar(file)
    avatar.value = response.data.avatar || ''
    broadcastUserUpdate()
  } catch (e) {
    avatarError.value = e?.response?.data?.error || '头像上传失败'
  } finally {
    avatarUploading.value = false
  }
}

onMounted(async () => {
  applyFontSizePreference(fontSizeId.value)
  try {
    const [credRes, meRes] = await Promise.all([getCredits(), http.get('/auth/me')])
    credits.value = credRes.data.credits || 0
    dailyResetAt.value = credRes.data.daily_reset_at || ''
    email.value = meRes.data.email || ''
    nickname.value = meRes.data.username || ''
    avatar.value = meRes.data.avatar || ''
    createdAt.value = meRes.data.created_at || ''
    lastLoginAt.value = meRes.data.last_login_at || ''
  } catch (e) {
    loadError.value = e?.response?.data?.error || '设置页信息加载失败'
  }
})
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.profile-view {
  display: grid;
  gap: var(--space-6);
}

.profile-hero,
.settings-section {
  border: 1px solid rgba(151, 210, 166, 0.2);
  border-radius: var(--radius-lg);
  background: rgba(7, 25, 18, 0.56);
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(24px) saturate(1.14);
}

.profile-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(24rem, 0.8fr);
  gap: var(--space-6);
  align-items: stretch;
  padding: clamp(var(--space-5), 3vw, var(--space-8));
  background:
    linear-gradient(135deg, rgba(8, 28, 21, 0.78), rgba(7, 25, 18, 0.48)),
    rgba(7, 25, 18, 0.58);
  box-shadow: var(--shadow-md);
}

.profile-identity {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  min-width: 0;
}

.avatar-uploader {
  position: relative;
  width: clamp(4.8rem, 8vw, 6.6rem);
  aspect-ratio: 1;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid rgba(32, 243, 232, 0.3);
  border-radius: var(--radius-lg);
  background: rgba(5, 15, 12, 0.78);
  color: #f4fff9;
  cursor: pointer;
}

.avatar-uploader input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.avatar-uploader img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-uploader > span {
  font-family: var(--font-serif);
  font-size: clamp(2rem, 4vw, 3.5rem);
  font-weight: 900;
}

.avatar-uploader em {
  position: absolute;
  inset-inline: 0;
  bottom: 0;
  padding: var(--space-1);
  background: rgba(2, 10, 7, 0.78);
  color: #e9ffcf;
  font-size: var(--text-small);
  font-style: normal;
  font-weight: 850;
  text-align: center;
}

.avatar-uploader.is-uploading {
  opacity: 0.72;
  pointer-events: none;
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
  margin-top: var(--space-1);
  font-size: clamp(2rem, 4vw, 3.8rem);
}

h2 {
  font-size: clamp(1.35rem, 2vw, 1.85rem);
}

.hero-copy,
.settings-link p,
.option-grid small,
.switch-row small {
  color: var(--color-text-muted);
  font-size: var(--text-small);
  font-weight: 720;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3);
}

.hero-stats div {
  display: grid;
  align-content: center;
  gap: var(--space-2);
  border: 1px solid rgba(151, 210, 166, 0.2);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  background: rgba(7, 25, 18, 0.54);
}

.hero-stats span,
.section-badge,
.profile-line span,
.preference-row > span {
  color: var(--color-text-subtle);
  font-size: var(--text-small);
  font-weight: 800;
}

.hero-stats strong {
  color: var(--color-text);
  font-size: var(--text-caption);
  font-weight: 900;
  overflow-wrap: anywhere;
}

.hero-stats div:first-child strong {
  color: #f2c94c;
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 1;
}

.notice {
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  font-size: var(--text-caption);
  font-weight: 800;
}

.notice--error {
  border: 1px solid rgba(239, 68, 68, 0.28);
  background: rgba(88, 25, 29, 0.48);
  color: #ffb4b4;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-5);
}

.settings-section {
  padding: clamp(var(--space-5), 3vw, var(--space-6));
}

.settings-section--wide {
  grid-column: 1 / -1;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.section-head--compact {
  margin-bottom: var(--space-4);
}

.section-badge {
  max-width: 20rem;
  border: 1px solid rgba(242, 201, 76, 0.28);
  border-radius: 999px;
  padding: var(--space-2) var(--space-3);
  background: rgba(242, 201, 76, 0.1);
  color: #f2c94c;
  overflow-wrap: anywhere;
}

.profile-fields,
.preference-block {
  display: grid;
  gap: var(--space-4);
}

.form-row {
  display: grid;
  grid-template-columns: 4rem minmax(0, 1fr);
  gap: var(--space-3);
  align-items: center;
}

.form-row label {
  color: var(--color-text-muted);
  font-size: var(--text-caption);
  font-weight: 800;
}

.input-group {
  display: flex;
  gap: var(--space-2);
  min-width: 0;
}

.input-group input {
  min-width: 0;
  height: 2.75rem;
  flex: 1;
  border: 1px solid rgba(151, 210, 166, 0.26);
  border-radius: var(--radius-md);
  padding: 0 var(--space-3);
  background: rgba(4, 16, 11, 0.48);
  color: var(--color-text);
  outline: none;
}

.input-group input:focus {
  border-color: rgba(32, 243, 232, 0.48);
  box-shadow: 0 0 0 3px rgba(32, 243, 232, 0.12);
}

.input-group button {
  height: 2.75rem;
  border-radius: var(--radius-md);
  padding: 0 var(--space-4);
  background: rgba(32, 243, 232, 0.16);
  color: #9ffcf6;
  font-size: var(--text-caption);
  font-weight: 900;
  white-space: nowrap;
}

.profile-line,
.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  border-top: 1px solid rgba(151, 210, 166, 0.14);
  padding-top: var(--space-4);
}

.profile-line strong,
.switch-row strong {
  color: var(--color-text);
  font-size: var(--text-caption);
  font-weight: 900;
}

.form-message {
  font-size: var(--text-small);
  font-weight: 850;
}

.form-message--ok {
  color: #9fffc6;
}

.form-message--error {
  color: #ffb4b4;
}

.option-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-2);
}

.option-grid--font,
.option-grid--four {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.option-grid button {
  min-height: 4.35rem;
  display: grid;
  align-content: center;
  gap: var(--space-1);
  border: 1px solid rgba(151, 210, 166, 0.22);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  background: rgba(4, 16, 11, 0.38);
  color: var(--color-text-muted);
  text-align: left;
}

.option-grid button.active {
  border-color: rgba(32, 243, 232, 0.52);
  background: rgba(32, 243, 232, 0.14);
  color: #e9ffcf;
  box-shadow: inset 0 1px rgba(255, 255, 255, 0.08);
}

.option-grid strong {
  color: var(--color-text);
  font-size: var(--text-caption);
  font-weight: 900;
}

.preference-row {
  display: grid;
  gap: var(--space-2);
}

.switch-row {
  cursor: pointer;
}

.switch-row > span {
  display: grid;
  gap: 2px;
}

.switch-row input {
  width: 1.15rem;
  height: 1.15rem;
  accent-color: var(--color-primary);
}

.settings-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-5);
  color: inherit;
}

.settings-link:hover {
  border-color: rgba(32, 243, 232, 0.36);
  background: rgba(14, 45, 31, 0.64);
}

.settings-link > span {
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

@media (max-width: 980px) {
  .profile-hero,
  .settings-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .profile-identity,
  .profile-line,
  .switch-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-stats,
  .option-grid,
  .option-grid--font,
  .option-grid--four {
    grid-template-columns: 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .input-group {
    flex-direction: column;
  }
}
</style>
