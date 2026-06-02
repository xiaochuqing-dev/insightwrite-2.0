<template>
  <main class="auth-layout">
    <canvas ref="authCanvas" id="auth-canvas" aria-hidden="true"></canvas>
    <section class="auth-card" aria-label="Authentication">
      <div class="auth-panel">
        <RouterLink class="auth-brand" to="/home" aria-label="InsightWrite home">
          <span class="auth-brand__mark" aria-hidden="true">IW</span>
          <span>
            <strong>InsightWrite</strong>
            <small>英语写作与精读分析</small>
          </span>
        </RouterLink>

        <div class="auth-content">
          <router-view />
        </div>
      </div>

      <aside class="auth-art" aria-label="InsightWrite 功能简介">
        <div class="feature-board">
          <span class="feature-board__eyebrow">InsightWrite</span>
          <strong>把英语文本变成可学习的表达地图。</strong>
          <div class="feature-list">
            <span>写作诊断</span>
            <span>高阶润色</span>
            <span>佳作精读</span>
            <span>表达积累</span>
          </div>
        </div>
        <div class="auth-art__copy">
          <span>五色标注 · 写作诊断 · 佳作拆解</span>
          <strong>既能看清自己的文章如何变好，也能拆解好文章为什么写得好。</strong>
        </div>
      </aside>
    </section>
  </main>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'

const authCanvas = ref(null)

// Canvas 状态
let ctx = null
let w = 0, h = 0, dpr = 1
let animationId = null
let fireflyTimer = null, sparkleTimer = null, starTimer = null

// 夜景粒子系统（冷色调，增强版）
let stars = []          // 星空粒子（80个）
let brightStars = []    // 亮星大粒（14个）
let sparkles = []       // 星芒闪烁
let fireflies = []      // 草地流萤（最多5个）
let shootingStars = []  // 流星（高频）

const nightChars = ['文', '思', '笔', '墨', '写', '读', '星', '光', '夜']

function resize() {
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  w = window.innerWidth
  h = window.innerHeight
  const canvas = authCanvas.value
  if (!canvas) return
  canvas.width = w * dpr
  canvas.height = h * dpr
  canvas.style.width = w + 'px'
  canvas.style.height = h + 'px'
  ctx.setTransform(1, 0, 0, 1, 0, 0)
  ctx.scale(dpr, dpr)
}

// ── 星空粒子：80 个，更密更亮，70% 偏向上半屏 ──
function initStars() {
  stars = []
  for (let i = 0; i < 80; i++) {
    const inSky = Math.random() < 0.7
    stars.push({
      x: Math.random() * 2200,
      y: inSky ? Math.random() * h * 0.65 : Math.random() * 1200,
      r: 0.8 + Math.random() * 2.2,
      vx: (Math.random() - 0.5) * 0.12,
      vy: -0.06 - Math.random() * 0.14,
      ba: 0.18 + Math.random() * 0.26,
      ph: Math.random() * Math.PI * 2
    })
  }
}

// ── 亮星：14 个，更大光晕 ──
function initBrightStars() {
  brightStars = []
  for (let i = 0; i < 14; i++) {
    brightStars.push({
      x: Math.random() * w,
      y: Math.random() * h * 0.55,
      r: 2.0 + Math.random() * 3.6,
      vx: (Math.random() - 0.5) * 0.06,
      vy: -0.03 - Math.random() * 0.08,
      ba: 0.26 + Math.random() * 0.24,
      ph: Math.random() * Math.PI * 2
    })
  }
}

// ── 草地流萤：最多 5 个同时存在，冷绿光 ──
function spawnFirefly() {
  if (fireflies.length >= 5) return
  fireflies.push({
    x: 60 + Math.random() * (w - 120),
    y: h * 0.42 + Math.random() * h * 0.48,
    vy: -0.18 - Math.random() * 0.30,
    life: 1,
    size: 14 + Math.random() * 10,  // 14~24px
    char: nightChars[Math.floor(Math.random() * nightChars.length)]
  })
}

