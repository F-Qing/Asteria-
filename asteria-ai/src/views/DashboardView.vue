<template>
  <div class="dashboard">
    <!-- 问候（滚动视差：比内容滚得慢，产生纵深） -->
    <section ref="heroEl" class="hero">
      <!-- 晨昏天体：随真实时间在天弧上连续移动（30s 一 Tick，平滑挪动） -->
      <div class="celestial" :class="phase" :style="celestialStyle" aria-hidden="true">
        <span class="sun" />
        <span class="moon" />
        <span class="veil" />
      </div>
      <p class="eyebrow hero-eyebrow">Study Overview</p>
      <h1 class="greeting">{{ greeting }}，今天准备复习什么？</h1>
      <p class="hero-sub">✦ 今天也一起加油复习吧</p>
    </section>

    <!-- 统计行：4× StatCard（数字滚动递增，入场从左到右错落） -->
    <section class="stat-row">
      <StatCard class="reveal" :style="{ '--stagger': 1 }" :icon="Library" :value="stats.bankCount" label="题库数" icon-bg="var(--blue-gray)" :loading="statsLoading" />
      <StatCard class="reveal" :style="{ '--stagger': 2 }" :icon="ListChecks" :value="stats.questionCount" label="题目总数" icon-bg="var(--lavender)" :loading="statsLoading" />
      <StatCard class="reveal" :style="{ '--stagger': 3 }" :icon="PenLine" :value="stats.practiced" label="已刷题" icon-bg="var(--mint)" :loading="statsLoading" />
      <StatCard class="reveal" :style="{ '--stagger': 4 }" :icon="Target" :value="stats.accuracy" suffix="%" label="正确率" icon-bg="var(--sakura)" :loading="statsLoading" />
    </section>

    <div class="dashboard-grid reveal" style="--stagger: 5">
      <CountdownCard />
      <GlassCard title="最近刷题" class="recent-card">
        <!-- 有记录就列出来，没有才显示空态；点一条直接回到那次会话 -->
        <div v-if="recentSessions.length" class="recent-list">
          <div
            v-for="s in recentSessions"
            :key="s.id"
            class="recent-item"
            @click="router.push(`/practice/${s.id}`)"
          >
            <div class="recent-info">
              <span class="recent-bank">{{ s.bankName }}</span>
              <span class="recent-meta">
                {{ s.mode === 'RANDOM' ? '随机' : '顺序' }} · {{ s.answeredCount }}/{{ s.totalCount }} 题 ·
                {{ formatRelative(s.createdAt) }}
              </span>
            </div>
            <SoftTag :variant="s.status === 'COMPLETED' ? 'mint' : 'blue-gray'">
              {{ s.status === 'COMPLETED' ? '已完成' : '进行中' }}
            </SoftTag>
          </div>
        </div>
        <EmptyState v-else scene="nap" text="还没有刷题记录" hint="去题库页开始第一次练习吧">
          <SoftButton variant="outline" :icon="ArrowRight" @click="router.push('/banks')">去刷题</SoftButton>
        </EmptyState>
      </GlassCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Library, ListChecks, PenLine, Target, ArrowRight } from 'lucide-vue-next'
import StatCard from '@/components/business/StatCard.vue'
import CountdownCard from '@/components/business/CountdownCard.vue'
import GlassCard from '@/components/base/GlassCard.vue'
import SoftButton from '@/components/base/SoftButton.vue'
import SoftTag from '@/components/base/SoftTag.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { useDayPhase, type DayPhase } from '@/composables/useDayPhase'
import { listBanks } from '@/api/bank'
import { getStudyStats, listRecentSessions } from '@/api/practice'
import { formatRelative } from '@/utils/format'
import type { RecentSession } from '@/types'
import { useReveal } from '@/composables/useReveal'

const router = useRouter()

/* 滚动渐入：主要区块进入视口时柔和浮现 */
useReveal()

/* ── 晨昏时段：问候语与天体（太阳/月亮）随时间变化 ── */
const phase = useDayPhase()
const GREETINGS: Record<DayPhase, string> = {
  night: '夜深了',
  morning: '早上好',
  day: '下午好',
  dusk: '晚上好'
}
const greeting = computed(() => GREETINGS[phase.value])

/* ── 天体连续运动（A4）：太阳 6-18 点、月亮 18-次日 6 点各走一道弧，
   弧线限制在 hero 右侧无文字区（58%→88%），避免实心天体盖住问候语；
   ?phase 预览时使用该时段的代表时刻，保证天体位置与场景一致 ── */
