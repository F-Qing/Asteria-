import { defineStore } from 'pinia'
import { createExam, deleteExam, listExams, updateExam } from '@/api/config'
import type { ExamCountdown, ExamCountdownCreateReq, ExamCountdownUpdateReq } from '@/types'

/**
 * 考试倒计时：数据由后端 MySQL 保存（换浏览器/清缓存不丢失）。
 * 首页完整版、侧边栏紧凑版、设置页管理共用此 store，保证即时联动。
 */
export const useExamStore = defineStore('exam', {
  state: () => ({
    exams: [] as ExamCountdown[],
    loaded: false
  }),
  getters: {
    /** 最近的一场考试（剩余天数最少） */
    nearest(state): ExamCountdown | null {
      if (!state.exams.length) return null
      return [...state.exams].sort((a, b) => a.daysLeft - b.daysLeft)[0]
    }
  },
  actions: {
    /** 拉取列表；失败静默为空（页面空态兜底，提示由 request 层统一给出） */
    async fetch(force = false) {
      if (this.loaded && !force) return
      try {
        this.exams = await listExams()
        this.loaded = true
      } catch {
        this.exams = []
      }
    },
    async create(data: ExamCountdownCreateReq) {
      await createExam(data)
      await this.fetch(true)
    },
    async update(id: number, data: ExamCountdownUpdateReq) {
      await updateExam(id, data)
      await this.fetch(true)
    },
    async remove(id: number) {
      await deleteExam(id)
      await this.fetch(true)
    }
  }
})
