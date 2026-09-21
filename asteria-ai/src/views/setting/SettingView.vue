<template>
  <div class="setting-view">
    <!-- ── AI 服务配置（用户自带密钥，随请求头发送给本地后端转发调用） ── -->
    <GlassCard title="AI 服务配置">
      <!-- 供应商选择 -->
      <div class="provider-grid">
        <div
          v-for="p in AI_PROVIDERS"
          :key="p.key"
          class="select-card"
          :class="{ selected: provider === p.key }"
          @click="onProviderChange(p.key)"
        >
          <div class="select-card-name">{{ p.name }}</div>
          <CircleCheck v-if="provider === p.key" class="check" :size="18" :stroke-width="1.8" />
        </div>
      </div>

      <!-- 表单 -->
      <div class="form-area">
        <div class="form-field">
          <label class="field-label">
            API Key
            <SoftTag v-if="aiConfig.apiKey && !apiKeyInput" variant="mint" class="saved-tag">已保存</SoftTag>
          </label>
          <el-input
            v-model="apiKeyInput"
            type="password"
            show-password
            clearable
            :placeholder="aiConfig.apiKey ? '••••••••（已保存，重新输入可覆盖）' : '请输入 API Key'"
          />
        </div>
        <div class="form-field">
          <label class="field-label">Base URL（可选，留空使用供应商默认地址）</label>
          <el-input v-model="baseUrlInput" placeholder="https://..." clearable />
        </div>
        <div class="form-field">
          <label class="field-label">模型名称</label>
          <el-select
            v-model="modelInput"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入模型名"
            class="model-select"
          >
            <el-option v-for="m in modelSuggestions" :key="m" :value="m" :label="modelLabelOf(provider, m)" />
          </el-select>
          <p v-if="provider === 'deepseek'" class="field-hint text-muted">
            deepseek-flash = DeepSeek-V4.1-Flash（2026-09-10 发布，官方说改用它就是调最新 V4.1 Flash）；
            老的 deepseek-chat / deepseek-reasoner 已下线，请不要再选。
          </p>
        </div>

        <div class="form-actions">
          <SoftButton :icon="Save" @click="save">保存</SoftButton>
          <SoftButton variant="outline" :icon="Plug" :loading="testing" @click="testAndSave">测试并保存</SoftButton>
          <SoftButton variant="ghost" :icon="Eraser" @click="clearConfig">清除配置</SoftButton>
        </div>
        <p class="field-hint text-muted">
          密钥仅保存在本机 localStorage，随请求头发送给你自己部署的本地后端，由后端转发调用大模型。
        </p>
      </div>
    </GlassCard>

    <!-- ── 考试倒计时（数据保存到后端 MySQL） ── -->
    <GlassCard title="考试倒计时">
      <div class="exam-form">
        <el-input v-model="examName" placeholder="考试名称，如：数据库系统概论" class="exam-name-input" />
        <el-date-picker
          v-model="examDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="考试日期"
          class="exam-date-picker"
        />
        <SoftButton :icon="Plus" :disabled="!examName.trim() || !examDate" @click="submitExam">
          {{ editingExamId != null ? '保存修改' : '添加' }}
        </SoftButton>
        <SoftButton v-if="editingExamId != null" variant="ghost" @click="resetExamForm">取消</SoftButton>
      </div>

      <div v-if="examStore.exams.length" class="exam-list">
        <div v-for="exam in examStore.exams" :key="exam.id" class="exam-item">
          <span class="exam-dot" :class="levelOf(exam)" />
          <div class="exam-info">
            <span class="exam-name">{{ exam.name }}</span>
            <span class="exam-date text-muted">{{ exam.date }}</span>
          </div>
          <span class="exam-days">还有 <b>{{ exam.daysLeft }}</b> 天</span>
          <button class="exam-op" title="编辑" @click="startEditExam(exam)">
            <Pen :size="14" :stroke-width="1.8" />
          </button>
          <button class="exam-op danger" title="删除" @click="removeExam(exam)">
            <Trash2 :size="14" :stroke-width="1.8" />
          </button>
        </div>
      </div>
      <p v-else class="field-hint text-muted">还没有考试安排，添加后会显示在首页与侧边栏。</p>
    </GlassCard>

    <!-- ── 外观主题 ── -->
    <GlassCard title="外观主题">
      <div class="card-grid">
        <div
          v-for="t in themes"
          :key="t.key"
          class="select-card"
          :class="{ selected: app.theme === t.key }"
          @click="onThemeClick(t.key, $event)"
        >
          <div class="theme-preview" :class="t.key" />
          <div class="select-card-name">{{ t.name }}</div>
          <div class="select-card-desc text-muted">{{ t.desc }}</div>
          <CircleCheck v-if="app.theme === t.key" class="check" :size="18" :stroke-width="1.8" />
        </div>
      </div>
    </GlassCard>

    <!-- ── 关于 ── -->
    <GlassCard title="关于 Asteria AI">
      <div class="about-row">
        <span class="text-sub">版本</span>
        <span>v0.1.0（MVP）</span>
      </div>
      <div class="about-row">
        <span class="text-sub">GitHub</span>
        <!-- TODO: GitHub 仓库地址确定后替换 GITHUB_URL 占位符 -->
        <a v-if="GITHUB_URL" :href="GITHUB_URL" target="_blank" rel="noopener">{{ GITHUB_URL }}</a>
        <span v-else class="text-muted">待补充</span>
      </div>
    </GlassCard>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Save, Plug, Eraser, Plus, Pen, Trash2 } from 'lucide-vue-next'