const now = ref(new Date())
let clockTimer: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  clockTimer = setInterval(() => {
    now.value = new Date()
  }, 30_000)
})
onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
})
const PHASE_REPRESENTATIVE_HOUR: Record<DayPhase, number> = { morning: 9, day: 13, dusk: 20, night: 0 }
const celestialStyle = computed(() => {
  const override = new URLSearchParams(window.location.search).get('phase')
  let h: number
  if (override && override in PHASE_REPRESENTATIVE_HOUR) {
    h = PHASE_REPRESENTATIVE_HOUR[override as DayPhase]
  } else {
    h = now.value.getHours() + now.value.getMinutes() / 60
  }
  const p =
    phase.value === 'night'
      ? ((h + 24 - 18) % 24) / 12 // 月亮：18:00→次日 6:00 走 0→1
      : (h - 6) / 12 // 太阳：6:00→18:00 走 0→1
  const pc = Math.min(1, Math.max(0, p))
  const x = 58 + pc * 30 // 水平 58%→88%（右侧无文字区）
  const y = 4 + (1 - Math.sin(pc * Math.PI)) * 20 // 弧顶在正午/午夜
  return { left: `${x}%`, top: `${y}%` }
})

/* ── hero 滚动视差（A10）：问候区以 0.22 倍速跟随滚动，transform-only ── */
const heroEl = ref<HTMLElement>()
let scrollEl: HTMLElement | null = null
const onHeroScroll = () => {
  if (!heroEl.value || !scrollEl) return
  heroEl.value.style.transform = `translateY(${Math.min(scrollEl.scrollTop * 0.22, 90)}px)`
}
onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  scrollEl = heroEl.value?.closest('.content') ?? null
  scrollEl?.addEventListener('scroll', onHeroScroll, { passive: true })
})
onBeforeUnmount(() => {
  scrollEl?.removeEventListener('scroll', onHeroScroll)
  scrollEl = null
})

/* ── 统计数据：题库数/题目数/已刷题/正确率；无数据时显示「—」，不使用编造占位值 ── */
const statsLoading = ref(true)
const stats = ref<{
  bankCount: number | null
  questionCount: number | null
  practiced: number | null
  accuracy: number | null
}>({ bankCount: null, questionCount: null, practiced: null, accuracy: null })

/* 首页「最近刷题」列表（最近 5 条）；拉不到就保持空数组 → 显示空态 */
const recentSessions = ref<RecentSession[]>([])

onMounted(async () => {
  // 三个请求互不依赖，各自 try/catch：任何一个失败都不影响其它两个的展示
  await Promise.all([
    (async () => {
      try {
        const data = await listBanks({ page: 1, pageSize: 100 })
        stats.value.bankCount = data.total
        stats.value.questionCount = data.list.reduce((sum, b) => sum + b.questionCount, 0)
      } catch {
        // 静默：后端未就绪时保持「—」，不显示假数据
      }
    })(),
    (async () => {
      try {
        const s = await getStudyStats()
        stats.value.practiced = s.practiced
        // 后端给的就是 0~100，而 StatCard 是「数字 + %」直接显示，不用再换算
        stats.value.accuracy = s.accuracy
      } catch {
        // 静默
      }
    })(),
    (async () => {
      try {
        recentSessions.value = await listRecentSessions(5)
      } catch {
        // 静默：拉不到就显示空态
      }
    })()
  ])
  statsLoading.value = false
})
</script>

<style scoped>
.dashboard {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}
.hero {
  position: relative;
  padding: var(--space-4) 0 var(--space-2);
}
.hero-eyebrow {
  margin-bottom: var(--space-2);
}

