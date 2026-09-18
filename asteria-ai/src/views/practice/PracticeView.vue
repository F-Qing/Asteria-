<template>
  <div class="practice-view">
    <template v-if="session">
      <!-- 完成态：得分环 + 正确率 + 错题列表 -->
      <GlassCard v-if="finished" class="finish-card">
        <div class="score-ring" :style="ringStyle">
          <span class="score-num">{{ result?.correctCount ?? session.correctCount }}</span>
          <span class="score-total">/ {{ result?.totalCount ?? session.totalCount }}</span>
        </div>
        <h2>本次练习完成 🎉</h2>
        <p class="text-sub">正确率 {{ formatPercent(accuracyRatio) }}</p>

        <div v-if="result?.typeStats?.length" class="type-stats">
          <SoftTag v-for="t in result.typeStats" :key="t.type" variant="blue-gray">
            {{ typeLabel(t.type) }} {{ t.correct }}/{{ t.total }}
          </SoftTag>
        </div>

        <div v-if="result?.wrongQuestions?.length" class="wrong-list">
          <h3>错题回顾</h3>
          <div v-for="w in result.wrongQuestions" :key="w.questionId" class="wrong-item">
            <SoftTag variant="sakura">{{ typeLabel(w.type) }}</SoftTag>
            <MarkdownView class="wrong-stem" :source="w.stem" />
            <span class="wrong-answer text-muted">你的答案 {{ w.userAnswer || '（未答）' }} → 正确 {{ w.answer }}</span>
          </div>
        </div>

        <div class="finish-actions">
          <SoftButton variant="outline" :icon="ArrowLeft" @click="router.push(`/banks/${session.bankId}`)">返回题库</SoftButton>
          <SoftButton :icon="RotateCcw" @click="rebrushWrong">重刷错题</SoftButton>
        </div>
      </GlassCard>

      <template v-else-if="question">
        <!-- 顶部：进度 + 题型 -->
        <div class="practice-head">
          <span class="progress-text">第 {{ currentIndex + 1 }} / {{ session.questions.length }} 题</span>
          <SoftProgress class="progress-bar" :value="(currentIndex + 1) / session.questions.length" />
          <SoftTag :variant="tagVariant(question.type)">{{ typeLabel(question.type) }}</SoftTag>
        </div>

        <!-- 题干 -->
        <GlassCard class="stem-card">
          <QuestionStem :stem="question.stem" :knowledge-points="question.knowledgePoints" />
        </GlassCard>

        <!-- 作答区 -->
        <GlassCard class="answer-card">
          <!-- 判断题：正确/错误双大卡 -->
          <div v-if="question.type === 'TRUE_FALSE'" class="tf-row">
            <OptionItem
              v-for="opt in tfOptions"
              :key="opt.key"
              :option-key="opt.key"
              :text="opt.text"
              :status="optionStatus(opt.key)"
              :disabled="submitted"
              class="tf-card"
              @select="selectSingle(opt.key)"
            />
          </div>

          <!-- 单选 -->
          <div v-else-if="question.type === 'SINGLE'" class="option-list">
            <OptionItem
              v-for="opt in question.options ?? []"
              :key="opt.key"
              :option-key="opt.key"
              :text="opt.text"
              :status="optionStatus(opt.key)"
              :disabled="submitted"
              @select="selectSingle(opt.key)"
            />
          </div>

          <!-- 多选 -->
          <div v-else-if="question.type === 'MULTIPLE'" class="option-list">
            <OptionItem
              v-for="opt in question.options ?? []"
              :key="opt.key"
              :option-key="opt.key"
              :text="opt.text"
              :status="optionStatus(opt.key)"
              :disabled="submitted"
              @select="toggleMultiple(opt.key)"
            />
          </div>

          <!-- 填空：单输入框，多个空用「；」分隔 -->
          <template v-else-if="question.type === 'FILL_BLANK'">
            <EssayEditor
              v-model="blankAnswer"
              :disabled="submitted"
              :show-count="false"
              placeholder="填写答案，多个空用；分隔，例如：北京；上海"
            />
          </template>

          <!-- 简答 -->
          <template v-else>
            <EssayEditor v-model="essayText" :disabled="submitted" />
          </template>

          <!-- 提交按钮 -->
          <div v-if="!submitted" class="submit-row">
            <SoftButton :icon="Check" :loading="submitting" :disabled="!canSubmit" @click="submit">
              提交答案
            </SoftButton>
          </div>
        </GlassCard>

        <!-- 提交后：AI 解析面板 -->
        <AnswerAnalysis
          v-if="submitted && submitResult"
          :answer="submitResult.answer"
          :analysis="submitResult.analysis"
          :is-correct="submitResult.isCorrect"
          :is-essay="question.type === 'ESSAY'"
        />

        <!-- 简答自评（仅影响本地统计展示） -->
        <div v-if="submitted && question.type === 'ESSAY' && submitResult" class="self-eval glass">
          <span class="text-sub">对照参考答案，你觉得自己答得如何？</span>
          <div class="eval-btns">
            <SoftButton variant="outline" :disabled="selfEval !== null" @click="evalSelf(true)">我答对了</SoftButton>
            <SoftButton variant="outline" :disabled="selfEval !== null" @click="evalSelf(false)">我答错了</SoftButton>
            <SoftTag v-if="selfEval === true" variant="mint">已标记为答对</SoftTag>
            <SoftTag v-if="selfEval === false" variant="sakura">已标记为答错</SoftTag>
          </div>
        </div>

        <!-- 底部：上一题/下一题 + 收藏/标记 -->
        <div class="practice-foot">
          <SoftButton variant="ghost" :icon="ChevronLeft" :disabled="currentIndex === 0" @click="go(currentIndex - 1)">
            上一题
          </SoftButton>
          <div class="foot-actions">
            <button class="icon-btn" :class="{ active: isFavorite }" title="收藏（仅本地）" @click="practiceStore.toggleFavorite(question.id)">
              <Star :size="16" :stroke-width="1.8" :fill="isFavorite ? 'currentColor' : 'none'" />
            </button>
            <button class="icon-btn" :class="{ active: isMarked }" title="错题标记（仅本地）" @click="practiceStore.toggleMarked(question.id)">
              <Flag :size="16" :stroke-width="1.8" :fill="isMarked ? 'currentColor' : 'none'" />
            </button>
          </div>
          <SoftButton v-if="currentIndex < session.questions.length - 1" variant="ghost" @click="go(currentIndex + 1)">
            下一题 <ChevronRight :size="15" :stroke-width="1.8" />
          </SoftButton>
          <SoftButton v-else :icon="FlagTriangleRight" @click="finish">完成练习</SoftButton>
        </div>
      </template>
    </template>

    <!-- API 失败：空态 -->
    <GlassCard v-else>
      <EmptyState :icon="CloudOff" text="刷题会话加载失败" hint="会话不存在，或后端服务尚未启动">
        <SoftButton variant="outline" :icon="ArrowLeft" @click="router.push('/banks')">返回题库</SoftButton>
      </EmptyState>
    </GlassCard>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  RotateCcw,
  Check,
  ChevronLeft,
  ChevronRight,
  Star,
  Flag,
  FlagTriangleRight,
  CloudOff
} from 'lucide-vue-next'
import GlassCard from '@/components/base/GlassCard.vue'
import SoftButton from '@/components/base/SoftButton.vue'
import SoftTag from '@/components/base/SoftTag.vue'
import SoftProgress from '@/components/base/SoftProgress.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import MarkdownView from '@/components/base/MarkdownView.vue'
import QuestionStem from '@/components/business/QuestionStem.vue'
import OptionItem from '@/components/business/OptionItem.vue'
import EssayEditor from '@/components/business/EssayEditor.vue'
import AnswerAnalysis from '@/components/business/AnswerAnalysis.vue'
import { createWrongSession, getSessionResult } from '@/api/practice'
import { usePracticeStore } from '@/stores/practice'
import { formatPercent } from '@/utils/format'
import type { AnswerSubmitResult, PracticeQuestion, QuestionType, SessionResult } from '@/types'

