import { get, post } from './request'
import type {
  AnswerSubmitReq,
  AnswerSubmitResult,
  PracticeMode,
  PracticeSession,
  PracticeSessionCreateReq,
  PracticeSessionDetail,
  RecentSession,
  SessionResult,
  StudyStats,
  WrongStats
} from '@/types'

/** POST /practice/sessions — 创建刷题会话 */
export function createSession(data: PracticeSessionCreateReq): Promise<PracticeSession> {
  return post<PracticeSession>('/practice/sessions', data)
}

/** POST /practice/sessions/wrong — 创建错题重刷会话 */
export function createWrongSession(bankId: number, mode: PracticeMode = 'RANDOM'): Promise<PracticeSession> {
  return post<PracticeSession>('/practice/sessions/wrong', { bankId, mode })
}

/** GET /practice/sessions/{id} — 会话详情 + 题目列表（不含答案）+ 已答记录 */
export function getSession(id: number): Promise<PracticeSessionDetail> {
  return get<PracticeSessionDetail>(`/practice/sessions/${id}`)
}

/** POST /practice/sessions/{id}/answers — 提交单题答案（返回答案与 AI 解析） */
export function submitAnswer(sessionId: number, data: AnswerSubmitReq): Promise<AnswerSubmitResult> {
  return post<AnswerSubmitResult>(`/practice/sessions/${sessionId}/answers`, data)
}

/** GET /practice/sessions/{id}/result — 会话结果（完成页） */
export function getSessionResult(id: number): Promise<SessionResult> {
  return get<SessionResult>(`/practice/sessions/${id}/result`)
}

/** GET /practice/wrong-stats — 错题统计 */
export function getWrongStats(bankId: number): Promise<WrongStats> {
  return get<WrongStats>('/practice/wrong-stats', { bankId })
}

/** GET /practice/stats — 学习统计：已刷题次数 + 总体正确率（首页统计卡） */
export function getStudyStats(bankId?: number): Promise<StudyStats> {
  return get<StudyStats>('/practice/stats', { bankId })
}

/** GET /practice/sessions?limit=5 — 最近刷题列表（首页卡片） */
export function listRecentSessions(limit = 5): Promise<RecentSession[]> {
  return get<RecentSession[]>('/practice/sessions', { limit })
}
