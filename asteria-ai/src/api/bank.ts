import { del, get, upload } from './request'
import type { Bank, BankDetail, BankQuery, ImportTask, ImportTaskCreated, PageData } from '@/types'

/** GET /banks — 题库列表 */
export function listBanks(query: BankQuery = {}): Promise<PageData<Bank>> {
  return get<PageData<Bank>>('/banks', { page: 1, pageSize: 100, ...query } as Record<string, unknown>)
}

/** GET /banks/{id} — 题库详情（含章节、错题数） */
export function getBank(id: number): Promise<BankDetail> {
  return get<BankDetail>(`/banks/${id}`)
}

/** DELETE /banks/{id} — 删除题库（级联，保留聊天会话） */
export function deleteBank(id: number): Promise<null> {
  return del<null>(`/banks/${id}`)
}

/** POST /banks/import — 上传文件创建导入任务（multipart） */
export function importBank(form: {
  file: File
  bankName?: string
  aiParse?: boolean
}): Promise<ImportTaskCreated> {
  const fd = new FormData()
  fd.append('file', form.file)
  if (form.bankName) fd.append('bankName', form.bankName)
  fd.append('aiParse', String(form.aiParse ?? false))
  return upload<ImportTaskCreated>('/banks/import', fd)
}

/** GET /banks/import/{taskId} — 导入进度（前端 1.5s 轮询） */
export function getImportTask(taskId: string): Promise<ImportTask> {
  return get<ImportTask>(`/banks/import/${taskId}`)
}
