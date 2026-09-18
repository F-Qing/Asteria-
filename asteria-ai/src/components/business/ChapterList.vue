<template>
  <div class="chapter-list">
    <div
      v-for="ch in chapters"
      :key="ch.id"
      class="chapter-item"
      :class="{ clickable: selectable, active: selectable && selectedId === ch.id }"
      @click="onClick(ch.id)"
    >
      <div class="chapter-info">
        <span class="chapter-name">{{ ch.name }}</span>
        <span class="chapter-count">{{ ch.questionCount }} 题</span>
      </div>
      <!-- 掌握度：后端提供数据时才渲染，不显示恒 0% 的假进度条 -->
      <SoftProgress v-if="hasMastery" class="chapter-progress" :value="masteryOf(ch.id)" show-text />
    </div>
    <EmptyState v-if="!chapters.length" :icon="BookOpen" text="暂无章节信息" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { BookOpen } from 'lucide-vue-next'
import SoftProgress from '@/components/base/SoftProgress.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import type { Chapter } from '@/types'

/**
 * 章节列表（含题数；掌握度由后端提供，无数据时不渲染进度条）
 * selectable = true 时章节可点，点击抛 select(chapterId)，由父组件决定「选中 / 取消选中」。
 */
const props = withDefaults(
  defineProps<{
    chapters: Chapter[]
    /** chapterId → 0~1 掌握度（可选） */
    mastery?: Record<number, number>
    /** 是否可点选（默认 false，保持原有只读用法不变） */
    selectable?: boolean
    /** 当前选中的章节 id（受控，由父组件传入） */
    selectedId?: number
  }>(),
  { mastery: () => ({}), selectable: false, selectedId: undefined }
)

const emit = defineEmits<{ (e: 'select', chapterId: number): void }>()

const hasMastery = computed(() => Object.keys(props.mastery).length > 0)

function masteryOf(id: number) {
  return props.mastery[id] ?? 0
}

function onClick(id: number) {
  if (props.selectable) emit('select', id)
}
</script>

<style scoped>
.chapter-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.chapter-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  transition: var(--transition);
}
.chapter-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.chapter-item.clickable {
  cursor: pointer;
}
/* 选中态：品牌色描边 + 淡底（hover 是抬升，选中是染色，两种反馈不混淆） */
.chapter-item.active {
  border-color: var(--primary-soft);
  background: var(--blue-gray);
}
.chapter-item.active .chapter-name {
  color: var(--primary);
}
.chapter-info {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}
.chapter-name {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chapter-count {
  font-size: 13px;
  color: var(--text-muted);
}
.chapter-progress {
  flex: 1;
}
</style>
