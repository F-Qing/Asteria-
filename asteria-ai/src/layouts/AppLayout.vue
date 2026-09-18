<template>
  <div class="app-layout">
    <!-- 晨昏天空层：4 层恒驻渐变，激活层 2s 淡入淡出（随时间换天色） -->
    <div class="sky" aria-hidden="true">
      <div
        v-for="p in PHASES"
        :key="p"
        class="sky-layer"
        :class="[`sky-${p}`, { active: p === phase }]"
      />
    </div>

    <!-- 深夜戏台：星星 / 流星 / 远云（仅 night 点亮，opacity + play-state 控制，零常驻开销） -->
    <div class="night-scene" :class="{ on: phase === 'night' }" aria-hidden="true">
      <span v-for="s in stars" :key="'star' + s.id" class="star" :style="s.style" />
      <span v-for="m in meteors" :key="'meteor' + m.id" class="meteor" :style="m.style" />
      <span class="cloud cloud-far" />
      <span class="cloud cloud-near" />
    </div>

    <!-- 全局氛围晕染层（token 控制强度，暗色极淡；指针穿透） -->
    <div class="ambient-decor" aria-hidden="true">
      <span class="glow-blob ambient-1"></span>
      <span class="glow-blob ambient-2"></span>
      <span class="glow-blob ambient-3"></span>
    </div>

    <!-- ── 左侧可折叠导航（240px ↔ 72px，0.3s 过渡） ── -->
    <aside class="sidebar glass" :class="{ collapsed: app.sidebarCollapsed }">
      <div class="sidebar-header">
        <router-link to="/" class="logo" :title="'Asteria AI'">
          <span class="logo-icon">✦</span>
          <span v-show="!app.sidebarCollapsed" class="logo-text">Asteria</span>
        </router-link>
        <button class="collapse-btn" :title="app.sidebarCollapsed ? '展开导航' : '折叠导航'" @click="app.toggleSidebar()">
          <component :is="app.sidebarCollapsed ? PanelLeftOpen : PanelLeftClose" :size="16" :stroke-width="1.8" />
        </button>
      </div>

      <nav class="menu">
        <!-- 滑动指示条：胶囊背景跟随激活项平滑移动（折叠/展开两种宽度都适用） -->
        <div v-show="indicator.visible" class="menu-indicator" :style="indicatorStyle" />
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          ref="itemRefs"
          :to="item.path"
          class="menu-item"
          :class="{ active: isActive(item.path) }"
          :title="item.label"
        >
          <component :is="item.icon" class="menu-icon" :size="18" :stroke-width="1.8" />
          <span v-show="!app.sidebarCollapsed" class="menu-label">{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <CountdownCard :compact="app.sidebarCollapsed" />
      </div>
    </aside>

    <!-- ── 主区：顶栏 + 内容 ── -->
    <div class="main-area">
      <header class="topbar glass">
        <h2 class="page-title">{{ route.meta.title ?? 'Asteria AI' }}</h2>
        <div class="avatar" title="Asteria 用户">A</div>
      </header>

      <main class="content" :class="{ immersive: route.meta.immersive }">
        <router-view v-slot="{ Component }">
          <Transition name="page" mode="out-in">
            <component :is="Component" />
          </Transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import {
  House,
  Bookmark,
  FileUp,
  MessagesSquare,
  Lightbulb,
  Settings,
  PanelLeftClose,
  PanelLeftOpen
} from 'lucide-vue-next'
import { useAppStore } from '@/stores/app'
import { useDayPhase, type DayPhase } from '@/composables/useDayPhase'
import CountdownCard from '@/components/business/CountdownCard.vue'

const app = useAppStore()
const route = useRoute()

/* ── 晨昏时段：驱动天空层/氛围光斑/问候语，同步到 body[data-phase] 供全局 CSS 使用 ── */
const PHASES: DayPhase[] = ['morning', 'day', 'dusk', 'night']
const phase = useDayPhase()
watchEffect(() => {
  document.body.dataset.phase = phase.value
})

/* ── 深夜场景：确定性伪随机布点（刷新不跳变）；全部 transform/opacity 动画 ── */
const stars = Array.from({ length: 14 }, (_, i) => {
  const seeded = (n: number) => {
    const v = Math.sin(i * 127.1 + n * 311.7) * 43758.5453
    return v - Math.floor(v)
  }
  const size = 4 + Math.round(seeded(1) * 3)
  return {
    id: i,
    style: {
      width: `${size}px`,
      height: `${size}px`,
      left: `${4 + seeded(2) * 92}%`,
      /* 星星只分布在顶部天空带（hero 露天区），避免透出在卡片上 */
      top: `${3 + seeded(3) * 19}%`,
      animationDuration: `${2.6 + seeded(4) * 2.8}s`,
      animationDelay: `${-seeded(5) * 4}s`
    }
  }
})

const meteors = [
  { id: 0, style: { right: '16%', top: '9%', animationDuration: '9s', animationDelay: '2.2s' } },
  { id: 1, style: { right: '52%', top: '20%', animationDuration: '12s', animationDelay: '7.5s' } }
]

/* 菜单统一 lucide 线性图标（1.8px 描边），与全站图标语言一致 */
const menuItems = [
  { path: '/', label: '首页', icon: House },
  { path: '/banks', label: '题库', icon: Bookmark },
  { path: '/banks/import', label: '导入', icon: FileUp },
  { path: '/chat', label: 'AI 助手', icon: MessagesSquare },
  { path: '/knowledge-summary', label: '知识点', icon: Lightbulb },
  { path: '/setting', label: '设置', icon: Settings }
]

/*
 * 激活互斥规则（修复“快速点击后两个菜单同时高亮”）：
 * 1. 完全相等优先：route.path === item.path；
 * 2. 详情类子路由归并父级：/banks/:id 高亮「题库」，
 *    但 /banks/import 是独立菜单项，只高亮「导入」自身；
 * 3. 首页必须精确匹配（避免 startsWith('/') 全部命中）。
 * 状态完全由 route 响应式驱动，不做任何缓存，点击后立即更新。
 */
function isActive(path: string): boolean {
  if (path === '/') return route.path === '/'
  if (route.path === path) return true
  if (path === '/banks' && route.matched.some((r) => r.path === '/banks/:id')) return true
  return false
}

/* ── 滑动指示条位置测量（路由或折叠态变化后重新量） ── */
const itemRefs = ref<HTMLElement[]>([])
const indicator = reactive({ top: 0, height: 0, visible: false })

const indicatorStyle = computed(() => ({
  transform: `translateY(${indicator.top}px)`,
  height: `${indicator.height}px`
}))

async function updateIndicator() {
  await nextTick()
  const idx = menuItems.findIndex((item) => isActive(item.path))
  const el = idx >= 0 ? itemRefs.value?.[idx] : null
  if (!el) {
    indicator.visible = false
    return
  }
  indicator.top = el.offsetTop
  indicator.height = el.offsetHeight
  indicator.visible = true
}

onMounted(updateIndicator)
watch([() => route.path, () => app.sidebarCollapsed], updateIndicator, { flush: 'post' })
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ── 全局氛围晕染（强度/模糊由 token 控制，明暗两套） ── */
.ambient-decor {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}
.ambient-1 {
  width: 440px;
  height: 440px;
  left: -150px;
  top: -170px;
  --blob-color: var(--sakura);
  opacity: var(--ambient-opacity);
}
.ambient-2 {
  width: 480px;
  height: 480px;
  right: -170px;
  top: 16%;
  --blob-color: var(--lavender);
  opacity: var(--ambient-opacity);
  animation-delay: -6s;
}
.ambient-3 {
  width: 420px;
  height: 420px;
  left: 36%;
  bottom: -190px;
  --blob-color: var(--blue-gray);
  opacity: var(--ambient-opacity);
  animation-delay: -12s;
}
.sidebar,
.main-area {
  position: relative;
  z-index: 1;
}

/* ── 深夜戏台：星星缓闪 + 流星周期划过 + 远云缓漂 ──
   z-index -1：夹在天空层(-2)与内容(≥0)之间——星星只出现在天空暴露区，
   卡片后面的星会被 backdrop-filter 模糊掉，不会形成锐利"脏点"；
   未点亮时 opacity:0 且动画 paused，零常驻渲染开销 */
.night-scene {
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  opacity: 0;
  transition: opacity 2s var(--ease-standard);
}
.night-scene.on {
  opacity: 1;
}
.night-scene .star,
.night-scene .meteor,
.night-scene .cloud {
  animation-play-state: paused;
}
.night-scene.on .star,
.night-scene.on .meteor,
.night-scene.on .cloud {
  animation-play-state: running;
}
.star {
  position: absolute;
  border-radius: 50%;
  /* 柔光渐变星点：即使 backdrop-filter 不可用、透过玻璃也呈微弱光斑而非锐利脏点 */
  background: radial-gradient(
    circle,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(255, 255, 255, 0.35) 40%,
    transparent 68%
  );
  opacity: 0.8;
  animation: star-twinkle 3s ease-in-out infinite;
}
@keyframes star-twinkle {
  0%, 100% { opacity: 0.1; }
  50% { opacity: 0.85; }
}
.meteor {
  position: absolute;
  width: 120px;
  height: 1.5px;
  border-radius: var(--radius-pill);
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.95));
  opacity: 0;
  animation: meteor-streak 9s ease-in infinite;
}
@keyframes meteor-streak {
  0% { transform: translate(0, 0) rotate(-32deg); opacity: 0; }
  3% { opacity: 1; }
  8% { transform: translate(-360px, 225px) rotate(-32deg); opacity: 0; }
  100% { transform: translate(-360px, 225px) rotate(-32deg); opacity: 0; }
}
.cloud {
  position: absolute;
  border-radius: 50%;
}
/* 径向渐变柔边云（无 filter blur） */
.cloud-far {
  width: 440px;
  height: 120px;
  left: 5%;
  top: 24%;
  background: radial-gradient(closest-side, rgba(186, 199, 232, 0.3), transparent);
  animation: cloud-drift 46s ease-in-out infinite alternate;
}
.cloud-near {
  width: 540px;
  height: 150px;
  right: 2%;
  top: 56%;
  background: radial-gradient(closest-side, rgba(164, 180, 222, 0.26), transparent);
  animation: cloud-drift 58s ease-in-out infinite alternate;
  animation-delay: -21s;
}
@keyframes cloud-drift {
  from { transform: translateX(-4%); }
  to { transform: translateX(6%); }
}

