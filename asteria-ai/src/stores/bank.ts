import { defineStore } from 'pinia'
import { getBank, listBanks } from '@/api/bank'
import type { Bank, BankDetail, BankQuery } from '@/types'

/** 题库列表/当前题库缓存，避免重复请求（见 docs/02 2.5） */
export const useBankStore = defineStore('bank', {
  state: () => ({
    banks: [] as Bank[],
    currentBank: null as BankDetail | null,
    loaded: false
  }),
  actions: {
    /** 拉取题库列表；失败静默（页面空态兜底） */
    async fetchBanks(query: BankQuery = {}, force = false) {
      if (this.loaded && !force && !query.keyword) return
      try {
        const data = await listBanks(query)
        this.banks = data.list
        this.loaded = true
      } catch {
        this.banks = []
      }
    },
    async fetchBankDetail(id: number) {
      try {
        this.currentBank = await getBank(id)
      } catch {
        this.currentBank = null
      }
      return this.currentBank
    },
    removeBank(id: number) {
      this.banks = this.banks.filter((b) => b.id !== id)
    }
  }
})
