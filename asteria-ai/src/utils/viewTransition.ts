/**
 * View Transitions 封装（A6/A9）：不支持或用户开启减少动态时直接同步执行。
 * origin 传入点击坐标时，新画面以该点为圆心圆形漾开（主题切换用）。
 */
type DocWithVT = Document & { startViewTransition?: (update: () => void) => void }

export function withViewTransition(update: () => void, origin?: { x: number; y: number }): void {
  const doc = document as DocWithVT
  if (!doc.startViewTransition || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    update()
    return
  }
  if (origin) {
    document.documentElement.style.setProperty('--vt-x', `${origin.x}px`)
    document.documentElement.style.setProperty('--vt-y', `${origin.y}px`)
  }
  doc.startViewTransition(update)
}
