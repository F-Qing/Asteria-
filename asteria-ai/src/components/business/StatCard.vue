<template>
  <GlassCard hoverable class="stat-card">
    <!-- 加载中：骨架占位（A2），数据到达后无缝替换 -->
    <template v-if="loading">
      <div class="skeleton stat-icon-sk" />
      <div class="stat-info">
        <div class="skeleton stat-val-sk" />
        <div class="skeleton stat-label-sk" />
      </div>
    </template>
    <template v-else>
      <div class="stat-icon" :style="{ background: iconBg }">
        <component :is="icon" :size="20" :stroke-width="1.8" :style="{ color: iconColor }" />
      </div>
      <div class="stat-info">
        <div class="stat-value">
          <template v-if="value != null">{{ display }}<span v-if="suffix" class="stat-suffix">{{ suffix }}</span></template>
          <span v-else class="stat-empty">—</span>
        </div>
        <div class="stat-label">{{ label }}</div>
      </div>
    </template>
  </GlassCard>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { onMounted, ref, watch } from 'vue'
import GlassCard from '@/components/base/GlassCard.vue'

/** 统计卡（线性图标 + 数字滚动递增 + 副标题）；value 为 null 时显示「—」（无数据不编造） */
const props = withDefaults(
  defineProps<{
    icon: Component
    value: number | null
    label: string
    suffix?: string
    iconBg?: string
    iconColor?: string
    loading?: boolean
  }>(),
  { suffix: '', iconBg: 'var(--blue-gray)', iconColor: 'var(--primary-soft)', loading: false }
)

/* 数字滚动递增：requestAnimationFrame + ease-out，进入视口（挂载）即触发；
   只更新文本，不动布局属性 */
const display = ref(0)
let rafId = 0

function animateTo(target: number) {
  cancelAnimationFrame(rafId)
  const from = display.value
  if (from === target) return
  const duration = 800
  const start = performance.now()
  const tick = (now: number) => {
    const t = Math.min(1, (now - start) / duration)
    const eased = 1 - Math.pow(1 - t, 3) // easeOutCubic
    display.value = Math.round(from + (target - from) * eased)
    if (t < 1) rafId = requestAnimationFrame(tick)
  }
  rafId = requestAnimationFrame(tick)
}

onMounted(() => {
  if (props.value != null) animateTo(props.value)
})
watch(
  () => props.value,
  (v) => {
    if (v != null) animateTo(v)
  }
)
</script>

<style scoped>
.stat-card :deep(.glass-card-body) {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}
.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  transition: transform var(--duration-normal) var(--ease-spring);
}
/* hover 图标反向轻移（与卡片抬升形成微视差，B3） */
.stat-card:hover .stat-icon {
  transform: translate(3px, -4px);
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}
/* hover 时数字换品牌渐变（B3） */
.stat-card:hover .stat-value:not(:has(.stat-empty)) {
  background: var(--brand-grad);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.stat-suffix {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-muted);
  margin-left: 2px;
}
.stat-empty {
  color: var(--text-muted);
}
.stat-label {
  font-size: 13px;
  color: var(--text-muted);
}
/* 骨架占位尺寸（A2） */
.stat-icon-sk {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}
.stat-val-sk {
  width: 64px;
  height: 26px;
  margin-bottom: var(--space-2);
}
.stat-label-sk {
  width: 48px;
  height: 14px;
}
</style>
