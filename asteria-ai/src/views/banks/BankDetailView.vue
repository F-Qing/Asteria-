<template>
  <div class="bank-detail-view">
    <template v-if="bank">
      <!-- 头部：题库名、题型统计、开始刷题（A6：与列表卡片共享元素配对） -->
      <GlassCard class="head-card" :style="`view-transition-name: bank-${bank.id}`">
        <div class="head-row">
          <div class="head-info">
            <div class="head-title">
              <h1>{{ bank.name }}</h1>
            </div>
            <div class="head-meta">
              <span>{{ bank.questionCount }} 题 · {{ bank.chapterCount }} 章</span>
              <span class="text-muted">{{ bank.fileName }} · {{ formatDate(bank.createdAt) }} 导入</span>
            </div>
            <div class="head-tags">
              <SoftTag v-if="bank.singleCount" variant="blue-gray">单选 {{ bank.singleCount }}</SoftTag>
              <SoftTag v-if="bank.multipleCount" variant="lavender">多选 {{ bank.multipleCount }}</SoftTag>
              <SoftTag v-if="bank.trueFalseCount" variant="mint">判断 {{ bank.trueFalseCount }}</SoftTag>
              <SoftTag v-if="bank.essayCount" variant="sakura">简答 {{ bank.essayCount }}</SoftTag>
              <SoftTag v-if="bank.fillBlankCount" variant="apricot">填空 {{ bank.fillBlankCount }}</SoftTag>
            </div>
          </div>
          <SoftButton :icon="Play" @click="configVisible = true">开始刷题</SoftButton>
        </div>
      </GlassCard>

      <div class="detail-grid">
        <GlassCard title="章节列表" class="chapters-card">
          <ChapterList
            :chapters="bank.chapters"
            selectable
            :selected-id="chapterId"
            @select="onChapterSelect"
          />
          <p v-if="bank.chapters.length" class="chapters-hint text-muted">
            点击章节可筛选下方题目，再点一次取消
          </p>
        </GlassCard>
        <WrongStatsPanel :bank-id="bank.id" :stats="wrongStats" />
      </div>

      <!-- 题目浏览：点章节 / 选题型 / 搜关键字 → 立刻刷新这一块（只读展示，含答案与解析） -->
      <div ref="questionsSectionRef">
        <GlassCard title="题目列表">
          <template #extra>
            <div class="questions-extra">
              <span v-if="selectedChapter" class="text-muted">已筛：{{ selectedChapter.name }}</span>
              <SoftButton v-if="chapterId != null" variant="ghost" :icon="X" @click="clearChapter">
                查看全部
              </SoftButton>
            </div>
          </template>
          <QuestionList :bank-id="bank.id" :chapter-id="chapterId" />
        </GlassCard>
      </div>

      <PracticeConfigDialog v-model="configVisible" :bank-id="bank.id" :chapters="bank.chapters" />
    </template>

    <!-- API 失败 / 题库不存在 -->
    <GlassCard v-else>
      <EmptyState scene="compass" text="题库走丢了" hint="题库不存在，或后端服务尚未启动">
        <SoftButton variant="outline" :icon="ArrowLeft" @click="router.push('/banks')">返回题库列表</SoftButton>
      </EmptyState>
    </GlassCard>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Play, ArrowLeft, X } from 'lucide-vue-next'
import GlassCard from '@/components/base/GlassCard.vue'
import SoftButton from '@/components/base/SoftButton.vue'
import SoftTag from '@/components/base/SoftTag.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import ChapterList from '@/components/business/ChapterList.vue'
import QuestionList from '@/components/business/QuestionList.vue'
import WrongStatsPanel from '@/components/business/WrongStatsPanel.vue'
import PracticeConfigDialog from '@/components/business/PracticeConfigDialog.vue'
import { getBank } from '@/api/bank'
import { getWrongStats } from '@/api/practice'
import { formatDate } from '@/utils/format'
import type { BankDetail, WrongStats } from '@/types'

const route = useRoute()
const router = useRouter()

const bank = ref<BankDetail | null>(null)
const wrongStats = ref<WrongStats | null>(null)
const configVisible = ref(false)

/** 题目列表的章节筛选（undefined = 整库）；只存这一份状态，章节列表和题目列表都读它 */
const chapterId = ref<number | undefined>(undefined)
const questionsSectionRef = ref<HTMLElement | null>(null)

const selectedChapter = computed(
  () => bank.value?.chapters.find((c) => c.id === chapterId.value) ?? null
)

onMounted(async () => {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) return
  try {
    bank.value = await getBank(id)
  } catch {
    bank.value = null // 静默失败 → 空态
    return
  }
  try {
    wrongStats.value = await getWrongStats(id)
  } catch {
    wrongStats.value = null // 错题统计失败不影响主内容（面板显示空态）
  }
})

/** 点章节：没选中 → 选中并滚到题目列表；已选中 → 再点一次取消（同一个按钮开关） */
function onChapterSelect(id: number) {
  chapterId.value = chapterId.value === id ? undefined : id
  if (chapterId.value == null) return
  // 等 DOM 更新完再滚，否则题目列表还停在旧高度，滚动位置会偏
  void nextTick(() => {
    questionsSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

function clearChapter() {
  chapterId.value = undefined
}
</script>

<style scoped>
.bank-detail-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}
.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-5);
}
.head-title {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
  margin-bottom: var(--space-2);
}
.head-meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-size: 14px;
  color: var(--text-sub);
  margin-bottom: var(--space-3);
}
.head-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: var(--space-5);
  align-items: start;
}
.chapters-hint {
  margin-top: var(--space-3);
  font-size: 13px;
}
.questions-extra {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: 13px;
}
@media (max-width: 1024px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