import GlassCard from '@/components/base/GlassCard.vue'
import SoftButton from '@/components/base/SoftButton.vue'
import SoftTag from '@/components/base/SoftTag.vue'
import { AI_PROVIDERS, modelLabelOf } from '@/utils/aiConfig'
import { withViewTransition } from '@/utils/viewTransition'
import { useAiConfigStore } from '@/stores/aiConfig'
import { useExamStore } from '@/stores/exam'
import { useAppStore } from '@/stores/app'
import { testAiConfig } from '@/api/ai'
import type { ThemeMode } from '@/stores/app'
import type { ExamCountdown } from '@/types'

const app = useAppStore()
const aiConfig = useAiConfigStore()
const examStore = useExamStore()

/* A9 主题切换：新主题从点击处圆形漾开（View Transitions，不支持时直接切换） */
function onThemeClick(key: ThemeMode, e: MouseEvent) {
  if (app.theme === key) return
  withViewTransition(
    () => app.setTheme(key),
    { x: e.clientX, y: e.clientY }
  )
}

/* ── AI 服务配置 ── */
const provider = ref(aiConfig.provider)
// Key 不回显明文：输入框留空表示沿用已保存的 Key
const apiKeyInput = ref('')
const baseUrlInput = ref(aiConfig.baseUrl)
const modelInput = ref(aiConfig.model)
const testing = ref(false)

const modelSuggestions = computed(
  () => AI_PROVIDERS.find((p) => p.key === provider.value)?.models ?? []
)

function onProviderChange(key: string) {
  provider.value = key
  const preset = AI_PROVIDERS.find((p) => p.key === key)
  if (!preset) return
  // 自动填充官方默认地址（自定义供应商需手动填写）
  baseUrlInput.value = preset.baseUrl
  if (modelInput.value && preset.models.length && !preset.models.includes(modelInput.value)) {
    modelInput.value = ''
  }
}

/** 保存配置；返回是否成功。Key 输入框留空时沿用已保存的 Key。 */
function save(): boolean {
  const apiKey = apiKeyInput.value.trim() || aiConfig.apiKey
  if (!apiKey) {
    ElMessage.warning('请填写 API Key')
    return false
  }
  const model = modelInput.value.trim()
  if (!model) {
    ElMessage.warning('请填写模型名称')
    return false
  }
  aiConfig.provider = provider.value
  aiConfig.apiKey = apiKey
  aiConfig.baseUrl = baseUrlInput.value.trim()
  aiConfig.model = model
  aiConfig.save()
  apiKeyInput.value = ''
  ElMessage.success('AI 配置已保存')
  return true
}

/** 测试并保存：先存本地，再让后端用同一份配置真的问一次 AI 服务商 */
async function testAndSave() {
  if (!save() || testing.value) return // save() 已把配置写进 localStorage

  // baseUrl 由前端提供（后端不再兜底），先拦一道
  if (!aiConfig.baseUrl) {
    ElMessage.warning('请先填写 Base URL（选供应商会自动带出官方地址）')
    return
  }

  testing.value = true
  try {
    const result = await testAiConfig()
    if (result.ok) {
      ElMessage.success(`已保存，${result.message} ✅`)
    } else {
      // 后端会把具体原因放在 message 里，比如"API Key 无效（HTTP 401）"
      ElMessage.warning(`已保存，但 AI 服务测试未通过：${result.message}`)
    }
  } catch {
    ElMessage.warning('已保存；测试请求失败，请确认本地后端已启动')
  } finally {
    testing.value = false
  }
}

function clearConfig() {
  aiConfig.clear()
  apiKeyInput.value = ''
  baseUrlInput.value = aiConfig.baseUrl
  modelInput.value = ''
  ElMessage.success('已清除 AI 配置')
}

/* ── 考试倒计时 CRUD（后端 MySQL 保存，接口契约联调时确认） ── */
const examName = ref('')
const examDate = ref('')
const editingExamId = ref<number | null>(null)

onMounted(() => {
  examStore.fetch()
})

function levelOf(exam: ExamCountdown): 'red' | 'orange' | 'green' {
  if (exam.level) return exam.level
  // level 缺省兜底：≤7 天红 / ≤14 天橙 / 其余绿
  return exam.daysLeft <= 7 ? 'red' : exam.daysLeft <= 14 ? 'orange' : 'green'
}

