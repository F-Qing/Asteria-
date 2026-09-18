import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

/**
 * Markdown 渲染封装（见 docs/02 2.6）：
 * markdown-it（默认配置 + 换行）→ DOMPurify.sanitize → 安全 HTML。
 * 所有 AI 输出、题干、解析统一走 MarkdownView，禁止直接 v-html 原始数据。
 */
const md: MarkdownIt = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
})

export function renderMarkdown(source: string): string {
  const raw = md.render(source ?? '')
  return DOMPurify.sanitize(raw)
}
