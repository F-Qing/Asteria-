<template>
  <el-dialog v-model="visible" title="开始刷题" width="440px" append-to-body>
    <div class="config-form">
      <div class="form-item">
        <label class="form-label">刷题模式</label>
        <div class="choice-row">
          <button
            v-for="m in modes"
            :key="m.value"
            class="choice-chip"
            :class="{ active: form.mode === m.value }"
            @click="form.mode = m.value"
          >
            {{ m.label }}
          </button>
        </div>
      </div>

      <div class="form-item">
        <label class="form-label">题型</label>
        <div class="choice-row">
          <button
            v-for="t in questionTypes"
            :key="t.value"
            class="choice-chip"
            :class="{ active: form.questionType === t.value }"
            @click="form.questionType = t.value"
          >
            {{ t.label }}
          </button>
        </div>
      </div>

      <div class="form-item">
        <label class="form-label">章节</label>
        <el-select v-model="form.chapterId" placeholder="全部章节" clearable class="chapter-select">
          <el-option :value="null" label="全部章节" />
          <el-option v-for="ch in chapters" :key="ch.id" :value="ch.id" :label="`${ch.name}（${ch.questionCount} 题）`" />
        </el-select>
      </div>
    </div>

    <template #footer>
      <SoftButton variant="ghost" @click="visible = false">取消</SoftButton>
      <SoftButton :icon="Play" :loading="creating" @click="start">开始</SoftButton>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Play } from 'lucide-vue-next'
import { createSession } from '@/api/practice'
import SoftButton from '@/components/base/SoftButton.vue'
import type { Chapter, PracticeMode, QuestionTypeWithAll } from '@/types'

/** 刷题配置对话框（模式/题型含 ALL/章节） */
const props = withDefaults(
  defineProps<{
    modelValue: boolean
    bankId: number
    chapters?: Chapter[]
  }>(),
  { chapters: () => [] }
)

const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const router = useRouter()
const creating = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const modes: { value: PracticeMode; label: string }[] = [
  { value: 'SEQUENTIAL', label: '顺序练习' },
  { value: 'RANDOM', label: '随机练习' }
]
const questionTypes: { value: QuestionTypeWithAll; label: string }[] = [
  { value: 'ALL', label: 'ALL 混合' },
  { value: 'SINGLE', label: '单选' },
  { value: 'MULTIPLE', label: '多选' },
  { value: 'TRUE_FALSE', label: '判断' },
  { value: 'ESSAY', label: '简答' },
  { value: 'FILL_BLANK', label: '填空' }
]

const form = reactive<{ mode: PracticeMode; questionType: QuestionTypeWithAll; chapterId: number | null }>({
  mode: 'SEQUENTIAL',
  questionType: 'ALL',
  chapterId: null
})

async function start() {
  if (creating.value) return
  creating.value = true
  try {
    const session = await createSession({
      bankId: props.bankId,
      mode: form.mode,
      questionType: form.questionType,
      chapterId: form.chapterId
    })
    visible.value = false
    router.push(`/practice/${session.id}`)
  } catch {
    // 错误提示已由 request 层给出（后端未就绪时静默失败）
    ElMessage.warning('暂时无法创建刷题会话，请确认后端已启动')
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.config-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}
.form-label {
  display: block;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: var(--space-2);
}
.choice-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}
.choice-chip {
  padding: 6px var(--space-4);
  border-radius: var(--radius-pill);
  border: 1.5px solid var(--glass-border);
  background: var(--glass-bg);
  color: var(--text-sub);
  font-size: 13px;
  transition: var(--transition);
}
.choice-chip:hover {
  border-color: var(--primary-soft);
}
.choice-chip.active {
  border-color: var(--primary-soft);
  background: var(--blue-gray);
  color: var(--text-main);
  font-weight: 600;
}
.chapter-select {
  width: 100%;
}
</style>