/* ── 侧栏 ── */
.sidebar {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  margin: var(--space-4);
  border-radius: var(--radius-xl);
  transition: width var(--duration-normal) var(--ease-standard);
  overflow: hidden;
}
.sidebar.collapsed {
  width: 72px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-5) var(--space-4) var(--space-4);
  gap: var(--space-2);
}
.sidebar.collapsed .sidebar-header {
  flex-direction: column;
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-family: var(--font-accent);
  font-weight: 600;
  font-size: 20px;
  color: var(--text-main);
  white-space: nowrap;
}
.logo-icon {
  color: var(--primary-soft);
  font-size: 20px;
  display: inline-block;
  animation: float 5s ease-in-out infinite;
}
.logo-text {
  letter-spacing: 0.04em;
}

.collapse-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
}
.collapse-btn:hover {
  background: var(--blue-gray);
  color: var(--text-main);
}

/* ── 菜单 ── */
.menu {
  position: relative;
  flex: 1;
  padding: var(--space-2) var(--space-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  overflow-y: auto;
}

/* 滑动胶囊：激活项背景，translateY 平滑跟随 */
.menu-indicator {
  position: absolute;
  left: var(--space-3);
  right: var(--space-3);
  top: 0;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--primary-bg) 40%, transparent);
  transition: transform var(--duration-normal) var(--ease-standard),
    height var(--duration-normal) var(--ease-standard);
  pointer-events: none;
}
/* 指示条左侧 3px 品牌渐变边 */
.menu-indicator::before {
  content: '';
  position: absolute;
  left: 0;
  top: 22%;
  height: 56%;
  width: 3px;
  border-radius: var(--radius-pill);
  background: var(--brand-grad);
}