// ── 星芒闪烁：更频繁，每次 2~3 粒 ──
function spawnSparkle() {
  sparkles.push({
    x: Math.random() * w,
    y: Math.random() * h * 0.78,
    life: 1,
    decay: 0.013 + Math.random() * 0.025,
    r: 0.7 + Math.random() * 2.2
  })
}

// ── 流星：40% 概率，间隔更短 ──
function spawnShootingStar() {
  const fromLeft = Math.random() < 0.5
  shootingStars.push({
    x: fromLeft ? -10 : w + 10,
    y: Math.random() * h * 0.34,
    vx: fromLeft ? 2.0 + Math.random() * 2.6 : -(2.0 + Math.random() * 2.6),
    vy: 0.6 + Math.random() * 1.4,
    life: 1,
    decay: 0.024 + Math.random() * 0.040,
    len: 40 + Math.random() * 60
  })
}

function animate() {
  animationId = requestAnimationFrame(animate)
  if (!ctx) return
  ctx.clearRect(0, 0, w, h)

  // ── 星空粒子 ──
  for (let i = 0; i < stars.length; i++) {
    const p = stars[i]
    p.x += p.vx
    p.y += p.vy
    if (p.x < -20) p.x = w + 20
    if (p.x > w + 20) p.x = -20
    if (p.y < -20) p.y = h + 20
    if (p.y > h + 20) p.y = -20
    const alpha = p.ba + Math.sin(Date.now() * 0.0013 + p.ph) * 0.12
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(215,230,248,' + Math.max(0.06, alpha) + ')'
    ctx.shadowColor = 'rgba(180,205,235,0.42)'
    ctx.shadowBlur = 7
    ctx.fill()
    ctx.shadowBlur = 0
  }

  // ── 亮星：大光晕 ──
  for (let i = 0; i < brightStars.length; i++) {
    const p = brightStars[i]
    p.x += p.vx
    p.y += p.vy
    if (p.x < -20) p.x = w + 20
    if (p.x > w + 20) p.x = -20
    if (p.y < -20) p.y = h + 20
    if (p.y > h + 20) p.y = -20
    const alpha = p.ba + Math.sin(Date.now() * 0.0008 + p.ph) * 0.12
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(228,242,255,' + Math.max(0.10, alpha) + ')'
    ctx.shadowColor = 'rgba(195,215,245,0.55)'
    ctx.shadowBlur = 16
    ctx.fill()
    ctx.shadowBlur = 0
  }

  // ── 星芒闪烁 ──
  for (let s = sparkles.length - 1; s >= 0; s--) {
    const sp = sparkles[s]
    sp.life -= sp.decay
    if (sp.life <= 0) { sparkles.splice(s, 1); continue }
    const alpha = sp.life * 0.55
    const radius = sp.r + (1 - sp.life) * 3.0
    ctx.beginPath()
    ctx.arc(sp.x, sp.y, radius, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(242,247,255,' + alpha + ')'
    ctx.shadowColor = 'rgba(215,230,248,' + (alpha * 0.85) + ')'
    ctx.shadowBlur = 8
    ctx.fill()
    ctx.shadowBlur = 0
  }

  // ── 草地流萤：更大光晕 ──
  for (let j = fireflies.length - 1; j >= 0; j--) {
    const f = fireflies[j]
    f.y += f.vy
    f.life -= 0.001
    if (f.life <= 0) { fireflies.splice(j, 1); continue }
    // 径向光晕（扩大到 20px）
    const glow = ctx.createRadialGradient(f.x, f.y, 0, f.x, f.y, 20)
    glow.addColorStop(0, 'rgba(180,220,178,' + (f.life * 0.28) + ')')
    glow.addColorStop(0.5, 'rgba(175,215,172,' + (f.life * 0.10) + ')')
    glow.addColorStop(1, 'rgba(170,210,168,0)')
    ctx.fillStyle = glow
    ctx.fillRect(f.x - 20, f.y - 20, 40, 40)
    // 文字
    ctx.font = f.size + 'px serif'
    ctx.fillStyle = 'rgba(185,222,180,' + (f.life * 0.76) + ')'
    ctx.shadowColor = 'rgba(165,205,168,0.64)'
    ctx.shadowBlur = 12
    ctx.fillText(f.char, f.x, f.y)
    ctx.shadowBlur = 0
  }

  // ── 流星 ──
  for (let ss = shootingStars.length - 1; ss >= 0; ss--) {
    const s = shootingStars[ss]
    s.x += s.vx
    s.y += s.vy
    s.life -= s.decay
    if (s.life <= 0 || s.x < -60 || s.x > w + 60) { shootingStars.splice(ss, 1); continue }
    const trailAlpha = s.life * 0.44
    ctx.strokeStyle = 'rgba(215,235,252,' + trailAlpha + ')'
    ctx.lineWidth = 0.9
    ctx.beginPath()
    ctx.moveTo(s.x - s.vx * s.len / 25, s.y - s.vy * s.len / 25)
    ctx.lineTo(s.x, s.y)
    ctx.stroke()
    // 头部
    ctx.beginPath()
    ctx.arc(s.x, s.y, 1.5, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(245,250,255,' + (s.life * 0.72) + ')'
    ctx.shadowColor = 'rgba(225,238,252,0.70)'
    ctx.shadowBlur = 10
    ctx.fill()
    ctx.shadowBlur = 0
  }
}

onMounted(() => {
  const canvas = authCanvas.value
  if (!canvas) return
  ctx = canvas.getContext('2d')

  resize()
  initStars()
  initBrightStars()
  animate()

  window.addEventListener('resize', resize)

  // 流萤：每 2.5s，60% 概率，最多 5 只
  fireflyTimer = setInterval(() => {
    if (Math.random() < 0.60) spawnFirefly()
  }, 2500)
  spawnFirefly()
  spawnFirefly()
  spawnFirefly()

  // 星芒：每 1s 生成 2~3 粒
  sparkleTimer = setInterval(() => {
    spawnSparkle()
    spawnSparkle()
    if (Math.random() < 0.55) spawnSparkle()
  }, 1000)

  // 流星：每 4.5s，40% 概率
  starTimer = setInterval(() => {
    if (Math.random() < 0.40) spawnShootingStar()
  }, 4500)
  // 开局就来两颗
  spawnShootingStar()
  spawnShootingStar()
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
  if (fireflyTimer) clearInterval(fireflyTimer)
  if (sparkleTimer) clearInterval(sparkleTimer)
  if (starTimer) clearInterval(starTimer)
  window.removeEventListener('resize', resize)
})
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.auth-layout {
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: clamp(var(--space-4), 4vw, var(--space-10));
  background:
    linear-gradient(120deg, rgba(12, 18, 32, 0.06), rgba(255, 255, 255, 0.02)),
    url('@/assets/images/login.jpg');
  background-size: cover;
  background-position: center;
}

/* ── Canvas 夜景粒子层 ── */
#auth-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.auth-card {
  position: relative;
  z-index: 1;
  width: min(100%, 66rem);
  min-height: min(42rem, calc(100vh - 2rem));
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(22rem, 1.08fr);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.06);
  box-shadow: var(--shadow-lg);
  backdrop-filter: blur(24px) saturate(1.18);
}

