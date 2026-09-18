<template>
  <div class="bank-list-view">
    <!-- 顶部：搜索 + 筛选题库（快速定位）+ 导入按钮 -->
    <div class="toolbar glass">
      <div class="search">
        <Search :size="15" :stroke-width="1.8" />
        <input v-model="keyword" type="text" placeholder="搜索题库名称…" @keyup.enter="load" />
      </div>
      <el-select
        v-model="selectedBankId"
        placeholder="筛选题库"
        clearable
        filterable
        class="bank-filter"
        @change="onBankSelect"
      >
        <el-option v-for="b in bankStore.banks" :key="b.id" :value="b.id" :label="b.name" />
      </el-select>
      <SoftButton :icon="FileUp" @click="router.push('/banks/import')">导入题库</SoftButton>
    </div>

    <!-- 骨架占位（A2）：数据到达前显示微光骨架卡 -->
    <div v-if="loading" class="bank-grid">
      <div v-for="i in 3" :key="i" class="bank-skeleton glass">
        <div class="sk-head">
          <div class="skeleton sk-icon" />
          <div class="skeleton sk-line" style="width: 45%" />
        </div>
        <div class="skeleton sk-line" style="width: 70%" />
        <div class="skeleton sk-line" style="width: 50%" />
      </div>
    </div>

    <!-- 题库卡片网格（入场错落） -->
    <TransitionGroup v-else-if="bankStore.banks.length" name="list" tag="div" class="bank-grid" appear>
      <BankCard
        v-for="(b, i) in bankStore.banks"
        :key="b.id"
        :bank="b"
        :style="{ '--stagger': Math.min(i, 8) }"
        @click="goDetail(b.id, $event)"
        @delete="onDelete"
      />
    </TransitionGroup>

    <GlassCard v-else>
      <EmptyState scene="books" text="还没有题库，先导入一份复习资料吧" hint="支持 DOCX / PDF / TXT，AI 会自动识别题目">
        <SoftButton :icon="FileUp" @click="router.push('/banks/import')">去导入</SoftButton>
      </EmptyState>
    </GlassCard>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Search, FileUp } from 'lucide-vue-next'
import GlassCard from '@/components/base/GlassCard.vue'
import SoftButton from '@/components/base/SoftButton.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import BankCard from '@/components/business/BankCard.vue'
import { deleteBank } from '@/api/bank'
import { useBankStore } from '@/stores/bank'
import { withViewTransition } from '@/utils/viewTransition'
import type { Bank } from '@/types'

const router = useRouter()
const route = useRoute()
const bankStore = useBankStore()

const keyword = ref((route.query.keyword as string) ?? '')
const selectedBankId = ref<number | undefined>(undefined)
const loading = ref(true)

function load() {
  // store 内部已 catch：失败静默显示空态
  loading.value = true
  void Promise.resolve(
    bankStore.fetchBanks({ keyword: keyword.value || undefined }, true)
  ).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  load()
})

function onBankSelect(id: number | undefined) {
  if (id == null) return
  router.push(`/banks/${id}`)
}

/* A6 共享元素转场：卡片以自身为圆心漾开成详情页（Chromium），其余环境直接跳转 */
function goDetail(id: number, e?: MouseEvent) {
  withViewTransition(
    () => router.push(`/banks/${id}`),
    e ? { x: e.clientX, y: e.clientY } : undefined
  )
}

async function onDelete(bank: Bank) {
  try {
    await ElMessageBox.confirm(
      `将级联删除题库「${bank.name}」及其章节、题目、刷题记录（AI 聊天会话会保留）。此操作不可恢复。`,
      '删除题库',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // 用户取消
  }
  try {
    await deleteBank(bank.id)
    bankStore.removeBank(bank.id)
  } catch {
    // 删除失败提示已由 request 层给出
  }
}
</script>

<style scoped>
/* 骨架卡（A2）：与 BankCard 同尺寸的占位 */
.bank-skeleton {
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.sk-head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.sk-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}
.sk-line {
  height: 14px;
}

.bank-list-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}
.toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-xl);
}
.search {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--text-muted);
  min-width: 0;
}
.search input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  color: var(--text-main);
  min-width: 0;
}
.search input::placeholder {
  color: var(--text-muted);
}
.bank-filter {
  width: 220px;
}
.bank-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-5);
}
</style>
