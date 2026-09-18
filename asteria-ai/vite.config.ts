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
