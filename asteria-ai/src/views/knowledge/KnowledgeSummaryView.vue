<template>
  <div class="knowledge-view">
    <!-- 选择器：题库（必选） -->
    <GlassCard class="selector-card">
      <div class="selector-row">
        <div class="field">
          <label>题库</label>
          <el-select v-model="bankId" placeholder="选择题库" class="field-select" @change="onBankChange">
            <el-option v-for="b in bankStore.banks" :key="b.id" :value="b.id" :label="b.name" />
          </el-select>
        </div>
        <SoftButton :icon="WandSparkles" :disabled="!bankId || generating" @click="generate(false)">
          生成总结
        </SoftButton>
      </div>
      <p class="selector-hint text-muted">选择题库后即可生成整库复习要点；已生成过的题库会直接展示缓存结果。</p>
      <p class="selector-hint text-muted">提示：每次生成最多通读题库的前 300 道题，总结以这 300 道为依据。</p>
    </GlassCard>

    <!-- 生成中 -->
    <GlassCard v-if="generating" class="loading-card">
      <BreathingLoader text="Asteria 正在通读题目，提炼复习要点…" />
    </GlassCard>

    <!-- 结果 -->
    <template v-else-if="summary">
      <div class="summary-meta">
        <SoftTag variant="blue-gray">{{ summary.bankName }}</SoftTag>
        <span class="text-muted meta-time">生成于 {{ formatDateTime(summary.generatedAt) }}</span>
        <span v-if="summary.fromCache" class="text-muted meta-time">（缓存结果）</span>
        <SoftButton variant="ghost" :icon="RefreshCw" :disabled="generating" @click="generate(true)">
          重新生成
        </SoftButton>
        <span class="text-muted meta-time">易错点按错题本生成，有了新错题要点「重新生成」才会更新</span>
      </div>
      <div class="summary-grid">
        <SummarySection title="核心亮点" :icon="Sparkles" :items="summary.highlights" icon-bg="var(--sakura)" />
        <SummarySection title="关键要点" :icon="Pin" :items="summary.keyPoints" icon-bg="var(--blue-gray)" />
        <SummarySection title="热门考点" :icon="Flame" :items="summary.hotTopics" icon-bg="var(--apricot)" />
        <SummarySection title="易错点" :icon="TriangleAlert" :items="summary.easyMistakes" icon-bg="var(--lavender)" />
        <SummarySection title="复习建议" :icon="Sprout" :items="summary.studySuggestions" icon-bg="var(--mint)" />
      </div>
    </template>

    <!-- 空态 -->
    <GlassCard v-else>
      <EmptyState scene="sprout" text="还没有生成知识点总结" hint="选择题库，让 Asteria 帮你提炼整库复习要点" />
    </GlassCard>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { WandSparkles, RefreshCw, Sparkles, Pin, Flame, TriangleAlert, Sprout } from 'lucide-vue-next'
import GlassCard from '@/components/base/GlassCard.vue'
import SoftButton from '@/components/base/SoftButton.vue'
import SoftTag from '@/components/base/SoftTag.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import BreathingLoader from '@/components/base/BreathingLoader.vue'
import SummarySection from '@/components/business/SummarySection.vue'
import { getSummary, generateSummary } from '@/api/knowledge'
import { useBankStore } from '@/stores/bank'
import { formatDateTime } from '@/utils/format'
import type { KnowledgeSummary } from '@/types'

const bankStore = useBankStore()

const bankId = ref<number | undefined>(undefined)
const summary = ref<KnowledgeSummary | null>(null)
const generating = ref(false)

onMounted(() => {
  bankStore.fetchBanks()
})

async function onBankChange() {
  summary.value = null
  if (bankId.value == null) return
  await loadCached()
}

/** 该题库已生成过总结则直接展示缓存 */
async function loadCached() {
  if (bankId.value == null) return
  try {
    summary.value = await getSummary(bankId.value)
  } catch {
    summary.value = null
  }
}

async function generate(force: boolean) {
  if (bankId.value == null || generating.value) return
  generating.value = true
  try {
    summary.value = await generateSummary({ bankId: bankId.value, force })
  } catch {
    ElMessage.error('生成失败，请稍后再试')
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.knowledge-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}
.selector-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.selector-row {
  display: flex;
  align-items: flex-end;
  gap: var(--space-4);
  flex-wrap: wrap;
}
.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}
.field label {
  font-size: 13px;
  color: var(--text-muted);
}
.field-select {
  width: 240px;
}
.selector-hint {
  font-size: 12px;
}
.loading-card {
  display: flex;
  justify-content: center;
  padding: var(--space-7);
}
.summary-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
}
.meta-time {
  font-size: 12px;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: var(--space-5);
}
</style>
