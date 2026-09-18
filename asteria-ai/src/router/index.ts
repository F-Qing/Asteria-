import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'banks',
        name: 'banks',
        component: () => import('@/views/banks/BankListView.vue'),
        meta: { title: '题库' }
      },
      {
        path: 'banks/import',
        name: 'bank-import',
        component: () => import('@/views/banks/BankImportView.vue'),
        meta: { title: '导入题库' }
      },
      {
        path: 'banks/:id',
        name: 'bank-detail',
        component: () => import('@/views/banks/BankDetailView.vue'),
        meta: { title: '题库概览' }
      },
      {
        path: 'practice/:sessionId',
        name: 'practice',
        component: () => import('@/views/practice/PracticeView.vue'),
        meta: { title: '刷题', immersive: true }
      },
      {
        path: 'chat/:sessionId?',
        name: 'chat',
        component: () => import('@/views/chat/ChatView.vue'),
        meta: { title: 'AI 助手' }
      },
      {
        path: 'knowledge-summary',
        name: 'knowledge',
        component: () => import('@/views/knowledge/KnowledgeSummaryView.vue'),
        meta: { title: '知识点总结' }
      },
      {
        path: 'setting',
        name: 'setting',
        component: () => import('@/views/setting/SettingView.vue'),
        meta: { title: '设置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