.menu-item {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 10px var(--space-3);
  border-radius: var(--radius-md);
  color: var(--text-sub);
  font-size: 15px;
  font-weight: 500;
  white-space: nowrap;
  transition: color var(--duration-normal) var(--ease-standard),
    background var(--duration-normal) var(--ease-standard);
}
.menu-item:hover {
  background: color-mix(in srgb, var(--blue-gray) 55%, transparent);
  color: var(--text-main);
}
/* 按压反馈（A5 微交互） */
.menu-item:active {
  transform: scale(0.97);
}
/* hover 图标微动 */
.menu-item:hover .menu-icon {
  transform: translateX(2px);
}

.menu-item.active {
  color: var(--text-main);
  /* 不加粗：仅用颜色区分，避免选中瞬间字重跳变 */
}

.menu-icon {
  width: 20px;
  display: flex;
  justify-content: center;
  color: var(--text-muted);
  flex-shrink: 0;
  transition: transform var(--duration-normal) var(--ease-standard),
    color var(--duration-normal) var(--ease-standard);
}
.menu-item:hover .menu-icon {
  color: var(--primary-soft);
}
.menu-item.active .menu-icon {
  color: var(--primary);
  /* 激活瞬间弹性出现 */
  animation: icon-pop 0.35s var(--ease-spring);
}
.menu-label {
  overflow: hidden;
  text-overflow: ellipsis;
  transition: opacity var(--duration-normal) var(--ease-standard);
}

.sidebar-footer {
  padding: var(--space-3);
}

/* ── 主区 ── */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: var(--space-4) var(--space-4) 0 0;
  padding: var(--space-3) var(--space-5);
  border-radius: var(--radius-xl);
  gap: var(--space-4);
}

.page-title {
  font-size: 18px;
  white-space: nowrap;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--brand-grad);
  color: var(--text-on-primary);
  font-family: var(--font-accent);
  font-weight: 600;
  font-size: 17px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: var(--transition);
}
.avatar:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-5) var(--space-6);
}
.content > * {
  max-width: 1200px;
  margin: 0 auto;
}
.content.immersive > * {
  max-width: 960px;
}

@media (max-width: 768px) {
  .content {
    padding: var(--space-4);
  }
}
</style>
