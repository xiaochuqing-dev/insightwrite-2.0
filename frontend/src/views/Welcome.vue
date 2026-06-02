<template>
  <main class="welcome-view">
    <div class="valley-haze" aria-hidden="true"></div>
    <div class="light-dust" aria-hidden="true"></div>
    <div class="horizon-line" aria-hidden="true"></div>
    <canvas ref="welcomeCanvas" id="welcome-canvas" aria-hidden="true"></canvas>
    <div class="deco-line deco-line-left" aria-hidden="true"></div>
    <div class="deco-line deco-line-right" aria-hidden="true"></div>
    <aside class="side-rail" aria-hidden="true">ENGLISH LEARNING WORKSPACE</aside>
    <section class="welcome-stage" aria-label="InsightWrite welcome">
      <span class="brand-mark">IW</span>
      <h1>InsightWrite</h1>
      <p class="tagline">Write Better. Read Deeper. Think Clearer.</p>
      <p class="subtitle">An English writing workspace for diagnosis, revision, and close reading.</p>
      <div class="capability-list" aria-label="Core capabilities">
        <span>写作诊断</span>
        <span>高阶润色</span>
        <span>佳作精读</span>
      </div>
      <button type="button" @click="enterHome">开启体验之旅</button>
      <p class="release-note">Personal English learning desk · v2.0</p>
    </section>
  </main>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const welcomeCanvas = ref(null)

// Canvas 动画状态
let ctx = null
let w = 0, h = 0, dpr = 1
let animationId = null
let fireflyTimer = null, birdTimer = null, sparkleTimer = null, starTimer = null

// 六层粒子系统（增强版）
let particles = []      // 暖金光点（60个）
let floaters = []       // 大粒缓浮光点（12个）
let sparkles = []       // 闪烁微光
let fireflies = []      // 文字流萤（最多5个）
let birds = []          // 远处飞鸟（2~4只一群）
let shootingStars = []  // 掠空流光

const fireflyChars = ['文', '思', '笔', '墨', '写', '读', 'W', 'R', 'E']

function resize() {
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  w = window.innerWidth
  h = window.innerHeight
  const canvas = welcomeCanvas.value
  if (!canvas) return
  canvas.width = w * dpr
  canvas.height = h * dpr
  canvas.style.width = w + 'px'
  canvas.style.height = h + 'px'
  ctx.setTransform(1, 0, 0, 1, 0, 0)
  ctx.scale(dpr, dpr)
}

// ── 暖金光点：60 个，更密更大 ──
function initParticles() {
  particles = []
  for (let i = 0; i < 60; i++) {
    particles.push({
      x: Math.random() * 2200,
      y: Math.random() * 1200,
      r: 1.4 + Math.random() * 3.0,
      vx: (Math.random() - 0.5) * 0.16,
      vy: -0.09 - Math.random() * 0.20,
      ba: 0.18 + Math.random() * 0.24,
      ph: Math.random() * Math.PI * 2
    })
  }
}

// ── 大粒浮游光点：12 个 ──
function initFloaters() {
  floaters = []
  for (let i = 0; i < 12; i++) {
    floaters.push({
      x: Math.random() * w,
      y: Math.random() * h,
      r: 3.0 + Math.random() * 4.0,
      vx: (Math.random() - 0.5) * 0.08,
      vy: -0.05 - Math.random() * 0.10,
      ba: 0.28 + Math.random() * 0.22,
      ph: Math.random() * Math.PI * 2
    })
  }
}

// ── 文字流萤：最多 5 个，字号 14~24px ──
function spawnFirefly() {
  if (fireflies.length >= 5) return
  fireflies.push({
    x: 80 + Math.random() * (w - 160),
    y: h * 0.40 + Math.random() * h * 0.50,
    vy: -0.20 - Math.random() * 0.32,
    life: 1,
    size: 14 + Math.random() * 10,
    char: fireflyChars[Math.floor(Math.random() * fireflyChars.length)]
  })
}

// ── 闪烁微光：每次 2~3 粒 ──
function spawnSparkle() {
  sparkles.push({
    x: Math.random() * w,
    y: Math.random() * h * 0.84,
    life: 1,
    decay: 0.015 + Math.random() * 0.026,
    r: 0.9 + Math.random() * 2.6
  })
}