.auth-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: clamp(var(--space-6), 4vw, var(--space-10));
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.24), rgba(226, 241, 233, 0.1)),
    rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(30px) saturate(1.24);
}

.auth-panel::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(ellipse at 22% 12%, rgba(255, 255, 255, 0.4), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.16), transparent 42%, rgba(3, 17, 26, 0.08));
}

.auth-brand {
  position: relative;
  z-index: 1;
  align-self: center;
  display: inline-grid;
  justify-items: center;
  align-items: center;
  gap: var(--space-3);
  width: min(100%, 20rem);
  padding-top: var(--space-2);
  color: var(--gray-900);
  text-align: center;
}

.auth-brand__mark {
  width: 5rem;
  height: 5rem;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 1.35rem;
  background: var(--gray-900);
  color: white;
  font-family: var(--font-serif);
  font-size: 1.36rem;
  font-weight: 700;
  box-shadow:
    0 18px 36px rgba(15, 23, 42, 0.24),
    0 0 34px rgba(32, 243, 232, 0.12);
}

.auth-brand strong,
.auth-brand small {
  display: block;
  line-height: 1.25;
}

.auth-brand strong {
  font-size: 1.42rem;
  letter-spacing: 0.01em;
}

.auth-brand small {
  margin-top: var(--space-1);
  color: rgba(51, 65, 85, 0.74);
  font-size: 1rem;
}

