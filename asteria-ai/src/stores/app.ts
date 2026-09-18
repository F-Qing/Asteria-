import { defineStore } from 'pinia'

export type ThemeMode = 'light' | 'dark' | 'system' | 'auto'

const THEME_KEY = 'asteria-theme'
const SIDEBAR_KEY = 'asteria-sidebar-collapsed'

let mediaCleanup: (() => void) | null = null
let clockTimer: ReturnType<typeof setInterval> | null = null

/** 跟随时间：18:00–6:00 深色，其余浅色（与晨昏问候时段呼应） */
function isNightNow(): boolean {
  const h = new Date().getHours()
  return h >= 18 || h < 6
}

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: localStorage.getItem(SIDEBAR_KEY) === '1',
    theme: (localStorage.getItem(THEME_KEY) as ThemeMode) || 'auto'
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem(SIDEBAR_KEY, this.sidebarCollapsed ? '1' : '0')
    },
    setTheme(theme: ThemeMode) {
      this.theme = theme
      localStorage.setItem(THEME_KEY, theme)
      this.applyTheme()
    },
    /** 主题应用到 <html class="dark">；system 跟随系统外观，auto 跟随时间（每分钟自检） */
    applyTheme() {
      const root = document.documentElement
      mediaCleanup?.()
      mediaCleanup = null
      if (clockTimer) {
        clearInterval(clockTimer)
        clockTimer = null
      }
      if (this.theme === 'system') {
        const media = window.matchMedia('(prefers-color-scheme: dark)')
        const sync = () => root.classList.toggle('dark', media.matches)
        sync()
        media.addEventListener('change', sync)
        mediaCleanup = () => media.removeEventListener('change', sync)
      } else if (this.theme === 'auto') {
        const sync = () => root.classList.toggle('dark', isNightNow())
        sync()
        clockTimer = setInterval(sync, 60_000)
      } else {
        root.classList.toggle('dark', this.theme === 'dark')
      }
    }
  }
})
