import { onBeforeUnmount, onMounted } from 'vue'

/**
 * 滚动渐入：为页面中带 .reveal 类的元素在进入视口时追加 .revealed
 * （opacity/translateY 渐入，纯 transform/opacity，不触发 layout）。
 * - 首屏元素：IntersectionObserver 注册后立即回调，同帧可见，不会出现内容空白
 * - prefers-reduced-motion：global.css 中 .reveal 直接降级为无动画显示
 *
 * 用法：在页面 setup 中调用 useReveal()，并给主要卡片加 class="reveal"。
 */
export function useReveal(selector = '.reveal') {
  let observer: IntersectionObserver | null = null

  onMounted(() => {
    if (!('IntersectionObserver' in window)) return
    observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            entry.target.classList.add('revealed')
            observer?.unobserve(entry.target)
          }
        }
      },
      { threshold: 0.08 }
    )
    document.querySelectorAll(`${selector}:not(.revealed)`).forEach((el) => observer!.observe(el))
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    observer = null
  })
}
