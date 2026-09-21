import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    /* 正常终端里这行够用，但它不是保险：实测在受限执行环境（AI 沙箱等拦截删除的环境）里，
       Node 的 fs.rmSync 会「不报错也删不掉」，Vite 的 emptyDir 遍历一圈等于没删，
       旧 chunk 原地留着（实测 dist 从 459 个文件涨到 488，assets 里同时存在两代
       SettingView-*.js / *.css）。所以清空提前到了 build 脚本第一步
       scripts/clean-dist.mjs：自校验 + 回退系统命令，删不掉就报错退出。 */
    emptyOutDir: true,
    rollupOptions: {
      output: {
        /* 大依赖手动分块：提升缓存命中与并行加载（index 主包不再 >1MB） */
        manualChunks: {
          vendor: ['vue', 'vue-router', 'pinia', 'axios'],
          'element-plus': ['element-plus'],
          markdown: ['markdown-it', 'dompurify']
        }
      }
    }
  }
})
