<template>
  <!-- A6：view-transition-name 与详情页头部配对，跳转时卡片 morph 成页头 -->
  <GlassCard hoverable class="bank-card" :style="`view-transition-name: bank-${bank.id}`">
    <div class="bank-head">
      <div class="bank-icon">
        <BookOpen :size="20" :stroke-width="1.8" />
      </div>
      <div class="bank-meta">
        <h3 class="bank-name" :title="bank.name">{{ bank.name }}</h3>
      </div>
      <button class="bank-delete" title="删除题库" @click.stop="$emit('delete', bank)">
        <Trash2 :size="15" :stroke-width="1.8" />
      </button>
    </div>

    <div class="bank-tags">
      <SoftTag v-if="bank.singleCount" variant="blue-gray">单选 {{ bank.singleCount }}</SoftTag>
      <SoftTag v-if="bank.multipleCount" variant="lavender">多选 {{ bank.multipleCount }}</SoftTag>
      <SoftTag v-if="bank.trueFalseCount" variant="mint">判断 {{ bank.trueFalseCount }}</SoftTag>
      <SoftTag v-if="bank.essayCount" variant="sakura">简答 {{ bank.essayCount }}</SoftTag>
      <SoftTag v-if="bank.fillBlankCount" variant="apricot">填空 {{ bank.fillBlankCount }}</SoftTag>
    </div>

    <div class="bank-foot">
      <span class="bank-foot-stat"><FolderOpen :size="14" :stroke-width="1.8" /> {{ bank.chapterCount }} 章 · {{ bank.questionCount }} 题</span>
      <span class="bank-time">{{ formatRelative(bank.createdAt) }}导入</span>
    </div>
  </GlassCard>
</template>

<script setup lang="ts">
import { BookOpen, Trash2, FolderOpen } from 'lucide-vue-next'
import GlassCard from '@/components/base/GlassCard.vue'
import SoftTag from '@/components/base/SoftTag.vue'
import { formatRelative } from '@/utils/format'
import type { Bank } from '@/types'

/** 题库卡片（图标/统计/进度） */
defineProps<{ bank: Bank }>()
defineEmits<{ (e: 'delete', bank: Bank): void }>()
</script>

<style scoped>
.bank-card {
  cursor: pointer;
}
.bank-head {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}
.bank-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--blue-gray);
  color: var(--primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.bank-meta {
  flex: 1;
  min-width: 0;
}
.bank-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bank-delete {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
  flex-shrink: 0;
}
.bank-delete:hover {
  background: var(--error-bg);
  color: var(--error);
}
.bank-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin: var(--space-4) 0;
}
.bank-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-muted);
}
.bank-foot-stat {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}
</style>
