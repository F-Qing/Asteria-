<template>
  <GlassCard title="错题统计">
    <template #extra>
      <SoftButton variant="outline" :icon="RotateCcw" :disabled="!stats || stats.wrongTotal === 0" @click="rebrush">
        错题重刷
      </SoftButton>
    </template>

    <div v-if="stats && stats.wrongTotal > 0" class="wrong-stats">
      <div class="wrong-total">
        <span class="wrong-num">{{ stats.wrongTotal }}</span>
        <span class="wrong-label">道错题待攻克</span>
      </div>
      <div class="wrong-dim">
        <div class="dim-block">
          <div class="dim-title">按题型</div>
          <div class="dim-tags">
            <SoftTag v-for="t in stats.byType" :key="t.type" variant="sakura">{{ typeLabel(t.type) }} {{ t.count }}</SoftTag>
          </div>
        </div>
        <div class="dim-block">
          <div class="dim-title">按章节</div>
          <div class="dim-tags">
            <SoftTag v-for="c in stats.byChapter" :key="c.chapterId" variant="lavender">{{ c.chapterName }} {{ c.count }}</SoftTag>
          </div>
        </div>
      </div>
    </div>

    <EmptyState v-else scene="trophy" text="还没有错题，继续保持" hint="答错的题目会自动收录到这里" />
  </GlassCard>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RotateCcw } from 'lucide-vue-next'
import GlassCard from '@/components/base/GlassCard.vue'
import SoftButton from '@/components/base/SoftButton.vue'
import SoftTag from '@/components/base/SoftTag.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import { createWrongSession } from '@/api/practice'
import type { QuestionType, WrongStats } from '@/types'

/** 错题统计面板 + 错题重刷入口 */
const props = withDefaults(
  defineProps<{
    bankId: number
    stats?: WrongStats | null
  }>(),
  { stats: null }
)

const router = useRouter()

function typeLabel(t: QuestionType) {
  return { SINGLE: '单选', MULTIPLE: '多选', TRUE_FALSE: '判断', ESSAY: '简答', FILL_BLANK: '填空' }[t]
}

async function rebrush() {
  try {
    const session = await createWrongSession(props.bankId, 'RANDOM')
    router.push(`/practice/${session.id}`)
  } catch {
    ElMessage.warning('暂时无法创建错题会话，请确认后端已启动')
  }
}
</script>

<style scoped>
.wrong-total {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}
.wrong-num {
  font-size: 28px;
  font-weight: 700;
  color: var(--error);
}
.wrong-label {
  font-size: 13px;
  color: var(--text-muted);
}
.wrong-dim {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.dim-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-sub);
  margin-bottom: var(--space-2);
}
.dim-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}
</style>