.auth-content {
  position: relative;
  z-index: 1;
  width: min(100%, 25rem);
  margin: clamp(var(--space-8), 6vh, var(--space-12)) 0 0;
  padding: 0 0 var(--space-5);
}

.auth-art {
  position: relative;
  display: grid;
  align-content: center;
  gap: var(--space-10);
  overflow: hidden;
  padding: clamp(var(--space-8), 6vw, var(--space-16));
  background:
    radial-gradient(ellipse at 28% 16%, rgba(107, 151, 255, 0.24), transparent 36%),
    linear-gradient(135deg, rgba(29, 78, 216, 0.38), rgba(15, 23, 42, 0.58));
  backdrop-filter: blur(16px) saturate(1.12);
  color: white;
}

.auth-art::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.08) 1px, transparent 1px);
  background-size: 2.5rem 2.5rem;
  mask-image: linear-gradient(135deg, black, transparent 78%);
}

.feature-board {
  position: relative;
  z-index: 1;
  display: grid;
  gap: var(--space-5);
  max-width: 28rem;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: var(--radius-lg);
  padding: clamp(var(--space-6), 4vw, var(--space-8));
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(22px) saturate(1.18);
}

.feature-board__eyebrow {
  width: max-content;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  padding: var(--space-1) var(--space-3);
  color: rgba(255, 255, 255, 0.78);
  font-size: var(--text-small);
  font-weight: 850;
  text-transform: uppercase;
}

.feature-board strong {
  color: white;
  font-family: var(--font-serif);
  font-size: clamp(2rem, 4vw, 3.4rem);
  line-height: var(--leading-tight);
}

.feature-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.feature-list span {
  border-radius: var(--radius-md);
  padding: var(--space-3);
  background: rgba(255, 255, 255, 0.13);
  color: rgba(255, 255, 255, 0.9);
  font-size: var(--text-caption);
  font-weight: 850;
}

.auth-art__copy {
  position: relative;
  z-index: 1;
  display: grid;
  gap: var(--space-3);
  max-width: 26rem;
}

.auth-art__copy span {
  color: rgba(255, 255, 255, 0.7);
  font-size: var(--text-caption);
  text-transform: uppercase;
}

.auth-art__copy strong {
  font-family: var(--font-serif);
  font-size: clamp(1.75rem, 3vw, 2.75rem);
  line-height: var(--leading-tight);
}

@media (max-width: 820px) {
  .auth-card {
    grid-template-columns: 1fr;
  }

  .auth-art {
    min-height: 20rem;
  }

  .auth-content {
    margin: var(--space-8) 0 0;
    padding-bottom: 0;
  }
}

@media (max-width: 520px) {
  .auth-layout {
    padding: 0;
  }

  .auth-card {
    min-height: 100vh;
    border: 0;
    border-radius: 0;
  }

  .auth-art {
    display: none;
  }
}
</style>
