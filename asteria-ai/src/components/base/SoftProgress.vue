<template>
  <div class="soft-progress" :title="`${Math.round(ratio * 100)}%`">
    <div class="soft-progress-track">
      <div class="soft-progress-bar" :style="{ width: `${ratio * 100}%` }"></div>
    </div>
    <span v-if="showText" class="soft-progress-text">{{ Math.round(ratio * 100) }}%</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

/** 淡蓝→淡紫渐变进度条 */
const props = withDefaults(
  defineProps<{
    /** 0–100 或 0–1（自动识别） */
    value?: number
    showText?: boolean
  }>(),
  { value: 0, showText: false }
)

const ratio = computed(() => {
  const v = props.value > 1 ? props.value / 100 : props.value
  return Math.min(1, Math.max(0, v))
})
</script>

<style scoped>
.soft-progress {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
}
.soft-progress-track {
  flex: 1;
  height: 8px;
  border-radius: var(--radius-pill);
  background: var(--blue-gray);
  overflow: hidden;
}
.soft-progress-bar {
  position: relative;
  height: 100%;
  border-radius: var(--radius-pill);
  background: linear-gradient(90deg, #b5cffa 0%, #ece6f0 100%);
  background-size: 200% 100%;
  animation: progress-flow 2s linear infinite;
  transition: width 0.6s var(--ease-spring);
}
/* 前进端柔光（A5）：进度头部的一点高光 */
.soft-progress-bar::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 14px;
  border-radius: inherit;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.8));
}
.soft-progress-text {
  font-size: 13px;
  color: var(--text-muted);
  min-width: 38px;
  text-align: right;
}

@keyframes progress-flow {
  0% { background-position: 0% 0; }
  100% { background-position: 200% 0; }
}
</style>