/* ── 晨昏天体：位置由内联样式驱动（真实时间弧线），left/top 30s 一挪，平滑过渡 ── */
.celestial {
  position: absolute;
  width: 130px;
  height: 130px;
  pointer-events: none;
  transition: left 1.5s linear, top 1.5s linear;
}
.celestial span {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 2s var(--ease-standard), transform 2s var(--ease-standard);
}
/* 太阳：白热核心→暖金→临边微暗的实心盘（拟真但保持粉彩画感） */
.celestial .sun {
  background:
    radial-gradient(circle at 50% 50%, transparent 0 60%, rgba(214, 142, 60, 0.28) 84%, rgba(214, 142, 60, 0.45) 100%),
    radial-gradient(circle at 45% 40%, #fffdf2 0%, #fff0c4 36%, #ffd88f 62%, #f9bc63 86%, #f2a94e 100%);
  box-shadow:
    0 0 24px 8px rgba(255, 205, 130, 0.6),
    0 0 70px 28px rgba(255, 190, 110, 0.3),
    0 0 140px 64px rgba(255, 180, 100, 0.12);
  transform: translateY(10px) scale(0.94);
}
/* 日冕呼吸层（transform-only，小元素无性能顾虑） */
.celestial .sun::before {
  content: '';
  position: absolute;
  inset: -24%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 212, 140, 0.42) 0%, rgba(255, 196, 120, 0.14) 46%, transparent 70%);
  animation: corona-breath 7s ease-in-out infinite;
}
@keyframes corona-breath {
  0%, 100% { transform: scale(1); opacity: 0.85; }
  50% { transform: scale(1.08); opacity: 1; }
}
/* 黄昏太阳：更橙更深、临边更重 */
.celestial.dusk .sun {
  background:
    radial-gradient(circle at 50% 50%, transparent 0 58%, rgba(196, 92, 42, 0.32) 84%, rgba(196, 92, 42, 0.5) 100%),
    radial-gradient(circle at 45% 40%, #fff3d2 0%, #ffcf92 34%, #ffaa66 62%, #ef8544 86%, #e0703a 100%);
  box-shadow:
    0 0 28px 10px rgba(255, 162, 92, 0.6),
    0 0 84px 38px rgba(255, 140, 84, 0.32),
    0 0 160px 76px rgba(255, 128, 72, 0.14);
}
.celestial.dusk .sun::before {
  background: radial-gradient(circle, rgba(255, 172, 110, 0.48) 0%, rgba(255, 152, 92, 0.16) 46%, transparent 70%);
}
/* 月亮：银灰球面 + 明显月海斑 + 明暗界线——不发亮，和太阳明确区分 */
.celestial .moon {
  background:
    radial-gradient(circle at 34% 26%, rgba(118, 130, 168, 0.52) 0 10%, transparent 13%),
    radial-gradient(circle at 62% 46%, rgba(118, 130, 168, 0.44) 0 8%, transparent 10%),
    radial-gradient(circle at 44% 62%, rgba(118, 130, 168, 0.38) 0 7%, transparent 9%),
    radial-gradient(circle at 66% 66%, rgba(118, 130, 168, 0.32) 0 5%, transparent 7%),
    radial-gradient(circle at 70% 24%, rgba(118, 130, 168, 0.3) 0 4%, transparent 6%),
    radial-gradient(circle at 34% 30%, #eef1f9 0%, #dde3f0 36%, #c3cce4 62%, #a3aed0 84%, #8e99bd 100%);
  box-shadow:
    inset -10px -12px 26px rgba(62, 74, 110, 0.5),
    inset 3px 4px 10px rgba(255, 255, 255, 0.55),
    0 0 10px 2px rgba(205, 216, 244, 0.3);
  transform: translateY(-10px) scale(0.92);
}
/* 极淡月晕（只衬托轮廓，不做发光——发光是太阳的事） */
.celestial .moon::before {
  content: '';
  position: absolute;
  inset: -16%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(206, 216, 242, 0.18) 0%, transparent 62%);
  opacity: 0.6;
}
.celestial.morning .sun,
.celestial.day .sun,
.celestial.dusk .sun {
  opacity: 1;
}
.celestial.morning .sun,
.celestial.day .sun {
  transform: translateY(0) scale(1);
}
.celestial.night .moon {
  opacity: 1;
  transform: translateY(0) scale(1);
}
.celestial.night .moon::before {
  animation: moon-halo 9s ease-in-out infinite;
}
@keyframes moon-halo {
  0%, 100% { opacity: 0.7; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.05); }
}
@media (prefers-reduced-motion: reduce) {
  .celestial .sun::before,
  .celestial.night .moon::before {
    animation: none;
  }
}
/* 半遮月薄云：周期性从月亮前掠过（transform 往复，无 filter） */
.celestial .veil {
  inset: auto;
  left: -46%;
  top: 32%;
  width: 74%;
  height: 40%;
  border-radius: var(--radius-pill);
  background: radial-gradient(closest-side, rgba(216, 226, 248, 0.9), rgba(216, 226, 248, 0) 76%);
  opacity: 0;
  transform: translateX(-36%);
}
.celestial.night .veil {
  opacity: 0.85;
  animation: veil-drift 15s ease-in-out infinite alternate;
}
@keyframes veil-drift {
  from { transform: translateX(-36%); }
  to { transform: translateX(26%); }
}

@media (max-width: 1024px) {
  .celestial {
    width: 96px;
    height: 96px;
  }
}

/* ── 亮色主题 + 深夜：问候语落在深蓝夜空上，换浅色墨 ── */
html:not(.dark) body[data-phase='night'] .greeting {
  background: linear-gradient(100deg, #eef1fb 0%, #9db4ec 55%, #bcaaf2 100%);
  -webkit-background-clip: text;
  background-clip: text;
}
html:not(.dark) body[data-phase='night'] .hero-sub {
  color: rgba(224, 231, 250, 0.75);
}
html:not(.dark) body[data-phase='night'] .hero-eyebrow {
  color: rgba(224, 231, 250, 0.55);
}
/* 问候语：整行恒定品牌渐变（墨色→蓝→紫），不再做流动高光 */
.greeting {
  margin-bottom: var(--space-2);
  background: linear-gradient(
    100deg,
    var(--text-main) 0%,
    var(--primary) 52%,
    var(--primary-violet) 100%
  );
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.hero-sub {
  font-family: var(--font-display);
  font-synthesis: none;
  color: var(--text-muted);
  font-size: 14px;
}
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-5);
}
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-5);
  align-items: stretch;
}

/* 最近刷题列表：一行一条，hover 轻微上浮 + 染色（和题库卡片的手感一致） */
.recent-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: var(--glass-bg);
  cursor: pointer;
  transition: var(--transition);
}
.recent-item:hover {
  border-color: var(--primary-soft);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}
.recent-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.recent-bank {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recent-meta {
  font-size: 13px;
  color: var(--text-muted);
}

@media (max-width: 1024px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
