<template>
  <div class="question-list">
    <!-- 筛选条：题型 + 题干关键字（章节筛选由父页面的章节列表联动，不在这里重复放一份状态） -->
    <div class="q-toolbar">
      <div class="q-search">
        <Search :size="15" :stroke-width="1.8" />
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索题干关键字，回车确认…"
          @keyup.enter="applyFilter"
        />
      </div>
      <el-select
        v-model="type"
        placeholder="全部题型"
        clearable
        class="q-type-filter"
        @change="applyFilter"
      >
        <el-option v-for="t in TYPE_OPTIONS" :key="t.value" :value="t.value" :label="t.label" />
      </el-select>
    </div>

    <!-- 加载骨架 -->
    <div v-if="loading" class="q-skeleton-wrap">
      <div v-for="i in 3" :key="i" class="q-skeleton glass">
        <div class="skeleton sk-line" style="width: 30%" />
        <div class="skeleton sk-line" style="width: 85%" />
        <div class="skeleton sk-line" style="width: 60%" />
      </div>
    </div>

    <!-- 题目卡片 -->
    <template v-else-if="list.length">
      <div v-for="(q, i) in list" :key="q.id" class="q-item glass">
        <div class="q-head">
          <span class="q-index">第 {{ (page - 1) * pageSize + i + 1 }} 题</span>
          <SoftTag :variant="TYPE_META[q.type]?.variant ?? 'blue-gray'">
            {{ TYPE_META[q.type]?.label ?? q.type }}
          </SoftTag>
          <SoftTag v-if="q.chapterName" variant="blue-gray">{{ q.chapterName }}</SoftTag>
        </div>

        <QuestionStem :stem="q.stem" :knowledge-points="q.knowledgePoints ?? []" />

        <!-- 选项：只读展示，正确项染薄荷绿 -->
        <div v-if="q.options?.length" class="q-options">
          <div
            v-for="opt in q.options"
            :key="opt.key"
            class="q-option"
            :class="{ 'is-answer': isAnswerOption(q, opt.key) }"
          >
            <span class="opt-key">{{ opt.key }}</span>
            <span class="opt-text">{{ opt.text }}</span>
          </div>
        </div>

        <div class="q-answer">
          <span class="answer-label">答案</span>
          <span class="answer-text">{{ answerText(q) }}</span>
        </div>

        <div v-if="q.analysis" class="q-analysis">
          <MarkdownView :source="q.analysis" />
        </div>
      </div>

      <div class="q-pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :current-page="page"
          :page-size="pageSize"
          @current-change="onPageChange"
        />
      </div>
    </template>

    <!-- 空态：区分「库本身是空的」和「被筛没了」 -->
    <EmptyState
      v-else
      :text="hasFilter ? '没有符合条件的题目' : '这个题库还没有题目'"
      :hint="hasFilter ? '换个关键字，或点「查看全部」清掉筛选' : '回到列表重新导入一份资料吧'"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Search } from 'lucide-vue-next'
import SoftTag from '@/components/base/SoftTag.vue'
import EmptyState from '@/components/base/EmptyState.vue'
import QuestionStem from '@/components/business/QuestionStem.vue'
import MarkdownView from '@/components/base/MarkdownView.vue'
import { listQuestions } from '@/api/question'
import type { Question, QuestionType } from '@/types'

/**
 * 题目浏览列表：题型 / 关键字 / 分页，全部走后端 GET /api/banks/{bankId}/questions
 * （参数 chapterId / type / keyword / page / pageSize，与接口文档一致）。
 * 章节筛选的状态放在父页面（BankDetailView），通过 chapterId 传进来 —— 一份筛选状态只存一个地方，
 * 避免章节列表和题目列表各记一份、互相不一致。
 */
const props = withDefaults(
  defineProps<{
    bankId: number
    /** 当前章节筛选；undefined = 不筛（整库） */
    chapterId?: number
  }>(),
  { chapterId: undefined }
)

const PAGE_SIZE = 10

/** 题型展示元数据：文案 + 标签色（与题库详情页头部统计的配色保持一致） */
const TYPE_META: Record<
  QuestionType,
  { label: string; variant: 'sakura' | 'mint' | 'lavender' | 'blue-gray' | 'apricot' }
