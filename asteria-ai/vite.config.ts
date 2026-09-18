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
    /* 每次构建前清空 outDir。默认值虽然是 true，但实测这里没生效：
       旧 chunk 一直累积（dist 里堆了 400 多个没人引用的历史版本），显式写死最稳。
       注意：清空只影响 dist，不会碰别的目录。 */
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
