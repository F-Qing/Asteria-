import { defineStore } from 'pinia'
import { clearAiConfig, readAiConfig, writeAiConfig, AI_PROVIDERS } from '@/utils/aiConfig'
import type { AiConfig } from '@/utils/aiConfig'

/**
 * AI 服务配置（用户自带密钥，BYOK）
 * 保存后立即写入 localStorage（key: asteria-ai-config），
 * 请求层通过 utils/aiConfig 的 aiConfigHeaders() 读取并随请求头发送。
 */
export const useAiConfigStore = defineStore('aiConfig', {
  state: (): AiConfig => ({
    provider: 'openai',
    apiKey: '',
    baseUrl: AI_PROVIDERS[0].baseUrl,
    model: '',
    ...(readAiConfig() ?? {})
  }),
  getters: {
    /** Key 与模型名均已填写才算配置完成 */
    isConfigured: (state): boolean => Boolean(state.apiKey && state.model),
    /** 当前供应商的模型快捷建议 */
    modelSuggestions: (state): string[] =>
      AI_PROVIDERS.find((p) => p.key === state.provider)?.models ?? []
  },
  actions: {
    /** 保存到 localStorage（调用前已完成 trim 与校验） */
    save() {
      writeAiConfig({
        provider: this.provider,
        apiKey: this.apiKey,
        baseUrl: this.baseUrl,
        model: this.model
      })
    },
    clear() {
      clearAiConfig()
      this.apiKey = ''
      this.baseUrl = AI_PROVIDERS.find((p) => p.key === this.provider)?.baseUrl ?? ''
      this.model = ''
    },
    /** 切换供应商：自动填充官方 Base URL；模型不在建议列表时清空待重选 */
    applyProvider(key: string) {
      this.provider = key
      const preset = AI_PROVIDERS.find((p) => p.key === key)
      if (!preset) return
      this.baseUrl = preset.baseUrl
      if (this.model && preset.models.length && !preset.models.includes(this.model)) {
        this.model = ''
      }
    }
  }
})