> = {
  SINGLE: { label: '单选题', variant: 'blue-gray' },
  MULTIPLE: { label: '多选题', variant: 'lavender' },
  TRUE_FALSE: { label: '判断题', variant: 'mint' },
  ESSAY: { label: '简答题', variant: 'sakura' },
  FILL_BLANK: { label: '填空题', variant: 'apricot' }
}
const TYPE_OPTIONS = (Object.keys(TYPE_META) as QuestionType[]).map((value) => ({
  value,
  label: TYPE_META[value].label
}))

const list = ref<Question[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(PAGE_SIZE)
const loading = ref(false)
const type = ref<QuestionType | undefined>(undefined)
const keyword = ref('')

const hasFilter = computed(() => !!props.chapterId || !!type.value || !!keyword.value.trim())

/** 真正干活的方法：带着当前所有筛选条件去后端要一页数据 */
async function load() {
  loading.value = true
  try {
    const data = await listQuestions(props.bankId, {
      chapterId: props.chapterId,
      type: type.value,
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      pageSize: pageSize.value
    })
    list.value = data.list ?? []
    total.value = data.total ?? 0
    // 页码/页大小以后端返回的为准（后端做了兜底和上限，避免两边算得不一样）
    page.value = data.page ?? page.value
    pageSize.value = data.pageSize ?? pageSize.value
  } catch {
    list.value = [] // 失败提示由 request 层统一弹出，这里只保证不留脏数据
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 改筛选条件 → 必须回到第 1 页，否则第 5 页 + 新条件很可能一条都查不到 */
function applyFilter() {
  page.value = 1
  void load()
}

function onPageChange(next: number) {
  page.value = next
  void load()
}

/** 换题库 / 换章节 → 回到第 1 页重新拉；immediate 保证首次进入就加载 */
watch(
  () => [props.bankId, props.chapterId],
  () => applyFilter(),
  { immediate: true }
)

/** 判断题答案是 A/B，直接显示字母没人看得懂，翻译成中文 */
function answerText(q: Question) {
  if (q.type === 'TRUE_FALSE') {
    if (q.answer === 'A') return '正确'
    if (q.answer === 'B') return '错误'
  }
  return q.answer || '—'
}

/** 单选/多选答案形如 "ACD"，包含该选项字母说明它是正确项 */
function isAnswerOption(q: Question, key: string) {
  if (q.type !== 'SINGLE' && q.type !== 'MULTIPLE') return false
  return (q.answer ?? '').toUpperCase().includes(key.toUpperCase())
}
</script>

<style scoped>
.question-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.q-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.q-search {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 8px var(--space-4);
  border-radius: var(--radius-md);
  background: var(--glass-bg);
  border: 1px solid var(--glass-line);
  color: var(--text-muted);
  min-width: 0;
  transition: var(--transition);
}
.q-search:focus-within {
  border-color: var(--primary-soft);
  box-shadow: var(--glow-primary);
}
.q-search input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  color: var(--text-main);
  min-width: 0;
}
.q-search input::placeholder {
  color: var(--text-muted);
}
.q-type-filter {
  width: 150px;
}

.q-skeleton-wrap,
.q-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.q-skeleton {
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.sk-line {
  height: 14px;
}

.q-item {
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-5);
  gap: var(--space-4);
}
.q-head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
}
.q-index {
  font-size: 13px;
  color: var(--text-muted);
}

.q-options {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.q-option {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: var(--glass-bg);
  line-height: 1.7;
  transition: var(--transition);
}
/* 正确项：薄荷绿淡底 + 实心圆标 */
.q-option.is-answer {
  border-color: var(--success);
  background: var(--mint);
}
.opt-key {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--blue-gray);
  color: var(--text-sub);
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.q-option.is-answer .opt-key {
  background: var(--success);
  color: var(--text-on-primary);
}
.opt-text {
  flex: 1;
}

.q-answer {
  display: flex;
  gap: var(--space-3);
  align-items: baseline;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  background: var(--blue-gray);
}
.answer-label {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--text-muted);
}
.answer-text {
  font-weight: 600;
  color: var(--success);
  word-break: break-all;
}

.q-analysis {
  font-size: 14px;
  color: var(--text-sub);
}

.q-pager {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--space-2);
}
</style>
