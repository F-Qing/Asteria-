<template>
  <div class="countdown-card" :class="{ compact }">
    <template v-if="examStore.nearest">
      <div class="cd-head">
        <Hourglass :size="14" :stroke-width="1.8" />
        <span v-if="!compact" class="cd-title">考试倒计时</span>
        <!-- 管理入口：跳设置页倒计时配置区 -->
        <router-link v-if="!compact" to="/setting" class="cd-manage" title="管理考试日期">
          <Settings :size="13" :stroke-width="1.8" />
        </router-link>
      </div>
      <div v-if="!compact" class="cd-name">{{ examStore.nearest.name }}</div>
      <div class="cd-days" :class="examStore.nearest.level">
        <!-- key 变化触发元素重建，播放轻微缩放动画 -->
        <span :key="examStore.nearest.daysLeft" class="cd-num">{{ examStore.nearest.daysLeft }}</span>
        <span class="cd-unit">天</span>
      </div>
      <div v-if="!compact" class="cd-date">{{ examStore.nearest.date }}</div>
    </template>
    <template v-else>
      <div class="cd-empty" :title="compact ? '暂无考试安排' : ''">
        <CalendarCheck :size="20" :stroke-width="1.6" />
        <template v-if="!compact">
          <span>暂无考试安排</span>
          <router-link to="/setting" class="cd-empty-link">去设置添加</router-link>
        </template>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { Hourglass, Settings, CalendarCheck } from 'lucide-vue-next'
import { useExamStore } from '@/stores/exam'

/** 考试倒计时（后端 MySQL 数据源，见 stores/exam；compact 为侧栏折叠迷你版） */
withDefaults(defineProps<{ compact?: boolean }>(), { compact: false })

const examStore = useExamStore()

onMounted(() => {
  examStore.fetch()
})
</script>

<style scoped>
.countdown-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(
    160deg,
    color-mix(in srgb, var(--blue-gray) 85%, transparent),
    color-mix(in srgb, var(--lavender) 60%, transparent)
  );
  border: 1px solid var(--glass-line);
  box-shadow: var(--shadow-sm), var(--rim-light);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  text-align: center;
  transition: var(--transition);
}
.cd-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  color: var(--text-muted);
  font-size: 13px;
  position: relative;
}
.cd-manage {
  position: absolute;
  right: 0;
  color: var(--text-muted);
  font-size: 12px;
  padding: 2px 4px;
  border-radius: var(--radius-sm);
  transition: var(--transition);
}
.cd-manage:hover {
  color: var(--primary-soft);
  background: var(--glass-bg-strong);
}
.cd-name {
  margin-top: var(--space-2);
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cd-days {
  margin-top: var(--space-1);
  line-height: 1.2;
}
.cd-num {
  display: inline-block;
  font-size: 32px;
  font-weight: 700;
  animation: num-pop 0.4s var(--ease-spring);
}
.cd-days.red .cd-num { color: var(--error); }
.cd-days.orange .cd-num { color: var(--warning); }
.cd-days.green .cd-num { color: var(--success); }
.cd-unit {
  margin-left: var(--space-1);
  font-size: 13px;
  color: var(--text-muted);
}
.cd-date {
  font-size: 13px;
  color: var(--text-muted);
}
.cd-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  color: var(--text-muted);
  font-size: 13px;
  padding: var(--space-2) 0;
}
.cd-empty-link {
  font-size: 12px;
  color: var(--primary-soft);
}
.cd-empty-link:hover {
  color: var(--primary);
}
.countdown-card.compact {
  padding: var(--space-3) var(--space-2);
}
</style>
