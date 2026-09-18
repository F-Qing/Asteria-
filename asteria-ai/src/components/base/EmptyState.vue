<template>
  <div class="empty-state">
    <!-- 场景模式：手绘 SVG 小场景（B2）；否则用线性图标圆底 -->
    <IllustrationScene v-if="scene" :name="scene" class="empty-scene" />
    <div v-else class="empty-figure">
      <component :is="icon ?? Library" :size="30" :stroke-width="1.6" class="empty-icon" />
      <span class="empty-spark" aria-hidden="true">✦</span>
    </div>
    <p class="empty-text">{{ text }}</p>
    <p v-if="hint" class="empty-hint">{{ hint }}</p>
    <div v-if="$slots.default" class="empty-action">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { Library } from 'lucide-vue-next'
import IllustrationScene from '@/components/base/IllustrationScene.vue'

/** 空状态：传 scene 用手绘场景（B2），否则用 icon 圆底；文案柔和 + 可选操作按钮
 *  注意：icon 不能设组件默认值——Vue 会把函数默认值当工厂调用，lucide 图标会被裸调崩溃 */
withDefaults(
  defineProps<{
    icon?: Component
    scene?: 'books' | 'nap' | 'plane' | 'sprout' | 'trophy' | 'compass'
    text?: string
    hint?: string
  }>(),
  { text: '这里还空空的呢', hint: '' }
)
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-7) var(--space-5);
  text-align: center;
}
/* 粉彩渐变圆底 + 受光细边：统一、干净，不再使用彩色 emoji */
.empty-figure {
  position: relative;
  width: 76px;
  height: 76px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--sakura) 55%, transparent),
    color-mix(in srgb, var(--blue-gray) 70%, transparent)
  );
  border: 1px solid var(--glass-line);
  box-shadow: var(--shadow-sm), var(--rim-light);
  margin-bottom: var(--space-4);
  animation: float 6s ease-in-out infinite;
}
.empty-icon {
  color: var(--primary-soft);
}
/* 角标小星星：保留一点二次元基因，慢闪点缀（A5） */
.empty-spark {
  position: absolute;
  top: -4px;
  right: -2px;
  font-family: var(--font-accent);
  font-size: 13px;
  color: var(--primary-violet);
  animation: spark-twinkle 3.4s ease-in-out infinite;
}
@keyframes spark-twinkle {
  0%, 100% { opacity: 0.35; transform: scale(0.9) rotate(0deg); }
  50% { opacity: 0.95; transform: scale(1.12) rotate(18deg); }
}
.empty-text {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-sub);
}
.empty-hint {
  margin-top: var(--space-2);
  font-size: 13px;
  color: var(--text-muted);
}
.empty-action {
  margin-top: var(--space-5);
}
</style>
