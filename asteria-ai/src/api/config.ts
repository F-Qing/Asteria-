import { del, get, post, put } from './request'
import type { ExamCountdown, ExamCountdownCreateReq, ExamCountdownUpdateReq } from '@/types'

/* ============================================================
   考试倒计时（数据由后端 MySQL 保存，CRUD 接口路径/请求体
   以后端实现为准，联调时确认； daysLeft/level 由后端计算返回）
   ============================================================ */

/** GET /config/exams — 考试倒计时列表 */
export function listExams(): Promise<ExamCountdown[]> {
  return get<ExamCountdown[]>('/config/exams')
}

/** POST /config/exams — 新增考试 */
export function createExam(data: ExamCountdownCreateReq): Promise<ExamCountdown> {
  return post<ExamCountdown>('/config/exams', data)
}

/** PUT /config/exams/{id} — 修改考试 */
export function updateExam(id: number, data: ExamCountdownUpdateReq): Promise<ExamCountdown> {
  return put<ExamCountdown>(`/config/exams/${id}`, data)
}

/** DELETE /config/exams/{id} — 删除考试 */
export function deleteExam(id: number): Promise<null> {
  return del<null>(`/config/exams/${id}`)
}
