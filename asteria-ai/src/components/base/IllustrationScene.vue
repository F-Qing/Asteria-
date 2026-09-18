<template>
  <!-- 手绘风空状态场景（B2）：粉彩扁平 + 墨线描边，配色走 token 自动适配明暗 -->
  <svg
    class="illustration-scene"
    :class="`scene-${name}`"
    viewBox="0 0 200 140"
    fill="none"
    aria-hidden="true"
  >
    <!-- 云上书堆（题库空） -->
    <g v-if="name === 'books'">
      <ellipse class="il-blob" cx="100" cy="112" rx="62" ry="16" fill="var(--blue-gray)" />
      <g class="il-float">
        <g transform="rotate(-4 100 92)">
          <rect x="62" y="84" width="76" height="14" rx="4" fill="var(--sakura)" stroke="var(--caramel)" stroke-width="1.6" />
          <line x1="72" y1="91" x2="128" y2="91" stroke="var(--caramel)" stroke-width="1.2" opacity="0.6" />
        </g>
        <g transform="rotate(3 100 74)">
          <rect x="66" y="66" width="70" height="14" rx="4" fill="var(--lavender)" stroke="var(--caramel)" stroke-width="1.6" />
          <line x1="76" y1="73" x2="126" y2="73" stroke="var(--caramel)" stroke-width="1.2" opacity="0.6" />
        </g>
        <g transform="rotate(-2 100 56)">
          <rect x="72" y="48" width="58" height="14" rx="4" fill="var(--mint)" stroke="var(--caramel)" stroke-width="1.6" />
          <line x1="82" y1="55" x2="120" y2="55" stroke="var(--caramel)" stroke-width="1.2" opacity="0.6" />
        </g>
      </g>
      <path class="il-twinkle" d="M152 30c.8 5 2.8 7 7.8 7.8-5 .8-7 2.8-7.8 7.8-.8-5-2.8-7-7.8-7.8 5-.8 7-2.8 7.8-7.8z" fill="var(--primary-violet)" />
      <circle cx="46" cy="38" r="3" fill="var(--primary-soft)" opacity="0.55" />
    </g>

    <!-- 月亮小猫打盹（最近刷题空） -->
    <g v-else-if="name === 'nap'">
      <g class="il-float">
        <path
          d="M126 24a44 44 0 1 0 26 74 38 38 0 0 1-26-74z"
          fill="var(--apricot)"
          stroke="var(--caramel)"
          stroke-width="1.8"
          stroke-linejoin="round"
        />
        <!-- 蜷睡的小猫 -->
        <g stroke="var(--text-sub)" stroke-width="1.8" stroke-linecap="round">
          <path d="M96 74c2-9 12-13 21-9 7 3 9 10 6 16-8 3-22 2-27-7z" fill="var(--sakura)" />
          <path d="M100 68l-2-7 7 3" fill="var(--sakura)" />
          <path d="M112 65l1-7 6 5" fill="var(--sakura)" />
          <path d="M104 72c1.6 1.2 3.4 1.2 5 0" fill="none" />
          <path d="M122 79c6 1 9 5 8 10" fill="none" />
        </g>
        <text x="120" y="46" class="il-zzz" fill="var(--primary-soft)">z</text>
        <text x="130" y="34" class="il-zzz il-zzz-2" fill="var(--primary-violet)">z</text>
      </g>
      <path class="il-twinkle" d="M48 34c.7 4.4 2.4 6.2 6.8 6.8-4.4.7-6.2 2.4-6.8 6.8-.7-4.4-2.4-6.2-6.8-6.8 4.4-.7 6.2-2.4 6.8-6.8z" fill="var(--primary-soft)" />
      <circle cx="66" cy="94" r="2.6" fill="var(--primary-violet)" opacity="0.5" />
      <circle cx="160" cy="106" r="2.2" fill="var(--primary-soft)" opacity="0.5" />
    </g>

    <!-- 纸飞机航迹（AI 助手空） -->
    <g v-else-if="name === 'plane'">
      <path
        class="il-trail"
        d="M28 96c26 10 52 8 70-10 14-14 16-30 8-44"
        stroke="var(--primary-soft)"
        stroke-width="1.8"
        stroke-linecap="round"
        stroke-dasharray="1 9"
      />
      <g class="il-plane">
        <path d="M128 30l44 14-30 12z" fill="var(--surface-solid)" stroke="var(--text-sub)" stroke-width="1.8" stroke-linejoin="round" />
        <path d="M142 44l4 16 10-16z" fill="var(--blue-gray)" stroke="var(--text-sub)" stroke-width="1.8" stroke-linejoin="round" />
      </g>
      <ellipse cx="52" cy="60" rx="26" ry="9" fill="var(--lavender)" opacity="0.8" />
      <ellipse cx="166" cy="104" rx="22" ry="8" fill="var(--blue-gray)" opacity="0.8" />
      <path class="il-twinkle" d="M40 30c.6 4 2.2 5.6 6.2 6.2-4 .6-5.6 2.2-6.2 6.2-.6-4-2.2-5.6-6.2-6.2 4-.6 5.6-2.2 6.2-6.2z" fill="var(--primary-violet)" />
    </g>

    <!-- 灯泡幼苗（知识点空） -->
    <g v-else-if="name === 'sprout'">
      <g class="il-float">
        <path
          d="M100 26c-17 0-29 12-29 27 0 10 5 16 10 22 4 4 5 8 5 12h28c0-4 1-8 5-12 5-6 10-12 10-22 0-15-12-27-29-27z"
          fill="var(--blue-gray)"
          stroke="var(--text-sub)"
          stroke-width="1.8"
          stroke-linejoin="round"
        />
        <path d="M100 76V56" stroke="var(--success)" stroke-width="2.2" stroke-linecap="round" />
        <path d="M100 62c-1-8-8-11-15-10 1 8 8 12 15 10z" fill="var(--mint)" stroke="var(--success)" stroke-width="1.6" />
        <path d="M100 56c1-8 8-11 15-10-1 8-8 12-15 10z" fill="var(--mint)" stroke="var(--success)" stroke-width="1.6" />
        <rect x="88" y="90" width="24" height="5" rx="2.5" fill="var(--caramel)" />
        <rect x="91" y="98" width="18" height="5" rx="2.5" fill="var(--caramel)" opacity="0.7" />
      </g>
      <g stroke="var(--primary-soft)" stroke-width="2" stroke-linecap="round" class="il-rays">
        <path d="M60 34l7 5" />
        <path d="M140 34l-7 5" />
        <path d="M100 12v8" />
      </g>
      <path class="il-twinkle" d="M156 58c.6 4 2.2 5.6 6.2 6.2-4 .6-5.6 2.2-6.2 6.2-.6-4-2.2-5.6-6.2-6.2 4-.6 5.6-2.2 6.2-6.2z" fill="var(--primary-violet)" />
    </g>

    <!-- 无错题奖杯（错题空） -->
    <g v-else-if="name === 'trophy'">
      <g class="il-float">
        <path
          d="M76 40h48v18c0 14-10 24-24 24s-24-10-24-24V40z"
          fill="var(--sakura)"
          stroke="var(--text-sub)"
          stroke-width="1.8"
          stroke-linejoin="round"
        />
        <path d="M76 46H62c0 12 6 18 16 19" stroke="var(--text-sub)" stroke-width="1.8" stroke-linecap="round" />
        <path d="M124 46h14c0 12-6 18-16 19" stroke="var(--text-sub)" stroke-width="1.8" stroke-linecap="round" />
        <path d="M96 82h8v12h-8z" fill="var(--lavender)" stroke="var(--text-sub)" stroke-width="1.6" />
        <path d="M84 100h32a4 4 0 0 1 4 4v4H80v-4a4 4 0 0 1 4-4z" fill="var(--lavender)" stroke="var(--text-sub)" stroke-width="1.6" />
        <path d="M100 50l2.6 5.4 5.9.8-4.3 4.1 1 5.9-5.2-2.8-5.2 2.8 1-5.9-4.3-4.1 5.9-.8z" fill="var(--apricot)" stroke="var(--caramel)" stroke-width="1.2" />
      </g>
      <circle cx="56" cy="42" r="3" fill="var(--primary-soft)" />
      <circle cx="150" cy="52" r="2.6" fill="var(--primary-violet)" />
      <circle cx="142" cy="30" r="2" fill="var(--primary-soft)" />
      <circle cx="64" cy="76" r="2.2" fill="var(--primary-violet)" opacity="0.6" />
    </g>

    <!-- 迷路指南针（题库走丢） -->
    <g v-else-if="name === 'compass'">
      <circle class="il-float" cx="100" cy="70" r="38" fill="var(--blue-gray)" stroke="var(--text-sub)" stroke-width="1.8" />
      <circle cx="100" cy="70" r="30" stroke="var(--caramel)" stroke-width="1.2" stroke-dasharray="3 7" />
      <g class="il-needle">
        <path d="M100 46l8 24-8 24-8-24z" fill="var(--sakura)" stroke="var(--text-sub)" stroke-width="1.4" stroke-linejoin="round" />
        <path d="M100 46l8 24h-16z" fill="var(--primary-soft)" stroke="none" />
      </g>
      <circle cx="100" cy="70" r="4" fill="var(--surface-solid)" stroke="var(--text-sub)" stroke-width="1.6" />
      <path class="il-twinkle" d="M152 40c.7 4.4 2.4 6.2 6.8 6.8-4.4.7-6.2 2.4-6.8 6.8-.7-4.4-2.4-6.2-6.8-6.8 4.4-.7 6.2-2.4 6.8-6.8z" fill="var(--primary-violet)" />
      <text x="42" y="52" class="il-zzz" fill="var(--primary-soft)">?</text>
    </g>
  </svg>