// ── 飞鸟 ──
function spawnBirds() {
  const count = 2 + Math.floor(Math.random() * 2)
  const baseY = h * 0.10 + Math.random() * h * 0.14
  const speed = 0.14 + Math.random() * 0.24
  const wingSpeed = 0.09 + Math.random() * 0.07
  for (let i = 0; i < count; i++) {
    birds.push({
      x: -30 - i * 58,
      y: baseY + i * 14,
      speed: speed,
      size: 3.0 + Math.random() * 3.8,
      wing: Math.random() * Math.PI * 2,
      wingSpeed: wingSpeed,
      bobAmp: 0.7 + Math.random() * 1.4,
      bobPh: Math.random() * Math.PI * 2
    })
  }
}

// ── 掠空流光：更频繁 ──
function spawnShootingStar() {
  const fromLeft = Math.random() < 0.5
  shootingStars.push({
    x: fromLeft ? -10 : w + 10,
    y: Math.random() * h * 0.36,
    vx: fromLeft ? 1.8 + Math.random() * 2.4 : -(1.8 + Math.random() * 2.4),
    vy: 0.8 + Math.random() * 1.5,
    life: 1,
    decay: 0.026 + Math.random() * 0.042,
    len: 35 + Math.random() * 55
  })
}

function animate() {
  animationId = requestAnimationFrame(animate)
  if (!ctx) return
  ctx.clearRect(0, 0, w, h)

  // ── 暖金光点 ──
  for (let i = 0; i < particles.length; i++) {
    const p = particles[i]
    p.x += p.vx
    p.y += p.vy
    if (p.x < -20) p.x = w + 20
    if (p.x > w + 20) p.x = -20
    if (p.y < -20) p.y = h + 20
    if (p.y > h + 20) p.y = -20
    const alpha = p.ba + Math.sin(Date.now() * 0.0011 + p.ph) * 0.08
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(230,205,148,' + Math.max(0.07, alpha) + ')'
    ctx.shadowColor = 'rgba(220,195,135,0.44)'
    ctx.shadowBlur = 8
    ctx.fill()
    ctx.shadowBlur = 0
  }

  // ── 大粒浮游光点 ──
  for (let i = 0; i < floaters.length; i++) {
    const p = floaters[i]
    p.x += p.vx
    p.y += p.vy
    if (p.x < -20) p.x = w + 20
    if (p.x > w + 20) p.x = -20
    if (p.y < -20) p.y = h + 20
    if (p.y > h + 20) p.y = -20
    const alpha = p.ba + Math.sin(Date.now() * 0.0009 + p.ph) * 0.10
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(242,212,138,' + Math.max(0.09, alpha) + ')'
    ctx.shadowColor = 'rgba(230,200,128,0.56)'
    ctx.shadowBlur = 16
    ctx.fill()
    ctx.shadowBlur = 0
  }

  // ── 闪烁微光 ──
  for (let s = sparkles.length - 1; s >= 0; s--) {
    const sp = sparkles[s]
    sp.life -= sp.decay
    if (sp.life <= 0) { sparkles.splice(s, 1); continue }
    const alpha = sp.life * 0.55
    const radius = sp.r + (1 - sp.life) * 3.0
    ctx.beginPath()
    ctx.arc(sp.x, sp.y, radius, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(255,235,172,' + alpha + ')'
    ctx.shadowColor = 'rgba(255,225,142,' + (alpha * 0.88) + ')'
    ctx.shadowBlur = 8
    ctx.fill()
    ctx.shadowBlur = 0
  }

  // ── 文字流萤 — 更大光晕 ──
  for (let j = fireflies.length - 1; j >= 0; j--) {
    const f = fireflies[j]
    f.y += f.vy
    f.life -= 0.001
    if (f.life <= 0) { fireflies.splice(j, 1); continue }
    // 径向渐变光晕（扩大到 20px）
    const glow = ctx.createRadialGradient(f.x, f.y, 0, f.x, f.y, 20)
    glow.addColorStop(0, 'rgba(222,198,148,' + (f.life * 0.28) + ')')
    glow.addColorStop(0.5, 'rgba(218,193,143,' + (f.life * 0.10) + ')')
    glow.addColorStop(1, 'rgba(218,193,143,0)')
    ctx.fillStyle = glow
    ctx.fillRect(f.x - 20, f.y - 20, 40, 40)
    // 文字
    ctx.font = f.size + 'px serif'
    ctx.fillStyle = 'rgba(222,198,148,' + (f.life * 0.76) + ')'
    ctx.shadowColor = 'rgba(212,186,130,0.66)'
    ctx.shadowBlur = 13
    ctx.fillText(f.char, f.x, f.y)
    ctx.shadowBlur = 0
  }

  // ── 远处飞鸟 ──
  for (let b = birds.length - 1; b >= 0; b--) {
    const bird = birds[b]
    bird.x += bird.speed
    bird.wing += bird.wingSpeed
    if (bird.x > w + 50) { birds.splice(b, 1); continue }
    const bw = bird.size
    const wy = Math.sin(bird.wing) * 4.0
    const by = Math.sin(Date.now() * 0.001 + bird.bobPh) * bird.bobAmp
    ctx.strokeStyle = 'rgba(92,97,90,0.36)'
    ctx.lineWidth = 1.15
    ctx.beginPath()
    ctx.moveTo(bird.x - bw, bird.y + by)
    ctx.quadraticCurveTo(bird.x - bw * 0.3, bird.y - bw * 0.65 + wy + by, bird.x, bird.y + by)
    ctx.quadraticCurveTo(bird.x + bw * 0.3, bird.y - bw * 0.65 - wy + by, bird.x + bw, bird.y + by)
    ctx.stroke()
  }

  // ── 掠空流光 ──
  for (let ss = shootingStars.length - 1; ss >= 0; ss--) {
    const s = shootingStars[ss]
    s.x += s.vx
    s.y += s.vy
    s.life -= s.decay
    if (s.life <= 0 || s.x < -60 || s.x > w + 60) { shootingStars.splice(ss, 1); continue }
    const trailAlpha = s.life * 0.42
    ctx.strokeStyle = 'rgba(255,228,160,' + trailAlpha + ')'
    ctx.lineWidth = 0.9
    ctx.beginPath()
    ctx.moveTo(s.x - s.vx * s.len / 25, s.y - s.vy * s.len / 25)
    ctx.lineTo(s.x, s.y)
    ctx.stroke()
    // 头部
    ctx.beginPath()
    ctx.arc(s.x, s.y, 1.5, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(255,242,190,' + (s.life * 0.70) + ')'
    ctx.shadowColor = 'rgba(255,225,150,0.70)'
    ctx.shadowBlur = 10
    ctx.fill()
    ctx.shadowBlur = 0
  }
}

onMounted(() => {
  const canvas = welcomeCanvas.value
  if (!canvas) return
  ctx = canvas.getContext('2d')

  resize()
  initParticles()
  initFloaters()
  animate()

  window.addEventListener('resize', resize)

  // 流萤：每 2.5s，60% 概率，最多 5 只
  fireflyTimer = setInterval(() => {
    if (Math.random() < 0.60) spawnFirefly()
  }, 2500)
  spawnFirefly()
  spawnFirefly()
  spawnFirefly()

  // 闪烁微光：每 1s 生成 2~3 粒
  sparkleTimer = setInterval(() => {
    spawnSparkle()
    spawnSparkle()
    if (Math.random() < 0.55) spawnSparkle()
  }, 1000)

  // 飞鸟：每 5.5s，60% 概率
  birdTimer = setInterval(() => {
    if (Math.random() < 0.60) spawnBirds()
  }, 5500)
  spawnBirds()

  // 掠空流光：每 5s，30% 概率
  starTimer = setInterval(() => {
    if (Math.random() < 0.30) spawnShootingStar()
  }, 5000)
  spawnShootingStar()
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
  if (fireflyTimer) clearInterval(fireflyTimer)
  if (birdTimer) clearInterval(birdTimer)
  if (sparkleTimer) clearInterval(sparkleTimer)
  if (starTimer) clearInterval(starTimer)
  window.removeEventListener('resize', resize)
})

function enterHome() {
  router.push('/login')
}
</script>

<style scoped>
@import "@/assets/styles/variables.css";

.welcome-view {
  --welcome-display: "Bodoni MT", "Baskerville Old Face", Garamond, "Goudy Old Style", Georgia, serif;
  --welcome-body: Constantia, Cambria, Georgia, serif;
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  overflow: hidden;
  padding: var(--space-8);
  background:
    radial-gradient(circle at 50% 28%, rgba(255, 210, 113, 0.18), transparent 24%),
    linear-gradient(180deg, rgba(8, 13, 16, 0.05), rgba(6, 19, 13, 0.55) 78%),
    url('@/assets/images/welcome.jpg') center / cover no-repeat;
  color: white;
}

.welcome-view::before {
  content: "";
  position: absolute;
  inset: auto -8% -14% -8%;
  height: 38%;
  background:
    radial-gradient(ellipse at 50% 100%, rgba(255, 208, 120, 0.2), transparent 60%),
    linear-gradient(rgba(255, 229, 174, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 229, 174, 0.04) 1px, transparent 1px);
  background-size: auto, 5.5rem 5.5rem, 5.5rem 5.5rem;
  opacity: 0.72;
  transform: perspective(34rem) rotateX(66deg);
  transform-origin: bottom;
  animation: driftField 12s linear infinite;
}

.valley-haze,
.light-dust,
.horizon-line {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.valley-haze {
  background:
    radial-gradient(ellipse at 51% 48%, rgba(255, 194, 92, 0.2), transparent 24%),
    radial-gradient(ellipse at 50% 74%, rgba(245, 187, 79, 0.16), transparent 34%),
    linear-gradient(90deg, rgba(2, 9, 7, 0.36), transparent 24%, transparent 76%, rgba(2, 9, 7, 0.38));
  mix-blend-mode: screen;
  animation: hazeBreathe 16s ease-in-out infinite;
}

.light-dust {
  opacity: 0.42;
  background-image:
    radial-gradient(circle at 12% 68%, rgba(255, 223, 151, 0.8) 0 1px, transparent 2px),
    radial-gradient(circle at 42% 58%, rgba(232, 255, 226, 0.56) 0 1px, transparent 2px),
    radial-gradient(circle at 74% 72%, rgba(255, 206, 93, 0.72) 0 1px, transparent 2px);
  background-size: 7rem 7rem, 9rem 9rem, 11rem 11rem;
  animation: floatDust 9s ease-in-out infinite;
}

.horizon-line {
  background:
    linear-gradient(90deg, transparent 18%, rgba(255, 221, 139, 0.16) 36%, rgba(255, 232, 170, 0.5) 50%, rgba(255, 221, 139, 0.16) 64%, transparent 82%),
    radial-gradient(ellipse at 50% 44%, rgba(255, 217, 120, 0.22), transparent 20%);
  -webkit-mask-image: linear-gradient(180deg, transparent 34%, #000 43%, transparent 53%);
  mask-image: linear-gradient(180deg, transparent 34%, #000 43%, transparent 53%);
}

/* ── Canvas 粒子层 ── */
#welcome-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

/* ── 两侧极细装饰线 ── */
.deco-line {
  position: absolute;
  top: 50%;
  height: 1px;
  pointer-events: none;
  z-index: 2;
}

.deco-line-left {
  left: 5%;
  right: calc(50% + 18rem);
  background: linear-gradient(to left, rgba(255, 221, 139, 0.42), transparent);
}

.deco-line-right {
  left: calc(50% + 18rem);
  right: 5%;
  background: linear-gradient(to right, rgba(255, 221, 139, 0.42), transparent);
}

.side-rail {
  position: absolute;
  z-index: 1;
  left: clamp(1rem, 3vw, 2.5rem);
  top: 50%;
  color: rgba(255, 235, 181, 0.48);
  font-family: var(--welcome-body);
  font-size: 0.72rem;
  letter-spacing: 0.42em;
  text-transform: uppercase;
  writing-mode: vertical-rl;
  transform: translateY(-50%);
}

.side-rail::before,
.side-rail::after {
  content: "";
  display: block;
  width: 1px;
  height: 5rem;
  margin: 0.9rem auto;
  background: linear-gradient(transparent, rgba(255, 221, 139, 0.52), transparent);
}

.welcome-stage {
  position: relative;
  z-index: 1;
  isolation: isolate;
  width: min(100%, 72rem);
  display: grid;
  justify-items: center;
  gap: var(--space-3);
  text-align: center;
  animation: fadeUpIn 1.6s var(--ease-out) both;
}

.welcome-stage::before {
  content: "";
  position: absolute;
  z-index: -1;
  inset: -3rem -4rem;
  background: radial-gradient(ellipse at 50% 50%, rgba(5, 15, 10, 0.54), rgba(5, 15, 10, 0.22) 46%, transparent 72%);
  filter: blur(2px);
}

.welcome-stage p {
  margin: 0;
}

.brand-mark {
  width: 4rem;
  height: 4rem;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 214, 112, 0.62);
  border-radius: 1.15rem;
  background:
    linear-gradient(145deg, rgba(255, 232, 166, 0.18), rgba(245, 158, 11, 0.1)),
    rgba(7, 25, 18, 0.22);
  color: #ffd978;
  font-family: var(--welcome-display);
  font-size: 1.25rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  box-shadow:
    0 18px 54px rgba(0, 0, 0, 0.2),
    0 0 34px rgba(255, 196, 87, 0.18);
  backdrop-filter: blur(12px);
}

h1 {
  margin: var(--space-3) 0 0;
  font-family: var(--welcome-display);
  font-size: clamp(4.25rem, 12vw, 9rem);
  font-weight: 700;
  line-height: 0.92;
  letter-spacing: 0.015em;
  color: #f5d076;
  -webkit-text-stroke: 1px rgba(76, 48, 5, 0.18);
  text-shadow:
    0 3px 12px rgba(42, 24, 3, 0.42),
    0 4px 34px rgba(245, 158, 11, 0.32),
    0 0 48px rgba(255, 216, 122, 0.26);
}

.tagline {
  position: relative;
  margin-top: var(--space-1);
  color: rgba(255, 238, 188, 0.96);
  font-family: var(--welcome-display);
  font-size: clamp(1.9rem, 3.45vw, 3.3rem);
  font-weight: 600;
  line-height: 1.12;
  letter-spacing: 0.03em;
  white-space: nowrap;
  text-shadow: 0 10px 32px rgba(0, 0, 0, 0.36);
}

.tagline::after {
  content: "";
  display: block;
  width: min(26rem, 72vw);
  height: 1px;
  margin: var(--space-4) auto 0;
  background: linear-gradient(90deg, transparent, rgba(255, 221, 139, 0.72), transparent);
}

.subtitle {
  max-width: 52rem;
  color: rgba(255, 238, 198, 0.86);
  font-family: var(--welcome-body);
  font-size: clamp(1.16rem, 2vw, 1.42rem);
  letter-spacing: 0.035em;
}

.capability-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-4);
  margin-top: var(--space-4);
}

.capability-list span {
  border: 1px solid rgba(255, 221, 139, 0.34);
  border-radius: 999px;
  padding: 0.68rem 1.16rem;
  background: rgba(7, 25, 18, 0.26);
  color: rgba(255, 238, 198, 0.88);
  font-family: var(--font-serif);
  font-size: 1.02rem;
  letter-spacing: 0.12em;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(14px);
}

button {
  margin-top: var(--space-6);
  min-width: 14.5rem;
  height: 3.85rem;
  border: 1px solid rgba(255, 214, 112, 0.62);
  border-radius: 999px;
  background:
    linear-gradient(135deg, rgba(255, 241, 197, 0.88), rgba(221, 155, 57, 0.8));
  color: #382508;
  font-family: var(--font-serif);
  font-size: 1.16rem;
  font-weight: 850;
  letter-spacing: 0.08em;
  box-shadow:
    0 18px 52px rgba(0, 0, 0, 0.22),
    0 0 28px rgba(245, 158, 11, 0.24);
  backdrop-filter: blur(12px);
  transition:
    transform var(--duration-fast),
    background var(--duration-fast),
    box-shadow var(--duration-fast);
}

button:hover {
  transform: translateY(-2px);
  background:
    linear-gradient(135deg, rgba(255, 248, 225, 0.96), rgba(245, 174, 72, 0.86));
  box-shadow:
    0 22px 62px rgba(0, 0, 0, 0.25),
    0 0 44px rgba(245, 158, 11, 0.3);
}

.release-note {
  margin-top: var(--space-2);
  color: rgba(255, 235, 181, 0.48);
  font-family: var(--welcome-body);
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

@keyframes fadeUpIn {
  from {
    opacity: 0;
    transform: translateY(2rem);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes driftField {
  to {
    background-position: 0 0, 0 5.5rem, 5.5rem 0;
  }
}

@keyframes floatDust {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }

  50% {
    transform: translate3d(0.6rem, -0.8rem, 0);
  }
}

@keyframes hazeBreathe {
  0%,
  100% {
    opacity: 0.82;
  }

  50% {
    opacity: 1;
  }
}

@media (max-width: 620px) {
  .welcome-view {
    padding: var(--space-5);
  }

  .side-rail {
    display: none;
  }

  .capability-list {
    gap: var(--space-2);
  }

  .capability-list span {
    font-size: 0.86rem;
    padding: 0.52rem 0.78rem;
  }

  .tagline {
    white-space: normal;
  }
}
</style>