const route = useRoute()
const router = useRouter()
const practiceStore = usePracticeStore()

const session = computed(() => practiceStore.session)
const currentIndex = ref(0)
const finished = ref(false)
const result = ref<SessionResult | null>(null)

/* ── 作答状态 ── */
const singleAnswer = ref('')
const multipleAnswers = ref<string[]>([])
const essayText = ref('')
const blankAnswer = ref('')
const submitted = ref(false)
const submitting = ref(false)
const submitResult = ref<AnswerSubmitResult | null>(null)
const selfEval = ref<boolean | null>(null)

const question = computed<PracticeQuestion | null>(() => session.value?.questions[currentIndex.value] ?? null)
const isFavorite = computed(() => question.value != null && practiceStore.favorites.includes(question.value.id))
const isMarked = computed(() => question.value != null && practiceStore.marked.includes(question.value.id))

const tfOptions = [
  { key: 'A', text: '正确' },
  { key: 'B', text: '错误' }
]

const submittedMap = computed(() => practiceStore.answers)

onMounted(async () => {
  const id = Number(route.params.sessionId)
  if (!Number.isFinite(id)) return
  const s = await practiceStore.fetchSession(id)
  if (s) {
    restoreForCurrent()
  }
})

watch(currentIndex, restoreForCurrent)

