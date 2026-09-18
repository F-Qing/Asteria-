<template>
  <button class="soft-btn" :class="[variant, { block }]" :disabled="disabled || loading" :type="nativeType">
    <LoaderCircle v-if="loading" class="soft-btn-spin" :size="16" :stroke-width="2" />
    <component :is="icon" v-else-if="icon" :size="16" :stroke-width="2" />
    <span v-if="$slots.default"><slot /></span>
  </button>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { LoaderCircle } from 'lucide-vue-next'

/** 圆角按钮（primary/outline/danger/ghost）；icon 传 lucide 图标组件 */
withDefaults(
  defineProps<{
    variant?: 'primary' | 'outline' | 'danger' | 'ghost'
    icon?: Component
    loading?: boolean
    disabled?: boolean
    block?: boolean
    nativeType?: 'button' | 'submit'
  }>(),
  { variant: 'primary', loading: false, disabled: false, block: false, nativeType: 'button' }
)
</script>

<style scoped>
.soft-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: 9px var(--space-5);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 500;
  transition: var(--transition);
  border: 1px solid transparent;
  white-space: nowrap;
}
.soft-btn:active:not(:disabled) {
  transform: scale(0.98);
}
/* hover 时图标轻轻前移（A5 微交互） */
.soft-btn svg {
  transition: transform 0.25s var(--ease-spring);
}
.soft-btn:hover:not(:disabled) svg {
  transform: translateX(2px);
}
.soft-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.soft-btn.block {
  width: 100%;
}
.soft-btn-spin {
  animation: soft-btn-rotate 0.8s linear infinite;
}
@keyframes soft-btn-rotate {
  to {
    transform: rotate(360deg);
  }
}

.soft-btn.primary {
  position: relative;
  overflow: hidden;
  background: var(--brand-grad);
  color: var(--text-on-primary);
  box-shadow: 0 2px 8px rgba(90, 120, 230, 0.25);
}
/* hover 光泽扫过（transform-only，炫酷但克制） */
.soft-btn.primary::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 40%;
  background: linear-gradient(105deg, transparent, rgba(255, 255, 255, 0.35), transparent);
  transform: translateX(-120%) skewX(-12deg);
  pointer-events: none;
}
.soft-btn.primary:hover:not(:disabled) {
  filter: brightness(1.06);
  transform: translateY(-2px);
  box-shadow: 0 4px 14px rgba(90, 120, 230, 0.35);
}
.soft-btn.primary:hover:not(:disabled)::after {
  animation: shine-sweep 0.6s var(--ease-standard);
}
.soft-btn.primary:active:not(:disabled) {
  filter: brightness(0.97);
}

.soft-btn.outline {
  background: transparent;
  border-color: color-mix(in srgb, var(--primary-soft) 65%, transparent);
  color: var(--primary);
}
.soft-btn.outline:hover:not(:disabled) {
  background: var(--blue-gray);
  border-color: var(--primary-soft);
  color: var(--primary-hover);
}

.soft-btn.danger {
  background: var(--error-bg);
  color: var(--error);
}
.soft-btn.danger:hover:not(:disabled) {
  background: var(--error);
  color: var(--text-on-primary);
}

.soft-btn.ghost {
  background: transparent;
  color: var(--text-sub);
}
.soft-btn.ghost:hover:not(:disabled) {
  background: var(--blue-gray);
  color: var(--text-main);
}
</style>
