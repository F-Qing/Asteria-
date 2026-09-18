import { defineStore } from 'pinia'
import { getSession, submitAnswer } from '@/api/practice'
import type { AnswerSubmitResult, PracticeSessionDetail } from '@/types'

const FAVORITE_KEY = 'asteria-favorites'
const MARKED_KEY = 'asteria-marked'

function loadIds(key: string): number[] {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as number[]) : []
  } catch {
    return []
  }
}

/** 当前刷题会话状态 + 本地收藏/标记（不持久化到后端，见 docs/02 2.5） */
export const usePracticeStore = defineStore('practice', {
  state: () => ({
    session: null as PracticeSessionDetail | null,
    /** questionId → 提交结果（含 answer/analysis/isCorrect） */
    answers: {} as Record<number, AnswerSubmitResult>,
    favorites: loadIds(FAVORITE_KEY),
    marked: loadIds(MARKED_KEY)
  }),
  getters: {
    answeredIds(state): Set<number> {
      return new Set(Object.keys(state.answers).map(Number))
    }
  },
  actions: {
    async fetchSession(id: number) {
      this.session = null
      this.answers = {}
      try {
        const detail = await getSession(id)
        this.session = detail
        // 断点续答：把已答记录标记为已答（答案内容需提交后才有，此处仅占位）
      } catch {
        this.session = null
      }
      return this.session
    },
    async submit(questionId: number, userAnswer: string) {
      if (!this.session) throw new Error('会话不存在')
      const result = await submitAnswer(this.session.id, { questionId, userAnswer })
      this.answers[questionId] = result
      this.session.answeredCount = result.answeredCount
      this.session.correctCount = result.correctCount
      this.session.status = result.sessionStatus
      return result
    },
    toggleFavorite(questionId: number) {
      this.favorites = this.favorites.includes(questionId)
        ? this.favorites.filter((id) => id !== questionId)
        : [...this.favorites, questionId]
      localStorage.setItem(FAVORITE_KEY, JSON.stringify(this.favorites))
    },
    toggleMarked(questionId: number) {
      this.marked = this.marked.includes(questionId)
        ? this.marked.filter((id) => id !== questionId)
        : [...this.marked, questionId]
      localStorage.setItem(MARKED_KEY, JSON.stringify(this.marked))
    }
  }
})