/** 切换题目时恢复该题作答状态（断点续答：records 仅有记录无答案，未提交的按未答处理） */
function restoreForCurrent() {
  const q = question.value
  if (!q) return
  const saved: AnswerSubmitResult | undefined = submittedMap.value[q.id]
  submitted.value = !!saved
  submitResult.value = saved ?? null
  selfEval.value = null
  if (saved) {
    if (q.type === 'MULTIPLE') multipleAnswers.value = saved.userAnswer.split('').filter(Boolean)
    else if (q.type === 'ESSAY') essayText.value = saved.userAnswer
    else if (q.type === 'FILL_BLANK') blankAnswer.value = saved.userAnswer
    else singleAnswer.value = saved.userAnswer
  } else {
    singleAnswer.value = ''
    multipleAnswers.value = []
    essayText.value = ''
    blankAnswer.value = ''
  }
}

/* ── 选择逻辑 ── */
function selectSingle(key: string) {
  singleAnswer.value = key
}
function toggleMultiple(key: string) {
  multipleAnswers.value = multipleAnswers.value.includes(key)
    ? multipleAnswers.value.filter((k) => k !== key)
    : [...multipleAnswers.value, key].sort()
}

const canSubmit = computed(() => {
  const q = question.value
  if (!q) return false
  if (q.type === 'MULTIPLE') return multipleAnswers.value.length > 0
  if (q.type === 'ESSAY') return essayText.value.trim().length > 0
  if (q.type === 'FILL_BLANK') return blankAnswer.value.trim().length > 0
  return singleAnswer.value !== ''
})

/* ── 提交后选项状态 ── */
function optionStatus(key: string): 'idle' | 'selected' | 'correct' | 'wrong' | 'missed' {
  const q = question.value
  if (!q) return 'idle'
  const picked = q.type === 'MULTIPLE' ? multipleAnswers.value.includes(key) : singleAnswer.value === key
  if (!submitted.value || !submitResult.value) return picked ? 'selected' : 'idle'
  const correctKeys = submitResult.value.answer.split('').filter(Boolean)
  const isCorrectKey = correctKeys.includes(key)
  if (picked && isCorrectKey) return 'correct'
  if (picked && !isCorrectKey) return 'wrong'
  if (!picked && isCorrectKey) return 'missed'
  return 'idle'
}

