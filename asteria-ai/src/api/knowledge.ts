import { get, post } from './request'
import type { KnowledgeSummary, KnowledgeSummaryReq } from '@/types'

/** POST /knowledge-summary — 按题库生成总结（force=false 时命中缓存直接返回） */
export function generateSummary(data: KnowledgeSummaryReq): Promise<KnowledgeSummary> {
  return post<KnowledgeSummary>('/knowledge-summary', data)
}

/** GET /knowledge-summary — 查询题库已生成总结（无则 null） */
export function getSummary(bankId: number): Promise<KnowledgeSummary | null> {
  return get<KnowledgeSummary | null>('/knowledge-summary', { bankId })
}
