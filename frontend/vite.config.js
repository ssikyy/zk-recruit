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
    // 通过代理与后端同源，Session Cookie 与 CSRF Cookie 都能正常工作
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: false
      }
    }
  }
})