/* ── 提交 ── */
async function submit() {
  const q = question.value
  if (!q || submitting.value) return
  const userAnswer =
    q.type === 'MULTIPLE'
      ? multipleAnswers.value.join('')
      : q.type === 'ESSAY'
        ? essayText.value.trim()
        : q.type === 'FILL_BLANK'
          ? blankAnswer.value.trim()
          : singleAnswer.value
  submitting.value = true
  try {
    submitResult.value = await practiceStore.submit(q.id, userAnswer)
    submitted.value = true
  } catch {
    ElMessage.warning('提交失败，请确认后端已启动后再试')
  } finally {
    submitting.value = false
  }
}

function evalSelf(ok: boolean) {
  selfEval.value = ok // 仅本地展示，不影响后端统计
}

function go(idx: number) {
  currentIndex.value = idx
}

/* ── 完成 ── */
const accuracyLocal = computed(() => {
  const s = session.value
  if (!s || !s.totalCount) return 0
  return s.correctCount / s.totalCount
})

/* 后端 /result 返回的 accuracy 按接口文档是 0~100，这里统一成 0~1，
   再交给 formatPercent() 和得分环 —— 两处都只认 0~1 */
const accuracyRatio = computed(() =>
  result.value?.accuracy != null ? result.value.accuracy / 100 : accuracyLocal.value
)

const ringStyle = computed(() => {
  const acc = accuracyRatio.value * 100
  return {
    background: `conic-gradient(var(--success) ${acc}%, var(--blue-gray) ${acc}%)`
  }
})

async function finish() {
  finished.value = true
  const s = session.value
  if (!s) return
  try {
    result.value = await getSessionResult(s.id)
  } catch {
    result.value = null // 结果接口失败时展示本地计数
  }
}

async function rebrushWrong() {
  const s = session.value
  if (!s) return
  try {
    const ns = await createWrongSession(s.bankId, 'RANDOM')
    router.push(`/practice/${ns.id}`)
  } catch {
    ElMessage.warning('暂时无法创建错题会话，请确认后端已启动')
  }
}

/* ── 展示辅助 ── */
function typeLabel(t: QuestionType) {
  return { SINGLE: '单选', MULTIPLE: '多选', TRUE_FALSE: '判断', ESSAY: '简答', FILL_BLANK: '填空' }[t]
}
function tagVariant(t: QuestionType) {
  return { SINGLE: 'blue-gray', MULTIPLE: 'lavender', TRUE_FALSE: 'mint', ESSAY: 'sakura', FILL_BLANK: 'apricot' }[t] as
    | 'blue-gray'
    | 'lavender'
    | 'mint'
    | 'sakura'
    | 'apricot'
}
</script>

<style scoped>
.practice-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}
.practice-head {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}
.progress-text {
  font-size: 13px;
  color: var(--text-muted);
  white-space: nowrap;
}
.progress-bar {
  flex: 1;
}
.option-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.tf-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}
.tf-card {
  justify-content: center;
  padding: var(--space-5);
  font-size: 16px;
}
.submit-row {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-4);
}
.self-eval {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  border-radius: var(--radius-lg);
  flex-wrap: wrap;
}
.eval-btns {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.practice-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}
.foot-actions {
  display: flex;
  gap: var(--space-2);
}
.icon-btn {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
}
.icon-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}
.icon-btn.active {
  color: var(--warning);
}

/* ── 完成态 ── */
.finish-card {
  text-align: center;
  padding: var(--space-7) var(--space-5);
}
.score-ring {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  margin: 0 auto var(--space-4);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.score-ring::before {
  content: '';
  position: absolute;
  inset: 10px;
  border-radius: 50%;
  background: var(--bg-base);
}
.score-num,
.score-total {
  position: relative;
}
.score-num {
  font-size: 32px;
  font-weight: 700;
}
.score-total {
  color: var(--text-muted);
  margin-top: 10px;
}
.type-stats {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin: var(--space-4) 0;
}
.wrong-list {
  text-align: left;
  margin: var(--space-5) 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.wrong-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  background: var(--error-bg);
}
.wrong-stem {
  flex: 1;
  min-width: 0;
}
.wrong-answer {
  font-size: 13px;
  white-space: nowrap;
}
.finish-actions {
  display: flex;
  justify-content: center;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
</style>