async function submitExam() {
  const name = examName.value.trim()
  if (!name || !examDate.value) return
  try {
    if (editingExamId.value != null) {
      await examStore.update(editingExamId.value, { name, date: examDate.value })
      ElMessage.success('考试已更新')
    } else {
      await examStore.create({ name, date: examDate.value })
      ElMessage.success('考试已添加')
    }
    resetExamForm()
  } catch {
    // 失败提示已由 request 层统一给出
  }
}

function startEditExam(exam: ExamCountdown) {
  editingExamId.value = exam.id
  examName.value = exam.name
  examDate.value = exam.date
}

function resetExamForm() {
  editingExamId.value = null
  examName.value = ''
  examDate.value = ''
}

async function removeExam(exam: ExamCountdown) {
  try {
    await ElMessageBox.confirm(`删除考试「${exam.name}」？`, '删除考试', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await examStore.remove(exam.id)
    if (editingExamId.value === exam.id) resetExamForm()
    ElMessage.success('已删除')
  } catch {
    // 失败提示已由 request 层统一给出
  }
}

/* ── 主题 ── */
const themes: { key: ThemeMode; name: string; desc: string }[] = [
  { key: 'auto', name: '跟随时间', desc: '白天浅色，夜晚深色' },
  { key: 'light', name: '浅色', desc: '奶油白，柔和治愈' },
  { key: 'dark', name: '深色', desc: '暖暗灰，夜间护眼' },
  { key: 'system', name: '跟随系统', desc: '自动匹配系统外观' }
]

/* ── 关于 ── */
/** GitHub 仓库地址（设置页自动渲染为可点击链接） */
const GITHUB_URL = 'https://github.com/F-Qing/Asteria-'
</script>

<style scoped>
.setting-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  max-width: 860px;
}

/* ── 选择卡片（供应商 / 主题共用） ── */
.card-grid,
.provider-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: var(--space-3);
}
.select-card {
  position: relative;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  border: 1.5px solid transparent;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: var(--transition);
  text-align: center;
}
html.dark .select-card {
  background: rgba(255, 255, 255, 0.04);
}
.select-card:hover {
  transform: translateY(-2px);
  border-color: rgba(59, 130, 246, 0.15);
  box-shadow: var(--shadow-md);
}
.select-card.selected {
  border-color: var(--primary);
  background: rgba(59, 130, 246, 0.05);
  box-shadow: var(--glow-primary);
}
.select-card-name {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 14px;
}
.select-card-desc {
  font-size: 12px;
  margin-top: 2px;
}
.check {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  color: var(--primary);
  font-size: 14px;
}

/* ── AI 配置表单 ── */
.form-area {
  margin-top: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}
.field-label {
  font-size: 13px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}
.saved-tag {
  font-size: 11px;
}
.model-select {
  width: 100%;
}
.form-actions {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}
.field-hint {
  font-size: 12px;
}

/* ── 考试倒计时 ── */
.exam-form {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
  align-items: center;
}
.exam-name-input {
  flex: 1;
  min-width: 200px;
}
.exam-date-picker {
  width: 180px;
}
.exam-list {
  margin-top: var(--space-4);
  display: flex;
  flex-direction: column;
}
.exam-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) 0;
}
.exam-item + .exam-item {
  border-top: 1px solid rgba(150, 140, 125, 0.12);
}
.exam-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.exam-dot.red { background: var(--error); box-shadow: 0 0 8px var(--error); }
.exam-dot.orange { background: var(--warning); box-shadow: 0 0 8px var(--warning); }
.exam-dot.green { background: var(--success); box-shadow: 0 0 8px var(--success); }
.exam-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.exam-name {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.exam-date {
  font-size: 12px;
}
.exam-days {
  font-size: 13px;
  color: var(--text-sub);
  white-space: nowrap;
}
.exam-days b {
  color: var(--primary-soft);
  font-size: 16px;
}
.exam-op {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
}
.exam-op:hover {
  background: var(--blue-gray);
  color: var(--text-main);
}
.exam-op.danger:hover {
  background: var(--error-bg);
  color: var(--error);
}

/* ── 主题预览 ── */
.theme-preview {
  height: 64px;
  border-radius: var(--radius-sm);
  margin-bottom: var(--space-3);
  border: 1px solid rgba(150, 140, 125, 0.2);
}
.theme-preview.light {
  background: linear-gradient(135deg, #fff8f0 0%, #e8f0f8 100%);
}
.theme-preview.dark {
  background: linear-gradient(135deg, #2a2830 0%, #211f25 100%);
}
.theme-preview.system {
  background: linear-gradient(135deg, #fff8f0 0%, #fff8f0 50%, #2a2830 50%, #211f25 100%);
}
/* 跟随时间：昼夜对角渐变 + 一弯月牙意象 */
.theme-preview.auto {
  background:
    radial-gradient(circle at 78% 26%, rgba(255, 255, 255, 0.85) 0%, rgba(255, 255, 255, 0) 22%),
    linear-gradient(135deg, #ffe9cf 0%, #f5d5d5 48%, #2a3253 52%, #1f2743 100%);
}

.about-row {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-3) 0;
  font-size: 14px;
}
.about-row + .about-row {
  border-top: 1px solid rgba(150, 140, 125, 0.12);
}
</style>
