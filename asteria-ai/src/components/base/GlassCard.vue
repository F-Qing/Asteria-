<template>
  <!-- 玻璃面板容器（slot + hover 抬升开关） -->
  <div class="glass-card glass" :class="{ hoverable }">
    <div v-if="title" class="glass-card-header">
      <h3>{{ title }}</h3>
      <slot name="extra" />
    </div>
    <div class="glass-card-body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
/** 玻璃面板容器（slot + hover 抬升开关） */
withDefaults(
  defineProps<{
    title?: string
    hoverable?: boolean
  }>(),
  { title: '', hoverable: false }
)
</script>

<style scoped>
.glass-card {
  position: relative;
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  transition: var(--transition);
}
.glass-card.hoverable:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  /* hover 描边微亮（B4）：克制的流光感，不做旋转跑马灯 */
  border-color: color-mix(in srgb, var(--primary) 30%, var(--glass-line));
}
/* hover 时顶部柔和高光（伪元素渐变，无额外 DOM） */
.glass-card.hoverable::before {
  content: '';
  position: absolute;
  top: 0;
  left: 8%;
  right: 8%;
  height: 1px;
  border-radius: var(--radius-pill);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.85), transparent);
  opacity: 0;
  transition: opacity var(--duration-normal) var(--ease-standard);
  pointer-events: none;
}
html.dark .glass-card.hoverable::before {
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.25), transparent);
}
.glass-card.hoverable:hover::before {
  opacity: 1;
}
.glass-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}
/* 米哈游式标题装饰：品牌渐变小菱形（B1） */
.glass-card-header h3 {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.glass-card-header h3::before {
  content: '';
  width: 7px;
  height: 7px;
  flex-shrink: 0;
  border-radius: 1.5px;
  background: var(--brand-grad);
  transform: rotate(45deg);
  opacity: 0.85;
}
</style>