</template>

<script setup lang="ts">
/** 手绘 SVG 场景（B2）：按 name 选择，替换空状态的图标圆底 */
defineProps<{
  name: 'books' | 'nap' | 'plane' | 'sprout' | 'trophy' | 'compass'
}>()
</script>

<style scoped>
.illustration-scene {
  width: 172px;
  height: auto;
  display: block;
  margin-bottom: var(--space-3);
}
.il-float {
  animation: il-float 5s ease-in-out infinite;
  transform-origin: center;
}
.il-twinkle {
  animation: il-twinkle 3.2s ease-in-out infinite;
  transform-origin: center;
  transform-box: fill-box;
}
.il-zzz {
  font-family: var(--font-accent);
  font-size: 13px;
  animation: il-twinkle 2.8s ease-in-out infinite;
}
.il-zzz-2 {
  font-size: 10px;
  animation-delay: -1.4s;
}
.il-trail {
  animation: il-trail 2.2s linear infinite;
}
.il-plane {
  animation: il-plane 4s ease-in-out infinite;
  transform-origin: center;
  transform-box: fill-box;
}
.il-needle {
  animation: il-needle 4s ease-in-out infinite;
  transform-origin: 100px 70px;
}
.il-rays {
  animation: il-twinkle 2.6s ease-in-out infinite;
}
@keyframes il-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
@keyframes il-twinkle {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}
@keyframes il-trail {
  to { stroke-dashoffset: -20; }
}
@keyframes il-plane {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-4px) rotate(-3deg); }
}
@keyframes il-needle {
  0%, 100% { transform: rotate(-9deg); }
  50% { transform: rotate(9deg); }
}
@media (prefers-reduced-motion: reduce) {
  .il-float,
  .il-twinkle,
  .il-zzz,
  .il-trail,
  .il-plane,
  .il-needle,
  .il-rays {
    animation: none;
  }
}
</style>
