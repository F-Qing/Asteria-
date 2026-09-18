<template>
  <div class="breathing-loader" :class="{ small }">
    <div class="orbit">
      <div class="core"></div>
      <span class="satellite">✦</span>
      <span class="satellite satellite-2">✦</span>
    </div>
    <span v-if="text" class="loader-text">{{ text }}</span>
  </div>
</template>

<script setup lang="ts">
/** AI 思考态（A8）：星轨加载——核心光晕呼吸 + 虚线星环慢转 + 双星绕轨 */
withDefaults(
  defineProps<{
    text?: string
    small?: boolean
  }>(),
  { text: '', small: false }
)
</script>

<style scoped>
.breathing-loader {
  display: inline-flex;
  align-items: center;
  gap: var(--space-3);
}
.orbit {
  position: relative;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}
/* 虚线星环：慢速旋转 */
.orbit::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px dashed color-mix(in srgb, var(--primary-soft) 45%, transparent);
  animation: orbit-spin 7s linear infinite;
}
/* 核心光晕：呼吸 */
.core {
  position: absolute;
  inset: 8px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--primary-bg) 0%, var(--lavender) 100%);
  animation: breath 2s ease-in-out infinite;
}
/* 两颗绕轨小星（错开半圈） */
.satellite {
  position: absolute;
  inset: 0;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  font-size: 9px;
  color: var(--primary);
  animation: orbit-spin 2.8s linear infinite;
}
.satellite-2 {
  font-size: 7px;
  color: var(--primary-violet);
  animation-duration: 4.2s;
  animation-direction: reverse;
}
@keyframes orbit-spin {
  to {
    transform: rotate(360deg);
  }
}
.small .orbit {
  width: 22px;
  height: 22px;
}
.small .core {
  inset: 5px;
}
.small .satellite {
  font-size: 6px;
}
.small .satellite-2 {
  font-size: 5px;
}
.loader-text {
  font-size: 13px;
  color: var(--text-muted);
}
@media (prefers-reduced-motion: reduce) {
  .orbit::before,
  .satellite {
    animation: none;
  }
}
</style>
