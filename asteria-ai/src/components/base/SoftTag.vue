<template>
  <span class="soft-tag" :class="variant">
    <slot />
  </span>
</template>

<script setup lang="ts">
/** 胶囊标签（sakura/mint/lavender/blue-gray/apricot 色变体） */
withDefaults(
  defineProps<{
    variant?: 'sakura' | 'mint' | 'lavender' | 'blue-gray' | 'apricot'
  }>(),
  { variant: 'blue-gray' }
)
</script>

<style scoped>
.soft-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: 2px var(--space-3);
  border-radius: var(--radius-pill);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.6;
  color: var(--text-sub);
  white-space: nowrap;
}
.soft-tag.sakura { background: var(--sakura); }
.soft-tag.mint { background: var(--mint); }
.soft-tag.lavender { background: var(--lavender); }
.soft-tag.blue-gray { background: var(--blue-gray); }
.soft-tag.apricot { background: var(--apricot); }
</style>
