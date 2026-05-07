import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  define: {
    global: 'window'
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        ws: true,
        configure: (proxy) => {
          proxy.on('error', (err, req, res) => {
            console.warn('[Vite proxy] 后端 8081 可能未启动:', err.message)
          })
        }
      }
    }
  }
})
