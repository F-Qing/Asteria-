import { get } from './request'
import type { PageData, Question, QuestionQuery } from '@/types'

/** GET /banks/{bankId}/questions — 题目分页（浏览/检索） */
export function listQuestions(bankId: number, query: QuestionQuery = {}): Promise<PageData<Question>> {
  return get<PageData<Question>>(`/banks/${bankId}/questions`, {
    page: 1,
    pageSize: 20,
    ...query
  } as Record<string, unknown>)
}
