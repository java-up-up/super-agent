import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://127.0.0.1:9082'

  return {
    plugins: [tailwindcss(), vue()],
    resolve: {
      alias: {
        // shadcn-vue / 新栈统一用 @ 指向 src，与老代码的相对路径引用并存
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            framework: ['vue', 'vue-router'],
            markdown: ['marked', 'dompurify', 'highlight.js/lib/core']
          }
        }
      }
    },
    server: {
      port: 5173,
      strictPort: true,
      // 开发阶段直接代理到后端模块，前端代码只需要面向统一的 /api 路径。
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true
        },
        '/admin/auth': {
          target: proxyTarget,
          changeOrigin: true
        },
        '/manage': {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    },
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.js'],
      include: ['src/**/*.spec.js'],
      css: false
    }
  }
})
