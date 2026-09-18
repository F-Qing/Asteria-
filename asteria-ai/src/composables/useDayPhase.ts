import { onMounted, onUnmounted, ref } from 'vue'

export type DayPhase = 'night' | 'morning' | 'day' | 'dusk'

const PHASES: DayPhase[] = ['night', 'morning', 'day', 'dusk']

function phaseOf(d: Date): DayPhase {
  const h = d.getHours()
  if (h < 6) return 'night'
  if (h < 12) return 'morning'
  if (h < 18) return 'day'
  if (h < 22) return 'dusk'
  return 'night'
}

/**
 * 当前晨昏时段（深夜 <6 / 清晨 6-12 / 白天 12-18 / 黄昏 18-22 / 深夜）。
 * 每分钟自检一次，跨段自动切换；URL 加 ?phase=dusk 可覆盖（预览/调试用）。
 */
export function useDayPhase() {
  const override = new URLSearchParams(window.location.search).get('phase')
  const valid = PHASES.includes(override as DayPhase)
  const phase = ref<DayPhase>(valid ? (override as DayPhase) : phaseOf(new Date()))

  let timer: ReturnType<typeof setInterval> | undefined
  onMounted(() => {
    if (valid) return // 预览覆盖时不需要定时器
    timer = setInterval(() => {
      phase.value = phaseOf(new Date())
    }, 60_000)
  })
  onUnmounted(() => {
    if (timer) clearInterval(timer)
  })

  return phase
}
