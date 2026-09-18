<template>
  <div
    class="option-item"
    :class="[status, { clickable: !disabled, shake: status === 'wrong' }]"
    @click="onClick"
  >
    <span class="option-key">{{ optionKey }}</span>
    <span class="option-text">{{ text }}</span>
    <!-- 答对：对勾笔画弹性画出（A7） -->
    <svg
      v-if="status === 'correct'"
      class="option-mark check-draw"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M5 12.5l4.2 4.2L19 7"
        stroke="currentColor"
        stroke-width="2.4"
        stroke-linecap="round"
        stroke-linejoin="round"
        pathLength="1"
      />
    </svg>
    <X v-else-if="status === 'wrong'" class="option-mark" :size="16" :stroke-width="2.2" />
    <!-- 答对小星迸发 -->
    <span v-if="status === 'correct'" class="burst" aria-hidden="true">
      <i>✦</i><i>✦</i><i>✦</i>
    </span>
  </div>
</template>

<script setup lang="ts">
import { X } from 'lucide-vue-next'
/**
 * 单选/多选/判断选项卡。
 * status：idle 默认 / selected 选中 / correct 答对（薄荷绿晕染）/ wrong 答错（柔和 shake）/ missed 漏选正确答案
 */
const props = withDefaults(
  defineProps<{
    optionKey: string
    text: string
    status?: 'idle' | 'selected' | 'correct' | 'wrong' | 'missed'
    disabled?: boolean
  }>(),
  { status: 'idle', disabled: false }
)

const emit = defineEmits<{ (e: 'select'): void }>()

function onClick() {
  if (!props.disabled) emit('select')
}
</script>

<style scoped>
.option-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  border: 1.5px solid var(--glass-border);
  background: var(--glass-bg);
  transition: var(--transition);
}
.option-item.clickable {
  cursor: pointer;
}
.option-item.clickable:hover {
  border-color: var(--primary-soft);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}
.option-key {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--blue-gray);
  color: var(--text-sub);
  font-weight: 600;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: var(--transition);
}
.option-text {
  flex: 1;
  line-height: 1.7;
}
.option-mark {
  flex-shrink: 0;
}

.option-item.selected {
  border-color: var(--primary-soft);
  background: var(--blue-gray);
}
.option-item.selected .option-key {
  background: var(--primary-soft);
  color: var(--text-on-primary);
}

/* 答对：薄荷绿晕染 + 轻微 scale(1.01) 回弹 + 光晕扩散 */
.option-item.correct {
  border-color: var(--success);
  background: var(--mint);
  animation: correct-pop 0.4s var(--ease-spring), correct-glow 0.7s var(--ease-standard);
}
.option-item.correct .option-key {
  background: var(--success);
  color: var(--text-on-primary);
}
.option-item.correct .option-mark {
  color: var(--success);
}
/* 对勾笔画画出（A7） */
.check-draw {
  width: 18px;
  height: 18px;
}
.check-draw path {
  stroke-dasharray: 1;
  stroke-dashoffset: 1;
  animation: check-draw 0.45s var(--ease-standard) 0.08s forwards;
}
@keyframes check-draw {
  to {
    stroke-dashoffset: 0;
  }
}
@keyframes correct-glow {
  0% {
    box-shadow: 0 0 0 0 color-mix(in srgb, var(--success) 40%, transparent);
  }
  100% {
    box-shadow: 0 0 0 16px transparent;
  }
}
/* 小星迸发：三颗 ✦ 从对勾处向外飞散淡出 */
.burst {
  position: absolute;
  right: 30px;
  top: 50%;
  width: 0;
  height: 0;
  pointer-events: none;
}
.burst i {
  position: absolute;
  font-style: normal;
  font-size: 11px;
  color: var(--success);
  opacity: 0;
  animation: burst-fly 0.65s var(--ease-standard) 0.12s forwards;
}
.burst i:nth-child(1) {
  --bx: -26px;
  --by: -20px;
}
.burst i:nth-child(2) {
  --bx: -6px;
  --by: -30px;
  animation-delay: 0.2s;
}
.burst i:nth-child(3) {
  --bx: 18px;
  --by: -16px;
  animation-delay: 0.16s;
}
@keyframes burst-fly {
  0% {
    opacity: 0;
    transform: translate(0, 0) scale(0.5);
  }
  25% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translate(var(--bx), var(--by)) scale(1.05);
  }
}

.option-item.wrong {
  border-color: var(--error);
  background: var(--error-bg);
}
.option-item.wrong .option-key {
  background: var(--error);
  color: var(--text-on-primary);
}
.option-item.wrong .option-mark {
  color: var(--error);
}

.option-item.missed {
  border-color: var(--success);
  border-style: dashed;
}
.option-item.missed .option-key {
  background: var(--mint);
  color: var(--success);
}

@keyframes correct-pop {
  0% { transform: scale(1); }
  50% { transform: scale(1.01); }
  100% { transform: scale(1); }
}
</style>
