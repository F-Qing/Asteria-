/* ============================================================
   用户自带 AI 配置（BYOK）：localStorage 轻量读写
   独立为工具模块而非直接从 store 读取，避免 api 层 ↔ Pinia 循环依赖。
   安全基线：密钥只存 localStorage，只随请求头发送给用户自己部署的
   本地后端；不写入日志、不放入 URL query、不明文常驻展示。
   ============================================================ */

export interface AiConfig {
  provider: string
  apiKey: string
  baseUrl: string
  model: string
}

export interface ProviderPreset {
  key: string
  name: string
  /** 官方默认地址，选择供应商后自动填充（可修改） */
  baseUrl: string
  /** 常见模型快捷建议。**这里必须是官方模型 ID 原文**（发请求用的就是它），仍允许手输 */
  models: string[]
  /**
   * 模型 ID → 下拉框里的说明文字（版本号/档位）。
   * 只影响展示：注意 ID 里往往不带版本号（如 deepseek-flash 实际是 V4.1），
   * 而版本是会换的，所以"给人看的名字"单独放这里，别去改 ID。
   */
  modelLabels?: Record<string, string>
}

const STORAGE_KEY = 'asteria-ai-config'

export const AI_PROVIDERS: ProviderPreset[] = [
  // 模型名以各家官网当前在售为准（2026-09 核对）：过期的名字会让请求直接 404
  { key: 'openai', name: 'OpenAI', baseUrl: 'https://api.openai.com/v1', models: ['gpt-5.5', 'gpt-5.4', 'gpt-5.4-mini', 'gpt-5.4-nano'] },
  {
    key: 'deepseek',
    name: 'DeepSeek',
    baseUrl: 'https://api.deepseek.com',
    // 官方在售的模型 ID 就这两个（2026-09-10 起）：
    //   deepseek-flash     → DeepSeek-V4.1-Flash（官方说明：改名调用最新 V4.1 Flash）
    //   deepseek-v4-pro    → DeepSeek-V4-Pro（GA）
    // 注意：ID 里不含 "4.1"，V4.1 是版本号；老的 deepseek-chat / deepseek-reasoner 已下线，
    //      deepseek-v4-flash 只是临时兼容路由，都不再作为选项给出。
    models: ['deepseek-flash', 'deepseek-v4-pro'],
    modelLabels: {
      'deepseek-flash': 'DeepSeek-V4.1-Flash（快 · 便宜）',
      'deepseek-v4-pro': 'DeepSeek-V4-Pro（旗舰 · 更强）'
    }
  },
  { key: 'moonshot', name: 'Moonshot (Kimi)', baseUrl: 'https://api.moonshot.cn/v1', models: ['kimi-k3', 'kimi-k2.7-code', 'kimi-k2.6'] },
  { key: 'dashscope', name: '通义千问', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', models: ['qwen3.8-flash', 'qwen3.7-plus', 'qwen3.8-max'] },
  { key: 'zhipu', name: '智谱 GLM', baseUrl: 'https://open.bigmodel.cn/api/paas/v4', models: ['glm-4.7-flash', 'glm-5.3-flash', 'glm-5.3'] },
  // 只保留兼容 OpenAI /chat/completions 协议的服务商：Gemini 要额外的兼容层路径、
  // Anthropic 是自有协议，两者的 baseUrl 都拼不出可直接调用的地址，故移除
  { key: 'custom', name: '自定义', baseUrl: '', models: [] }
]

/** 模型 ID → 展示名（没配就原样显示 ID）。发请求用的始终是 ID 本身。 */
export function modelLabelOf(providerKey: string, modelId: string): string {
  const preset = AI_PROVIDERS.find((p) => p.key === providerKey)
  return preset?.modelLabels?.[modelId] ?? modelId
}

export function readAiConfig(): AiConfig | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as AiConfig) : null
  } catch {
    return null
  }
}

export function writeAiConfig(config: AiConfig): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(config))
}

export function clearAiConfig(): void {
  localStorage.removeItem(STORAGE_KEY)
}

/**
 * 生成随 /api 请求发送的用户 AI 配置请求头（未配置时返回空对象）。
 * 字段名以后端契约为准，联调时确认。
 * 注意：该请求携带用户自带密钥 —— 后端不得落库、不得进日志。
 *
 * temperature / maxTokens 不在这里传：它们是按任务定的工程参数，
 * 由后端在各自功能里写死（生成解析要稳定 → 低温度），不该让用户填。
 */
export function aiConfigHeaders(): Record<string, string> {
  const cfg = readAiConfig()
  if (!cfg || !cfg.apiKey || !cfg.model) return {}
  const headers: Record<string, string> = {
    'X-AI-Provider': cfg.provider,
    'X-AI-Model': cfg.model,
    'X-AI-Key': cfg.apiKey
  }
  if (cfg.baseUrl) headers['X-AI-Base-Url'] = cfg.baseUrl
  return headers
}
