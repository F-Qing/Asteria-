<template>
  <el-dialog v-model="visible" title="题目格式要求" width="600px" append-to-body>
    <div class="guide">
      <p class="guide-intro">
        按下面的标准格式整理，识别率最高。文件是从网页或 Word 里复制来的、格式比较乱时，
        建议先交给 AI 整理一次再上传。
      </p>

      <!-- 标准格式示例 -->
      <div class="guide-block">
        <span class="block-title">标准格式示例</span>
        <pre class="code-block">{{ SAMPLE }}</pre>
      </div>

      <!-- AI 整理提示词 -->
      <div class="guide-block">
        <div class="block-head">
          <span class="block-title">用 AI 快速整理</span>
          <SoftButton variant="outline" :icon="ClipboardCopy" @click="copyPrompt">
            复制提示词
          </SoftButton>
        </div>
        <p class="block-desc">
          复制后粘贴到任意 AI 对话窗口，把题目接在最后面；把 AI 的回复保存成 txt 再上传即可。
        </p>
      </div>

      <!-- 注意事项 -->
      <div class="guide-block">
        <span class="block-title">注意</span>
        <ul class="tips">
          <li>题型只能是这五种：单选题、多选题、判断题、填空题、简答题</li>
          <li>判断题答案写 <b>A</b>（正确）或 <b>B</b>（错误）</li>
          <li>多选题答案字母连写、不分隔、升序，例如 <b>ACD</b></li>
          <li>原文没给答案就留空，不要自己编</li>
          <li>没有「【第 N 题】」题号时题目会被整批跳过，务必保留</li>
        </ul>
      </div>
    </div>

    <template #footer>
      <SoftButton variant="ghost" @click="visible = false">知道了</SoftButton>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { ClipboardCopy } from 'lucide-vue-next'
import SoftButton from '@/components/base/SoftButton.vue'

/** 题目格式说明弹窗：标准格式示例 + 一键复制 AI 整理提示词 */
const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

/** 标准格式示例（只展示，不参与复制） */
const SAMPLE = `【第 1 题】题型：单选题
题目：网页是由 HTML 语言来实现的，HTML 语言是
选项：
  A. 大型数据库
  B. 网页源文件中出现的唯一一种语言
  C. 网络通信协议
  D. 超文本标记语言
正确答案：D`

/**
 * 给 AI 的整理提示词。
 *
 * ⚠️ 改这里的格式规则时，必须同步确认后端 QuestionParser（asteria-common）
 * 认得这些标签 —— 题号行【第 N 题】、题型、题目/题干、选项、正确答案。
 * 判断题答案 A/B、多选题 ACD 是前后端约定，不要改成 对/错 或 A,C,D。
 */
const PROMPT = `请按下面的标准格式整理题目文本。

【重要】只调整结构和补充标签：不得改动任何原文文字，不得编造或补全答案，不得增删题目。

标准格式：
【第 1 题】题型：单选题
题目：题干内容
选项：
  A. 选项内容
  B. 选项内容
  C. 选项内容
  D. 选项内容
正确答案：A

格式要求：
1. 每道题以【第 N 题】开头，N 从 1 开始递增
2. 题型只能是这五种之一：单选题、多选题、判断题、填空题、简答题
3. 选项行固定写成「字母. 内容」；判断题也要写成 A. 正确 / B. 错误
4. 答案写法：
   - 单选题：单个字母，如 A
   - 多选题：字母连写、不分隔、升序，如 ACD
   - 判断题：A 表示正确，B 表示错误
   - 填空题：多个空用中文分号「；」分隔
   - 简答题：答案原文
5. 原文没给答案的，「正确答案：」后面留空，不要自己编
6. 只输出整理后的纯文本，不要任何说明文字，不要 markdown 代码块
7. 如果支持文件输出，请把整理结果保存成一个 txt 文件（文件名：整理后的题目.txt）给我下载；
   不支持文件输出就直接输出纯文本，我自行保存成 txt

待整理文本：
（把题目粘贴在这里）`

async function copyPrompt() {
  try {
    // 需要 https 或 localhost；不满足时走下面的兜底
    await navigator.clipboard.writeText(PROMPT)
    ElMessage.success('提示词已复制，粘贴到 AI 对话框即可')
    return
  } catch {
    // 兜底：老浏览器 / 非安全上下文
  }
  const ta = document.createElement('textarea')
  ta.value = PROMPT
  ta.style.position = 'fixed'
  ta.style.opacity = '0'
  document.body.appendChild(ta)
  ta.select()
  const ok = document.execCommand('copy')
  document.body.removeChild(ta)
  if (ok) {
    ElMessage.success('提示词已复制，粘贴到 AI 对话框即可')
  } else {
    ElMessage.warning('复制失败，请手动选中提示词复制')
  }
}
</script>

<style scoped>
.guide {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}
.guide-intro {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-sub);
}
.guide-block {
  display: flex;
  flex-direction: column;
}
.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
}
.block-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-main);
}
.block-desc {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-muted);
  margin-top: var(--space-2);
}
.code-block {
  margin: var(--space-2) 0 0;
  padding: var(--space-4);
  background: var(--blue-gray);
  border: 1px solid var(--glass-line);
  border-radius: var(--radius-md);
  font-family: var(--font-mono, ui-monospace, Menlo, Consolas, monospace);
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-main);
  white-space: pre;
  overflow-x: auto;
}
.tips {
  margin: var(--space-2) 0 0;
  padding-left: var(--space-5);
  font-size: 13px;
  line-height: 1.9;
  color: var(--text-sub);
}
.tips b {
  color: var(--primary);
}
</style>
